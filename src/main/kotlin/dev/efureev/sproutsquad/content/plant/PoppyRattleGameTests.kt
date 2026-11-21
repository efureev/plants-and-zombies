package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.plant.PoppyRattle.Companion.PHASE_TICKS
import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.ServerConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.util.ProblemReporter
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.phys.AABB

/** GameTests of the Poppy Rattle (docs/en/characters/poppy_rattle.md). Registered in SquadGameTests. */
object PoppyRattleGameTests {
    /** Health, price, planting limit, never asleep, and the plants it never hands out. */
    fun stats(helper: GameTestHelper) {
        val type = SquadEntities.POPPY_RATTLE.get()
        val rattle = type.create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Poppy Rattle does not spawn")
        helper.assertValueEqual(rattle.maxHealth, 16f, "max health")
        helper.assertValueEqual(PazConfig.getSunCost(type), 6, "sun cost")
        helper.assertValueEqual(PazConfig.getEntityLimit(type), 6, "planting limit")
        helper.assertFalse(rattle.sleepsDuringDay(), "sleeps by day")
        helper.assertFalse(rattle.sleepsDuringNight(), "sleeps at night")
        listOf(PazEntities.MARIGOLD, PazEntities.CITRON, PazEntities.ROYAL_SUNFLOWER, type).forEach {
            helper.assertTrue(it.builtInRegistryHolder().`is`(PoppyRattle.EXCLUDED), "$it is excluded")
        }
        helper.succeed()
    }

    /** A rattle that is not in the world: nothing ticks it, the tests drive its clock. */
    private fun rattle(helper: GameTestHelper, at: BlockPos = BlockPos(0, 1, 0)): PoppyRattle {
        val rattle = SquadEntities.POPPY_RATTLE.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: throw IllegalStateException("the Poppy Rattle does not spawn")
        val pos = helper.absolutePos(at)
        rattle.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        return rattle
    }

    /** Middle of a phase: a seed scheduled in the phase's first half is owed by then. */
    private fun mid(phase: Long) = phase * PHASE_TICKS + PHASE_TICKS / 2

    /** Fills the rattle with [count] packets, a phase each, starting a fresh rattle in phase 0. */
    private fun fill(rattle: PoppyRattle, count: Int) {
        rattle.advanceClock(100)
        for (phase in 1..count.toLong()) {
            rattle.advanceClock(mid(phase))
            rattle.deliverSeed()
        }
    }

    private fun typeId(type: EntityType<*>) = EntityType.getKey(type).path

    /** Weights by price, the day and night lists, no plant twice in a row, an empty list. */
    fun pool(helper: GameTestHelper) {
        mapOf(0 to 4, 4 to 4, 5 to 3, 7 to 3, 8 to 2, 10 to 2, 11 to 1, 14 to 1).forEach { (cost, weight) ->
            helper.assertValueEqual(PoppyRattle.weightFor(cost), weight, "weight for $cost sun")
        }
        val day = PoppyRattlePool.candidates(helper.level, night = false).map(::typeId)
        val night = PoppyRattlePool.candidates(helper.level, night = true).map(::typeId)
        listOf("peashooter", "needle_spruce", "potling").forEach { helper.assertTrue(it in day, "$it is missing by day") }
        listOf("puffshroom", "marigold", "citron", "royal_sunflower", "poppy_rattle").forEach { helper.assertFalse(it in day, "$it is handed out by day") }
        listOf("puffshroom", "peashooter").forEach { helper.assertTrue(it in night, "$it is missing at night") }
        listOf("sunflower", "potling", "marigold", "poppy_rattle").forEach { helper.assertFalse(it in night, "$it is handed out at night") }

        val random = RandomSource.create(42)
        var last: EntityType<*>? = null
        repeat(200) {
            val next = PoppyRattlePool.pick(helper.level, night = false, random, last)
                ?: return helper.fail("nothing picked by day")
            helper.assertTrue(next != last, "${typeId(next)} twice in a row")
            last = next
        }
        helper.assertTrue(PoppyRattlePool.pickFrom(emptyList(), random, null) == null, "a pick from no plants")
        val only = PazEntities.PEA_SHOOTER
        helper.assertTrue(PoppyRattlePool.pickFrom(listOf(only), random, only) == only, "the only plant, even if it was the last")
        helper.succeed()
    }

    /** A new rattle waits for the next phase; a seed is owed in the first half of each phase. */
    fun phases(helper: GameTestHelper) {
        val rattle = rattle(helper)
        rattle.advanceClock(100)
        rattle.advanceClock(PHASE_TICKS - 1)
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "a new rattle owes a seed in the phase it was planted in")
        rattle.advanceClock(mid(1))
        helper.assertValueEqual(rattle.owedSeeds(), listOf(true), "owed in the night")
        rattle.advanceClock(null)
        helper.assertValueEqual(rattle.owedSeeds(), listOf(true), "a dimension without a clock changes nothing")
        helper.assertTrue(rattle.deliverSeed(), "the seed was not delivered")
        helper.assertValueEqual(rattle.heldCount, 1, "packets held")
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "the seed is still owed")
        rattle.advanceClock(PHASE_TICKS * 2 - 1)
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "a second seed in the same night")
        helper.succeed()
    }

    /** Sleeping through the night: the night's seed is not lost, the morning brings its own. */
    fun skippedNight(helper: GameTestHelper) {
        val rattle = rattle(helper)
        rattle.advanceClock(100)
        rattle.advanceClock(PHASE_TICKS)          // dusk: the night's seed is scheduled
        rattle.advanceClock(2 * PHASE_TICKS + 100) // woke up in the morning
        helper.assertTrue(rattle.owedSeeds().firstOrNull() == true, "the night's seed is missing: ${rattle.owedSeeds()}")
        rattle.advanceClock(mid(2))
        helper.assertTrue(false in rattle.owedSeeds(), "the morning's seed is missing: ${rattle.owedSeeds()}")
        helper.succeed()
    }

    /** Time set back gives nothing, not even when the old phase comes round again. */
    fun timeRewind(helper: GameTestHelper) {
        val rattle = rattle(helper)
        rattle.advanceClock(4 * PHASE_TICKS + 100)
        rattle.advanceClock(mid(5))
        while (rattle.owedSeeds().isNotEmpty()) rattle.deliverSeed()
        rattle.advanceClock(mid(3))
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "owed after the time was set back")
        rattle.advanceClock(3 * PHASE_TICKS + 11_000)
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "owed later in the rewound phase")
        helper.succeed()
    }

    /** Two packets at most: a third seed is lost. */
    fun capacity(helper: GameTestHelper) {
        val rattle = rattle(helper)
        fill(rattle, 2)
        helper.assertValueEqual(rattle.heldCount, 2, "packets held")
        rattle.advanceClock(mid(3))
        helper.assertFalse(rattle.deliverSeed(), "a third packet was delivered")
        helper.assertValueEqual(rattle.heldCount, 2, "packets held after the third seed")
        helper.assertValueEqual(rattle.held().size, 2, "packet list")
        helper.succeed()
    }

    /** Worn on a head it grows nothing, and the phase it is taken off in counts as used. */
    fun worn(helper: GameTestHelper) {
        val pos = BlockPos(0, 1, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        val wearer = helper.spawn(EntityTypes.ZOMBIE, pos)
        wearer.isNoAi = true
        wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.PLANT_POT_HELMET))
        val rattle = helper.spawn(SquadEntities.POPPY_RATTLE.get(), pos)
        helper.assertTrue(rattle.attachToEntity(wearer), "the rattle did not attach to the zombie's plant pot")
        rattle.advanceClock(100)
        rattle.advanceClock(mid(1))
        rattle.advanceClock(mid(2))
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "owed while worn")
        rattle.detachFromEntity()
        rattle.advanceClock(mid(2) + 100)
        helper.assertTrue(rattle.owedSeeds().isEmpty(), "owed in the phase it was taken off in")
        rattle.advanceClock(mid(3))
        helper.assertValueEqual(rattle.owedSeeds(), listOf(true), "owed in the next phase")
        wearer.discard()
        rattle.discard()
        helper.succeed()
    }

    /** The owner takes both packets with an empty hand; a stranger and a sneaking owner do not. */
    fun collect(helper: GameTestHelper) {
        val config = PazConfig.server
        val coop = config.coopPlanting
        val owner = helper.makeMockPlayer(GameType.SURVIVAL)
        val stranger = helper.makeMockPlayer(GameType.SURVIVAL)
        val rattle = rattle(helper)
        rattle.tame(owner)
        fill(rattle, 2)
        val held = rattle.held().toList()
        try {
            config.coopPlanting = false
            helper.assertValueEqual(rattle.mobInteract(stranger, InteractionHand.MAIN_HAND), InteractionResult.FAIL, "a stranger's click")
            helper.assertValueEqual(rattle.heldCount, 2, "packets after a stranger's click")
            owner.setShiftKeyDown(true)
            rattle.mobInteract(owner, InteractionHand.MAIN_HAND)
            helper.assertValueEqual(rattle.heldCount, 2, "packets after a sneaking click")
            owner.setShiftKeyDown(false)
            rattle.mobInteract(owner, InteractionHand.MAIN_HAND)
            helper.assertValueEqual(rattle.heldCount, 0, "packets after the owner's click")
            val got = owner.inventory.nonEquipmentItems.filter { it.`is`(PazItems.SEED_PACKET) }
                .flatMap { stack -> List(stack.count) { stack.get(DataComponents.ENTITY_DATA)?.type() } }
            helper.assertValueEqual(got.sortedBy { it.toString() }, held.sortedBy { it.toString() }, "packets in the inventory")
        } finally {
            config.coopPlanting = coop
        }
        helper.succeed()
    }

    private fun packetsAround(helper: GameTestHelper, rattle: PoppyRattle): List<EntityType<*>?> =
        helper.level.getEntitiesOfClass(ItemEntity::class.java, AABB(rattle.blockPosition()).inflate(3.0))
            .filter { it.item.`is`(PazItems.SEED_PACKET) }
            .onEach { it.discard() }
            .map { it.item.get(DataComponents.ENTITY_DATA)?.type() }

    /** Death and the shovel drop the held packets. */
    fun drops(helper: GameTestHelper) {
        val dying = rattle(helper)
        fill(dying, 2)
        val held = dying.held().toList()
        dying.die(helper.level.damageSources().generic())
        helper.assertValueEqual(packetsAround(helper, dying).sortedBy { it.toString() }, held.sortedBy { it.toString() }, "packets dropped on death")

        val dug = rattle(helper, BlockPos(2, 1, 0))
        fill(dug, 2)
        dug.dropAsSeedPacketItem(force = true)
        val dropped = packetsAround(helper, dug)
        helper.assertValueEqual(dropped.size, 3, "packets dropped by the shovel (its own and two held): $dropped")
        helper.succeed()
    }

    /** Held and owed packets and the phase survive a save, as with the glove or a worn plant. */
    fun saved(helper: GameTestHelper) {
        val rattle = rattle(helper)
        fill(rattle, 2)
        rattle.advanceClock(mid(3))
        val tag = rattle.wearerSnapshot()
        val loaded = rattle(helper)
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), tag))
        helper.assertValueEqual(loaded.held(), rattle.held(), "held packets")
        helper.assertValueEqual(loaded.heldCount, 2, "held count")
        helper.assertValueEqual(loaded.owedSeeds(), rattle.owedSeeds(), "owed seeds")
        loaded.advanceClock(mid(3) + 100)
        helper.assertValueEqual(loaded.owedSeeds(), rattle.owedSeeds(), "owed after a tick in the same phase")
        helper.succeed()
    }

    /**
     * Glowing night eyes only while awake and alive: it sleeps while growing its own seeds on farmland,
     * and then shows the plain `_sleep` texture (there is no `_night_sleep`).
     */
    fun nightTexture(helper: GameTestHelper) {
        val rattle = rattle(helper)
        rattle.advanceClock(100)
        helper.assertTrue(rattle.textureSuffix() == null, "a suffix by day")
        rattle.advanceClock(mid(1))
        helper.assertTrue(rattle.textureSuffix() == "night", "no night suffix at night")
        rattle.isAsleep = true
        helper.assertTrue(rattle.textureSuffix() == null, "the night suffix while asleep")
        rattle.isAsleep = false
        rattle.deathTime = 5
        helper.assertTrue(rattle.textureSuffix() == null, "the night suffix while dying")
        helper.succeed()
    }

    /**
     * A plant limit the server config does not name falls back to the global default, not to the
     * original's built-in list; our own plants' limits from code still apply.
     */
    fun entityLimitFallback(helper: GameTestHelper) {
        val saved = PazConfig.server
        try {
            PazConfig.server = ServerConfig(customPlantEntityLimits = mutableMapOf())
            helper.assertValueEqual(PazConfig.getEntityLimit(PazEntities.MELON_PULT), 30, "Melon-pult limit with an empty config map")
            helper.assertValueEqual(PazConfig.getEntityLimit(PazEntities.COFFEE_BEAN), 30, "Coffee Bean limit with an empty config map")
            helper.assertValueEqual(PazConfig.getEntityLimit(SquadEntities.POPPY_RATTLE.get()), 6, "Poppy Rattle limit from code")
        } finally {
            PazConfig.server = saved
        }
        helper.succeed()
    }

    /** Asleep (growing its own seeds) it delivers nothing; the owed seed waits for it to wake. */
    fun asleepWaits(helper: GameTestHelper) {
        val rattle = rattle(helper)
        rattle.advanceClock(100)
        rattle.advanceClock(mid(1))
        val goal = PoppyRattle.PoppySeedGoal(rattle)
        rattle.isAsleep = true
        helper.assertFalse(goal.canUse(), "delivers while asleep")
        rattle.isAsleep = false
        helper.assertTrue(goal.canUse(), "does not deliver awake")
        helper.assertTrue(rattle.hasOwedSeeds, "the owed seed is gone")
        helper.succeed()
    }

    /** Full, it owes nothing: no "packet ready" jolt for a seed that would be thrown away. */
    fun fullOwesNothing(helper: GameTestHelper) {
        val rattle = rattle(helper)
        fill(rattle, 2)
        rattle.advanceClock(mid(3))
        helper.assertFalse(rattle.hasOwedSeeds, "owes a seed while full")
        rattle.takeAll(helper.makeMockPlayer(GameType.SURVIVAL))
        rattle.advanceClock(mid(4))
        helper.assertTrue(rattle.hasOwedSeeds, "owes nothing once emptied")
        helper.succeed()
    }

    /** The renderer learns how to draw our plants from SquadPlantLook, not from per-plant branches. */
    fun look(helper: GameTestHelper) {
        val rattle = rattle(helper)
        val look: SquadPlantLook = rattle
        fill(rattle, 1)
        helper.assertValueEqual(look.lookPose(), 1, "Poppy Rattle pose")
        helper.assertTrue(look.lookSuffix(null) == "night", "Poppy Rattle suffix in the night it was filled in")
        val spruce: SquadPlantLook = SquadEntities.NEEDLE_SPRUCE.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Needle Spruce does not spawn")
        helper.assertTrue(spruce.lookSuffix(100L) == "strain", "Needle Spruce straining face")
        helper.assertFalse(spruce.lookSpecialAction(), "a live Needle Spruce stands on a stump")
        helper.succeed()
    }
}
