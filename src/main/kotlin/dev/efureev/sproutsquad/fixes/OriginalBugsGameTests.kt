package dev.efureev.sproutsquad.fixes

import com.google.gson.JsonParser
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.ai.goal.WakeUpSleepingPlantsGoal
import joshxviii.plantz.entity.plant.GraveBuster
import joshxviii.plantz.inventory.LetterMenu
import joshxviii.plantz.item.component.LetterDataComponent
import joshxviii.plantz.mixin.MobAccessor
import net.minecraft.world.item.Items
import joshxviii.plantz.block.entity.GravestoneBlockEntity
import joshxviii.plantz.block.entity.SunBatteryBlockEntity
import joshxviii.plantz.entity.PlantPotMinecart
import joshxviii.plantz.entity.plant.Chomper
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.plant.processSeedPacketInteraction
import joshxviii.plantz.entity.zombie.NewspaperZombie
import joshxviii.plantz.getTotalSun
import joshxviii.plantz.item.RollerSkatesItem
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.SunCost
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import java.util.UUID

/**
 * More GameTests for bugs of the original fixed in the fork (docs/en/UPSTREAM.md), see also
 * [OriginalGapsGameTests]. Registered in [dev.efureev.sproutsquad.debug.SquadGameTests].
 */
object OriginalBugsGameTests {
    /** Open ground beside the 1x1x1 "empty" structure, inside its chunk (see [OriginalGapsGameTests]). */
    private fun spot(helper: GameTestHelper): BlockPos {
        val origin = helper.absolutePos(BlockPos.ZERO)
        val dx = if (Math.floorMod(origin.x, 16) <= 12) 0 else -3
        val dz = if (Math.floorMod(origin.z, 16) <= 9) 5 else -5
        return BlockPos(dx, 0, dz)
    }

    private fun clear(helper: GameTestHelper, pos: BlockPos) {
        helper.level.getEntities(null as Entity?, AABB(helper.absolutePos(pos)).inflate(4.0)) { it !is Player }.forEach { it.discard() }
    }

    private fun prepareGround(helper: GameTestHelper, pos: BlockPos) {
        clear(helper, pos)
        for (dx in -1..1) for (dz in -1..1) {
            helper.setBlock(pos.offset(dx, -1, dz), Blocks.GRASS_BLOCK)
            for (dy in 0..2) helper.setBlock(pos.offset(dx, dy, dz), Blocks.AIR)
        }
    }

    /**
     * A Chomper removes a mob it swallows, but not a target it cannot chomp (a player, a boss, an iron
     * golem): those die their own way. It removed any target it killed, players included.
     */
    fun chomperSparesUnchompable(helper: GameTestHelper) {
        val chomper = helper.spawn(PazEntities.CHOMPER, BlockPos(0, 1, 0))
        val goal = Chomper.ChompAttackGoal(chomper)
        try {
            val golem = helper.spawn(EntityTypes.IRON_GOLEM, BlockPos(1, 1, 0))
            golem.health = 1f
            chomper.target = golem
            goal.doAction()
            helper.assertTrue(!golem.isAlive, "the Chomper did not kill the 1 HP golem")
            helper.assertTrue(!golem.isRemoved, "the Chomper removed a golem it cannot chomp")
            golem.discard()

            val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(1, 1, 0))
            chomper.target = zombie
            goal.doAction()
            helper.assertTrue(zombie.isRemoved, "the Chomper did not swallow the zombie")
        } finally {
            chomper.discard()
        }
        helper.succeed()
    }

    /** A plant planted into a Pumpkin keeps its own attributes (it got the Pumpkin's: a Citron could not walk). */
    fun pumpkinKeepsPlantAttributes(helper: GameTestHelper) {
        val pos = spot(helper)
        prepareGround(helper, pos)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val packet = SeedPacketItem.stackFor(PazEntities.CITRON)
        val area = AABB(helper.absolutePos(pos)).inflate(2.0)
        lateinit var pumpkin: Plant
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                pumpkin = helper.spawn(PazEntities.PUMPKIN, pos)
                pumpkin.tame(player)
            }
            .thenIdle(5) // a Pumpkin that has lived a few ticks, as in the game: its attribute instances exist and are saved
            .thenExecute {
                player.inventory.add(ItemStack(PazItems.SUN, SunCost.getSunCost(packet)))
                pumpkin.processSeedPacketInteraction(player, packet, InteractionHand.MAIN_HAND)
                val citron = helper.level.getEntitiesOfClass(Plant::class.java, area) { it.type == PazEntities.CITRON }.firstOrNull()
                helper.assertTrue(citron != null, "no Citron was planted into the Pumpkin")
                for (attribute in listOf(Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.FOLLOW_RANGE)) {
                    val expected = DefaultAttributes.getSupplier(PazEntities.CITRON).getBaseValue(attribute)
                    val actual = citron!!.getAttribute(attribute)!!.baseValue
                    helper.assertTrue(actual == expected, "the Citron in the Pumpkin has ${attribute.registeredName} $actual, expected $expected")
                }
                helper.assertTrue(citron!!.hasPumpkinArmor(), "the Citron did not keep the Pumpkin as armor")
                helper.level.getEntitiesOfClass(Plant::class.java, area).forEach { it.discard() }
            }
            .thenSucceed()
    }

    /** Planting into a Plant Pot Minecart costs sun like planting on the ground (it was free). */
    fun minecartPlantingCostsSun(helper: GameTestHelper) {
        val cart = helper.spawn(PazEntities.PLANT_POT_MINECART, BlockPos(0, 1, 0)) as PlantPotMinecart
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val packet = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER)
        player.setItemInHand(InteractionHand.MAIN_HAND, packet)
        val cost = SunCost.getSunCost(packet)
        try {
            helper.assertTrue(cost > 0, "the Peashooter costs no sun ($cost)")
            cart.interact(player, InteractionHand.MAIN_HAND, cart.position())
            helper.assertTrue(cart.passengers.isEmpty(), "a player without sun planted into the minecart")
            helper.assertTrue(player.mainHandItem.count == 1, "the packet was used up without planting")

            player.inventory.add(ItemStack(PazItems.SUN, cost))
            cart.interact(player, InteractionHand.MAIN_HAND, cart.position())
            helper.assertTrue(cart.passengers.singleOrNull()?.type == PazEntities.PEA_SHOOTER, "the paid Peashooter is not in the minecart")
            helper.assertTrue(player.getTotalSun() == 0, "planting into the minecart took ${cost - player.getTotalSun()} of $cost sun")
            helper.assertTrue(player.mainHandItem.isEmpty, "the packet was not used up")
        } finally {
            cart.passengers.forEach { it.discard() }
            cart.discard()
        }
        helper.succeed()
    }

    /** Roller Skates that break while worn take their friction and step-height modifiers with them. */
    fun skatesModifiersCleared(helper: GameTestHelper) {
        val pos = spot(helper)
        prepareGround(helper, pos)
        lateinit var wearer: net.minecraft.world.entity.monster.zombie.Zombie
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                wearer = helper.spawn(EntityTypes.ZOMBIE, pos)
                wearer.isNoAi = true
                wearer.setItemSlot(EquipmentSlot.FEET, ItemStack(PazItems.ROLLER_SKATES))
            }
            .thenIdle(1) // worn for a while before they break
            .thenExecute {
                val skates = wearer.getItemBySlot(EquipmentSlot.FEET)
                (skates.item as RollerSkatesItem).inventoryTick(skates, helper.level, wearer, EquipmentSlot.FEET)
                helper.assertTrue(wearer.getAttribute(Attributes.FRICTION_MODIFIER)!!.hasModifier(RollerSkatesItem.ROLLER_SKATES_ID), "the skates added no friction modifier")
                skates.shrink(1) // breaks in place, as hurtAndBreak does
            }
            .thenIdle(2)
            .thenExecute {
                val left = listOf(Attributes.FRICTION_MODIFIER, Attributes.STEP_HEIGHT, Attributes.MOVEMENT_SPEED)
                    .filter { wearer.getAttribute(it)?.hasModifier(RollerSkatesItem.ROLLER_SKATES_ID) == true }
                helper.assertTrue(left.isEmpty(), "broken skates left modifiers on ${left.joinToString { it.registeredName }}")
                clear(helper, pos)
            }
            .thenSucceed()
    }

    /** A Newspaper Zombie whose newspaper breaks (or that loads without it) does get angry. */
    fun newspaperAnger(helper: GameTestHelper) {
        val pos = spot(helper)
        prepareGround(helper, pos)
        lateinit var zombie: NewspaperZombie
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                zombie = helper.spawn(PazEntities.NEWSPAPER_ZOMBIE, pos)
                zombie.isNoAi = true
                zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack(PazItems.NEWSPAPER))
            }
            .thenIdle(1)
            .thenExecute {
                helper.assertTrue(!zombie.getAttribute(Attributes.MOVEMENT_SPEED)!!.hasModifier(NewspaperZombie.ANGRY_BONUS_ID), "angry while holding the newspaper")
                zombie.offhandItem.shrink(1) // torn while blocking (BlockingItemBreakChance)
            }
            .thenIdle(2)
            .thenExecute {
                helper.assertTrue(zombie.isAngry(), "the zombie still holds its newspaper")
                helper.assertTrue(zombie.getAttribute(Attributes.MOVEMENT_SPEED)!!.hasModifier(NewspaperZombie.ANGRY_BONUS_ID), "the angry zombie got no speed boost")
                clear(helper, pos)
            }
            .thenSucceed()
    }

    /** Sun added to a Solar Battery is saved even when the battery's fill level stays the same. */
    fun sunBatterySaved(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.SUN_BATTERY_BLOCK)
        try {
            val battery = helper.getBlockEntity(pos, SunBatteryBlockEntity::class.java)
            battery.addSun(1) // the fill level goes from 0 to 1
            val chunk = helper.level.getChunkAt(helper.absolutePos(pos))
            chunk.tryMarkSaved()
            battery.addSun(1) // same fill level
            helper.assertTrue(chunk.isUnsaved, "the battery's new sun is not marked for saving")
        } finally {
            helper.setBlock(pos, Blocks.AIR)
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /** A Gravestone forgets zombies that vanished without dying (despawned, discarded) and spawns again. */
    fun gravestoneForgetsVanished(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.GRAVESTONE)
        try {
            val gravestone = helper.getBlockEntity(pos, GravestoneBlockEntity::class.java)
            @Suppress("UNCHECKED_CAST")
            val tracked = GravestoneBlockEntity::class.java.getDeclaredField("trackedZombieIds")
                .apply { isAccessible = true }.get(gravestone) as MutableSet<UUID>
            tracked += UUID.randomUUID() // a zombie that is gone for good
            repeat(GravestoneBlockEntity.FORGET_MISSING_TICKS + 2) {
                GravestoneBlockEntity.tick(helper.level, helper.absolutePos(pos), helper.getBlockState(pos), gravestone)
            }
            helper.assertTrue(tracked.isEmpty(), "the Gravestone still waits for a zombie that is gone")
        } finally {
            helper.setBlock(pos, Blocks.AIR)
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /**
     * The raid checks its raiders once every 20 ticks; the panic teleport now fires on exactly one of
     * those checks, whatever tick the wave started on (it needed the timer to hit 600 on a check tick).
     */
    fun panicTeleportWindow(helper: GameTestHelper) {
        val duration = ZombieRaid.FINAL_WAVE_DURATION
        for (start in 0 until 20) {
            var hits = 0
            var waveTimer = duration
            for (tick in start + 1..start + duration) {
                waveTimer--
                if (tick % 20 == 0 && ZombieRaid.isPanicCheck(waveTimer)) hits++
            }
            helper.assertTrue(hits == 1, "a wave started on tick $start panics $hits times")
        }
        helper.succeed()
    }

    private fun armoredPlant(helper: GameTestHelper, owner: Player): Plant {
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(0, 1, 0))
        plant.tame(owner)
        plant.applyPumpkinArmor(pumpkinData = TypedEntityData.of(PazEntities.PUMPKIN, CompoundTag()))
        return plant
    }

    /** Shears take pumpkin armor only off a plant the player may tend, and wear like on sheep. */
    fun shearsFollowOwnership(helper: GameTestHelper) {
        val config = PazConfig.server
        val coop = config.coopPlanting
        val owner = helper.makeMockPlayer(GameType.SURVIVAL)
        val stranger = helper.makeMockPlayer(GameType.SURVIVAL)
        val plant = armoredPlant(helper, owner)
        try {
            config.coopPlanting = false
            stranger.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.SHEARS))
            plant.mobInteract(stranger, InteractionHand.MAIN_HAND)
            helper.assertTrue(plant.hasPumpkinArmor(), "a stranger sheared the pumpkin armor off someone else's plant")
            val shears = ItemStack(Items.SHEARS)
            owner.setItemInHand(InteractionHand.MAIN_HAND, shears)
            plant.mobInteract(owner, InteractionHand.MAIN_HAND)
            helper.assertTrue(!plant.hasPumpkinArmor(), "the owner could not shear the pumpkin armor off")
            helper.assertTrue(shears.damageValue == 1, "the shears did not wear (damage ${shears.damageValue})")
        } finally {
            config.coopPlanting = coop
            helper.level.getEntitiesOfClass(Entity::class.java, AABB(helper.absolutePos(BlockPos(0, 1, 0))).inflate(2.0)) { it !is Player }.forEach { it.discard() }
        }
        helper.succeed()
    }

    /** A Grave Buster whose gravestone is gone before it busts stays (it vanished for nothing). */
    fun graveBusterNeedsGrave(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), PazBlocks.GRAVESTONE)
        val buster = helper.spawn(PazEntities.GRAVE_BUSTER, pos)
        buster.tame(helper.makeMockPlayer(GameType.SURVIVAL))
        try {
            helper.setBlock(pos.below(), Blocks.AIR) // broken during the wind-up
            GraveBuster.BustGraveGoal(buster).doAction()
            helper.assertTrue(!buster.isRemoved, "the Grave Buster vanished with no gravestone to bust")
        } finally {
            buster.discard()
        }
        helper.succeed()
    }

    /** A Coffee Bean with nobody left to wake is not used up (it vanished anyway). */
    fun coffeeBeanNeedsSleeper(helper: GameTestHelper) {
        val bean = helper.spawn(PazEntities.COFFEE_BEAN, BlockPos(0, 1, 0))
        try {
            val goal = (bean as MobAccessor).goalSelector.availableGoals.map { it.goal }.filterIsInstance<WakeUpSleepingPlantsGoal>().single()
            helper.assertTrue(goal.targets.isEmpty(), "the test bean has someone to wake")
            helper.assertTrue(!goal.doAction(), "the Coffee Bean counts waking nobody as a success")
        } finally {
            bean.discard()
        }
        helper.succeed()
    }

    /** A Citron just planted walks to its targets (it got the goal only after a reload or a push). */
    fun citronChasesWhenNew(helper: GameTestHelper) {
        val citron = helper.spawn(PazEntities.CITRON, BlockPos(0, 1, 0))
        try {
            val goals = (citron as MobAccessor).goalSelector.availableGoals.map { it.goal }
            helper.assertTrue(goals.any { it === citron.walkGoal }, "a new Citron has no goal to walk to its target")
        } finally {
            citron.discard()
        }
        helper.succeed()
    }

    /** The letter menu writes the attachment only into the letter it was opened on (another letter switched into the hand got it too). */
    fun letterMenuKeepsItsLetter(helper: GameTestHelper) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val opened = ItemStack(PazItems.LETTER).apply { set(PazComponents.LETTER_DATA, LetterDataComponent(item = ItemStack(Items.DIAMOND))) }
        val other = ItemStack(PazItems.LETTER)
        player.inventory.setItem(0, opened)
        player.inventory.setItem(1, other)
        player.inventory.selectedSlot = 0
        val menu = LetterMenu(0, player.inventory, 0)
        player.inventory.selectedSlot = 1 // a modified client can switch hotbar slots with the screen open
        helper.assertTrue(!menu.stillValid(player), "the menu stays open on another letter")
        menu.removed(player)
        val leaked = other.get(PazComponents.LETTER_DATA)?.item ?: ItemStack.EMPTY
        helper.assertTrue(leaked.isEmpty, "the other letter got the attachment ${leaked.hoverName.string}")
        helper.succeed()
    }

    /** Two letters with the same content are equal components (ItemStack inside compared by content). */
    fun letterDataEquality(helper: GameTestHelper) {
        val a = LetterDataComponent(item = ItemStack(Items.DIAMOND), pages = listOf("hi"))
        val b = LetterDataComponent(item = ItemStack(Items.DIAMOND), pages = listOf("hi"))
        helper.assertTrue(a == b && a.hashCode() == b.hashCode(), "equal letters are different components")
        helper.assertTrue(a != b.copy(item = ItemStack(Items.EMERALD)), "letters with different attachments are equal")
        helper.succeed()
    }

    /** Every block of the mod has a name in en_us and ru_ru (19 had only their item's name). */
    fun blockNames(helper: GameTestHelper) {
        val missing = mutableListOf<String>()
        for (lang in listOf("en_us", "ru_ru")) {
            val json = OriginalBugsGameTests::class.java.getResourceAsStream("/assets/${PazMain.MODID}/lang/$lang.json")
                ?.reader(Charsets.UTF_8)?.use { JsonParser.parseReader(it).asJsonObject }
            for (block in BuiltInRegistries.BLOCK) {
                if (BuiltInRegistries.BLOCK.getKey(block).namespace != PazMain.MODID) continue
                if (json?.has(block.descriptionId) != true) missing += "$lang: ${block.descriptionId}"
            }
        }
        if (missing.isNotEmpty()) helper.fail("blocks without a name: ${missing.joinToString()}")
        helper.succeed()
    }

    /** A zombie saved with a state number the game does not know loads idle (it crashed loading). */
    fun zombieUnknownState(helper: GameTestHelper) {
        val zombie = helper.spawn(PazEntities.BROWN_COAT, BlockPos(0, 1, 0))
        try {
            TypedEntityData.of(PazEntities.BROWN_COAT, CompoundTag().apply { putInt("sproutsquad:State", 99) }).loadInto(zombie)
            helper.assertTrue(zombie.state == ZombieState.IDLE, "the zombie loaded as ${zombie.state}")
        } finally {
            zombie.discard()
        }
        helper.succeed()
    }
}
