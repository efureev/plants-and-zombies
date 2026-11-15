package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.registry.SquadBlocks
import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazSpawnPlacements
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.entity.projectile.Needle
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.tags.EntityTypeTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.GameType
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

/**
 * GameTests of the Needle Spruce (docs/en/characters/needle_spruce.md). Registered in SquadGameTests.
 * The lane tests run in `gametest/lane`, an empty 26x6x3 template: the runner loads every chunk under
 * a test's structure, so the whole lane ticks (with the 1x1x1 "empty" structure it did not).
 */
object NeedleSpruceGameTests {
    /** Length of the test strip along +x: the spruce at 0, targets further on. */
    private const val STRIP = 24

    /** Health, range, needle damage and price from the design. */
    fun stats(helper: GameTestHelper) {
        val type = SquadEntities.NEEDLE_SPRUCE.get()
        val spruce = type.create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Needle Spruce does not spawn")
        helper.assertValueEqual(spruce.maxHealth, NeedleSpruce.MAX_HEALTH.toFloat(), "max health")
        helper.assertValueEqual(spruce.getAttributeValue(Attributes.FOLLOW_RANGE), NeedleSpruce.RANGE, "range")
        helper.assertValueEqual(spruce.getAttributeValue(Attributes.ATTACK_DAMAGE), NeedleSpruce.NEEDLE_DAMAGE, "needle damage")
        helper.assertValueEqual(PazConfig.getSunCost(type), NeedleSpruce.SUN_COST, "sun cost")
        helper.assertValueEqual(NeedleSpruce.MAX_HEALTH, 40.0, "designed health")
        helper.assertValueEqual(NeedleSpruce.RANGE, 20.0, "designed range")
        helper.assertValueEqual(NeedleSpruce.SUN_COST, 7, "designed price")
        helper.succeed()
    }

    /**
     * The face follows the state: plain without a target, angry while aiming, straining for the first
     * 750 ms of a volley, and the plain sleep texture while dying (there is no `_angry_sleep`).
     */
    fun face(helper: GameTestHelper) {
        val spruce = SquadEntities.NEEDLE_SPRUCE.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Needle Spruce does not spawn")
        helper.assertValueEqual(spruce.faceSuffix(null), "", "no target")
        spruce.isAggressive = true
        helper.assertValueEqual(spruce.faceSuffix(null), "angry", "aiming")
        helper.assertValueEqual(spruce.faceSuffix(100L), "strain", "100 ms into a volley")
        helper.assertValueEqual(spruce.faceSuffix(900L), "angry", "900 ms into a volley")
        spruce.isAggressive = false
        helper.assertValueEqual(spruce.faceSuffix(100L), "strain", "a volley at a target just lost")
        spruce.isAggressive = true
        spruce.deathTime = 5
        helper.assertValueEqual(spruce.faceSuffix(100L), "", "dying")
        helper.succeed()
    }

    /** A zombie 10 blocks away gets volleys of exactly three 1-damage needles, one volley per 70 ticks. */
    fun volley(helper: GameTestHelper) {
        val (dx, dz) = strip(helper)
        lateinit var spruce: NeedleSpruce
        val seen = mutableSetOf<Int>()
        val volleys = mutableListOf<Pair<Long, List<Needle>>>() // game time, the needles that appeared then
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper, dx, dz)
                spruce = helper.spawn(SquadEntities.NEEDLE_SPRUCE.get(), BlockPos(dx, 0, dz))
                wall(helper, dx + 11, dz)
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(dx + 10, 0, dz))
            }
            .thenWaitUntil {
                val fresh = needlesOf(helper, spruce, dx, dz).filter { seen.add(it.id) }
                if (fresh.isNotEmpty()) volleys += helper.level.gameTime to fresh
                helper.assertTrue(volleys.size >= 2, "waiting for two volleys, seen ${volleys.size}")
            }
            .thenExecute {
                val (firstTime, first) = volleys[0]
                val (secondTime, second) = volleys[1]
                helper.assertValueEqual(first.size, NeedleSpruce.VOLLEY_SIZE, "needles in the first volley")
                helper.assertValueEqual(second.size, NeedleSpruce.VOLLEY_SIZE, "needles in the second volley")
                (first + second).forEach { helper.assertValueEqual(it.damage, 1.0f, "needle damage") }
                helper.assertTrue(secondTime - firstTime >= 70, "volleys ${secondTime - firstTime} ticks apart, expected 70")
                clearMobs(helper, dx, dz)
            }
            .thenSucceed()
    }

    /** No needles at a zombie 22 blocks away, nor at one behind a wall. */
    fun outOfRange(helper: GameTestHelper) {
        val (dx, dz) = strip(helper)
        lateinit var spruce: NeedleSpruce
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper, dx, dz)
                spruce = helper.spawn(SquadEntities.NEEDLE_SPRUCE.get(), BlockPos(dx, 0, dz))
                wall(helper, dx + 23, dz)
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(dx + 22, 0, dz))
            }
            .thenIdle(100)
            .thenExecute {
                helper.assertTrue(needlesOf(helper, spruce, dx, dz).isEmpty(), "shot at a zombie 22 blocks away")
                clearMobs(helper, dx, dz)
                prepare(helper, dx, dz)
                spruce = helper.spawn(SquadEntities.NEEDLE_SPRUCE.get(), BlockPos(dx, 0, dz))
                wall(helper, dx + 5, dz)
                wall(helper, dx + 11, dz)
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(dx + 10, 0, dz))
            }
            .thenIdle(100)
            .thenExecute {
                helper.assertTrue(needlesOf(helper, spruce, dx, dz).isEmpty(), "shot through a wall")
                clearMobs(helper, dx, dz)
            }
            .thenSucceed()
    }

    /**
     * Wild in snowy mountains: a biome spawn for `#sproutsquad:plant/has_needle_spruce` (snowy slopes and
     * groves), standing on snow but not on stone. SPAWNER skips the 42-block spacing from other plants
     * (the test world is full of them) and the random source always passes the 60% roll.
     */
    fun spawns(helper: GameTestHelper) {
        val type = SquadEntities.NEEDLE_SPRUCE.get()
        val entry = PazSpawnPlacements.allBiomeSpawns.singleOrNull { it.entityType == type }
            ?: return helper.fail("no biome spawn for the Needle Spruce")
        helper.assertValueEqual(entry.biomeTag, NeedleSpruce.HAS_NEEDLE_SPRUCE, "biome tag")
        val biomes = helper.level.registryAccess().lookupOrThrow(Registries.BIOME)
        listOf(Biomes.SNOWY_SLOPES, Biomes.GROVE).forEach {
            helper.assertTrue(biomes.getOrThrow(it).`is`(NeedleSpruce.HAS_NEEDLE_SPRUCE), "${it.identifier()} is not in the tag")
        }
        helper.assertFalse(biomes.getOrThrow(Biomes.PLAINS).`is`(NeedleSpruce.HAS_NEEDLE_SPRUCE), "plains are in the tag")

        val lucky = object : RandomSource by RandomSource.create() { override fun nextFloat() = 0f }
        fun canSpawnOn(ground: net.minecraft.world.level.block.Block): Boolean {
            helper.setBlock(SPOT.below(), ground)
            for (y in 0..3) helper.setBlock(SPOT.above(y), Blocks.AIR)
            return SpawnPlacements.checkSpawnRules(type, helper.level, EntitySpawnReason.SPAWNER, helper.absolutePos(SPOT), lucky)
        }
        helper.assertTrue(canSpawnOn(Blocks.SNOW_BLOCK), "does not spawn on a snow block")
        helper.assertFalse(canSpawnOn(Blocks.STONE), "spawns on stone")
        helper.succeed()
    }

    /** It grows wild on snow, so it can live and be planted there (otherwise it dries out and dies). */
    fun livesOnSnow(helper: GameTestHelper) {
        listOf(Blocks.SNOW_BLOCK, Blocks.GRASS_BLOCK).forEach { ground ->
            val spruce = standOn(helper, ground)
            helper.assertTrue(spruce.onValidGround(), "cannot live on ${ground.name.string}")
            helper.assertTrue(spruce.canPlaceOn(ground.defaultBlockState()), "cannot be planted on ${ground.name.string}")
            spruce.discard()
        }
        helper.assertFalse(standOn(helper, Blocks.STONE).onValidGround(), "lives on stone")
        helper.succeed()
    }

    /**
     * It is three blocks tall: a seed packet does not plant it under a lower ceiling (it would stand in
     * the ceiling, suffocate and die), and plants it where there is room.
     */
    fun needsHeadroom(helper: GameTestHelper) {
        // a mock "creative" player gets no infinite materials: a survival one with sun, as in OriginalGapsGameTests
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.inventory.setItem(9, ItemStack(PazItems.SUN, 64)) // not the selected slot: the packet goes there
        fun plant(): InteractionResult {
            val packet = SeedPacketItem.stackFor(SquadEntities.NEEDLE_SPRUCE.get())
            player.setItemInHand(InteractionHand.MAIN_HAND, packet)
            val below = helper.absolutePos(SPOT.below())
            val hit = BlockHitResult(Vec3.atCenterOf(below).add(0.0, 0.5, 0.0), Direction.UP, below, false)
            return packet.useOn(UseOnContext(helper.level, player, InteractionHand.MAIN_HAND, packet, hit))
        }
        fun planted() = helper.level.getEntities(SquadEntities.NEEDLE_SPRUCE.get(), around(helper)) { true }

        standOn(helper, Blocks.SNOW_BLOCK).discard()
        helper.setBlock(SPOT.above(2), Blocks.STONE) // a room two blocks high
        val refused = plant()
        helper.assertTrue(planted().isEmpty(), "planted in a room two blocks high ($refused)")
        helper.setBlock(SPOT.above(2), Blocks.AIR)
        helper.setBlock(SPOT.above(3), Blocks.STONE) // three blocks high: enough for a 2.9-block tree
        val result = plant()
        helper.assertValueEqual(planted().size, 1, "spruces planted in a room three blocks high ($result)")
        planted().forEach { it.discard() }
        helper.succeed()
    }

    /**
     * Powder snow looks like snow but swallows a plant: it cannot be planted there. And a mountain
     * spruce does not freeze.
     */
    fun powderSnow(helper: GameTestHelper) {
        val spruce = standOn(helper, Blocks.SNOW_BLOCK)
        helper.assertFalse(spruce.canPlaceOn(Blocks.POWDER_SNOW.defaultBlockState()), "can be planted on powder snow")
        helper.assertTrue(spruce.type.builtInRegistryHolder().`is`(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES), "freezes in powder snow")
        spruce.discard()
        helper.succeed()
    }

    // region death and the stump

    private val SPOT = BlockPos(2, 0, 1)

    /** Killed by a player on snow: a stump where it stood and 4 sun. */
    fun stumpOnDeath(helper: GameTestHelper) {
        val spruce = standOn(helper, Blocks.SNOW_BLOCK)
        spruce.hurtServer(helper.level, helper.level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)), 1000f)
        helper.assertTrue(spruce.isDeadOrDying, "the spruce survived")
        helper.assertBlockPresent(SquadBlocks.NEEDLE_SPRUCE_STUMP.get(), SPOT)
        helper.assertValueEqual(sunAround(helper), NeedleSpruce.PLAYER_KILL_SUN, "sun dropped")
        // a second die() call (another mod, a double hit in one tick) pays nothing more
        spruce.die(helper.level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)))
        helper.assertValueEqual(sunAround(helper), NeedleSpruce.PLAYER_KILL_SUN, "sun after a second die()")
        helper.succeed()
    }

    /** Killed by a zombie: the stump, but no sun. */
    fun killedByZombie(helper: GameTestHelper) {
        val spruce = standOn(helper, Blocks.SNOW_BLOCK)
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, SPOT.offset(3, 0, 0))
        spruce.hurtServer(helper.level, helper.level.damageSources().mobAttack(zombie), 1000f)
        helper.assertBlockPresent(SquadBlocks.NEEDLE_SPRUCE_STUMP.get(), SPOT)
        helper.assertValueEqual(sunAround(helper), 0, "sun dropped")
        zombie.discard()
        helper.succeed()
    }

    /** A snow layer where it stood is replaced by the stump. */
    fun stumpReplacesSnowLayer(helper: GameTestHelper) {
        val spruce = standOn(helper, Blocks.SNOW_BLOCK)
        helper.setBlock(SPOT, Blocks.SNOW)
        spruce.kill(helper.level)
        helper.assertBlockPresent(SquadBlocks.NEEDLE_SPRUCE_STUMP.get(), SPOT)
        helper.succeed()
    }

    /** In water or on a bottom slab there is no stump: its sapling and sticks drop as items. */
    fun noStumpInWaterOrOnSlab(helper: GameTestHelper) {
        var spruce = standOn(helper, Blocks.SNOW_BLOCK)
        helper.setBlock(SPOT, Blocks.WATER)
        spruce.kill(helper.level)
        helper.assertBlockPresent(Blocks.WATER, SPOT)
        assertStumpLoot(helper, "in water")
        helper.level.getEntities(EntityTypes.ITEM, around(helper)) { true }.forEach { it.discard() }

        spruce = standOn(helper, Blocks.OAK_SLAB)
        spruce.kill(helper.level)
        helper.assertBlockPresent(Blocks.AIR, SPOT)
        assertStumpLoot(helper, "on a slab")
        helper.succeed()
    }

    /** Breaking the stump gives a spruce sapling and one or two sticks. */
    fun stumpLoot(helper: GameTestHelper) {
        helper.setBlock(SPOT.below(), Blocks.SNOW_BLOCK)
        helper.setBlock(SPOT, SquadBlocks.NEEDLE_SPRUCE_STUMP.get())
        helper.level.destroyBlock(helper.absolutePos(SPOT), true)
        helper.assertBlockPresent(Blocks.AIR, SPOT)
        assertStumpLoot(helper, "from the broken stump")
        helper.succeed()
    }

    /**
     * A death that does not happen leaves nothing: a removed spruce (or one whose LivingDeathEvent was
     * cancelled) gets no stump and pays no sun.
     */
    fun noStumpWithoutDeath(helper: GameTestHelper) {
        val spruce = standOn(helper, Blocks.SNOW_BLOCK)
        spruce.discard()
        spruce.die(helper.level.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL)))
        helper.assertBlockPresent(Blocks.AIR, SPOT)
        helper.assertValueEqual(sunAround(helper), 0, "sun paid for a death that did not happen")
        helper.succeed()
    }

    /**
     * On ground a little lower than a full block (farmland, a dirt path, mud) the spruce stands inside the
     * ground's block space; the stump goes on top of it, where the trunk was.
     */
    fun stumpOnFarmlandPathAndMud(helper: GameTestHelper) {
        listOf(Blocks.FARMLAND, Blocks.DIRT_PATH, Blocks.MUD).forEach { ground ->
            val spruce = standOn(helper, ground)
            val groundPos = helper.absolutePos(SPOT.below())
            val top = helper.level.getBlockState(groundPos).getCollisionShape(helper.level, groundPos).max(Direction.Axis.Y)
            spruce.setPos(spruce.x, groundPos.y + top, spruce.z) // where it settles: inside the ground's block
            spruce.kill(helper.level)
            helper.assertBlockPresent(SquadBlocks.NEEDLE_SPRUCE_STUMP.get(), SPOT)
            helper.assertTrue(spruce.standsOnItsStump(), "the dying spruce on ${ground.name.string} does not know about its stump")
            helper.setBlock(SPOT, Blocks.AIR)
            helper.level.getEntities(EntityTypes.ITEM, around(helper)) { true }.forEach { it.discard() }
        }
        helper.succeed()
    }

    /** The model hides the lower trunk only when the stump stands in its place: not in water. */
    fun standsOnItsStumpOnlyWithAStump(helper: GameTestHelper) {
        var spruce = standOn(helper, Blocks.SNOW_BLOCK)
        helper.assertFalse(spruce.standsOnItsStump(), "a living spruce stands on a stump")
        spruce.kill(helper.level)
        helper.assertTrue(spruce.standsOnItsStump(), "dead on snow, but no stump under it")
        helper.setBlock(SPOT, Blocks.AIR)
        spruce = standOn(helper, Blocks.SNOW_BLOCK)
        helper.setBlock(SPOT, Blocks.WATER)
        spruce.kill(helper.level)
        helper.assertFalse(spruce.standsOnItsStump(), "dead in water, where no stump was placed")
        helper.succeed()
    }

    private fun standOn(helper: GameTestHelper, ground: net.minecraft.world.level.block.Block): NeedleSpruce {
        helper.setBlock(SPOT.below(), ground)
        for (y in 0..3) helper.setBlock(SPOT.above(y), Blocks.AIR)
        return helper.spawn(SquadEntities.NEEDLE_SPRUCE.get(), SPOT)
    }

    private fun around(helper: GameTestHelper): AABB = AABB(helper.absolutePos(SPOT)).inflate(3.0)

    private fun sunAround(helper: GameTestHelper): Int =
        helper.level.getEntities(PazEntities.SUN, around(helper)) { true }.sumOf { it.value }

    private fun assertStumpLoot(helper: GameTestHelper, where: String) {
        val stacks = helper.level.getEntities(EntityTypes.ITEM, around(helper)) { true }.map { it.item }
        helper.assertValueEqual(stacks.filter { it.`is`(Items.SPRUCE_SAPLING) }.sumOf { it.count }, 1, "spruce saplings $where")
        val sticks = stacks.filter { it.`is`(Items.STICK) }.sumOf { it.count }
        helper.assertTrue(sticks in 1..2, "$sticks sticks $where, expected 1-2")
    }

    // endregion

    // region strip helpers: a straight lane along +x through the middle of `gametest/lane`

    private fun strip(@Suppress("UNUSED_PARAMETER") helper: GameTestHelper): Pair<Int, Int> = 1 to 1

    private fun ticking(helper: GameTestHelper, dx: Int, dz: Int): Boolean =
        (0 until STRIP).all { helper.level.isPositionEntityTicking(helper.absolutePos(BlockPos(dx + it, 0, dz))) }

    private fun area(helper: GameTestHelper, dx: Int, dz: Int): AABB =
        AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos(dx, 0, dz)), helper.absolutePos(BlockPos(dx + STRIP - 1, 3, dz))).inflate(3.0)

    private fun clearMobs(helper: GameTestHelper, dx: Int, dz: Int) {
        helper.level.getEntities(null as Entity?, area(helper, dx, dz)) { it !is Player }.forEach { it.discard() }
    }

    /** Snow-block floor and three blocks of air above it (the spruce is 3 blocks tall). */
    private fun prepare(helper: GameTestHelper, dx: Int, dz: Int) {
        clearMobs(helper, dx, dz)
        for (x in 0 until STRIP) {
            helper.setBlock(BlockPos(dx + x, -1, dz), Blocks.SNOW_BLOCK)
            for (y in 0..3) helper.setBlock(BlockPos(dx + x, y, dz), Blocks.AIR)
        }
    }

    /** A stone wall 4 blocks high across the lane: stops knockback and blocks line of sight. */
    private fun wall(helper: GameTestHelper, x: Int, dz: Int) {
        for (y in 0..3) helper.setBlock(BlockPos(x, y, dz), Blocks.STONE)
    }

    private fun needlesOf(helper: GameTestHelper, spruce: NeedleSpruce, dx: Int, dz: Int): List<Needle> =
        helper.level.getEntities(PazEntities.NEEDLE, area(helper, dx, dz)) { it.owner == spruce }

    // endregion
}
