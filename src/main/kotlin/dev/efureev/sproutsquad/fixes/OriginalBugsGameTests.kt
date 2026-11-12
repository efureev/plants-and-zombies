package dev.efureev.sproutsquad.fixes

import com.google.gson.JsonParser
import java.util.UUID
import joshxviii.plantz.PazBlocks
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.ai.ZombieState
import joshxviii.plantz.ai.goal.WakeUpSleepingPlantsGoal
import joshxviii.plantz.block.entity.FlagBlockEntity
import joshxviii.plantz.block.entity.GravestoneBlockEntity
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.block.entity.MailboxManager
import joshxviii.plantz.block.entity.SunBatteryBlockEntity
import joshxviii.plantz.entity.plant.Chomper
import joshxviii.plantz.entity.plant.GraveBuster
import joshxviii.plantz.entity.plant.PacketInteractionResult
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.plant.processSeedPacketInteraction
import joshxviii.plantz.entity.plant.processSunItem
import joshxviii.plantz.entity.plant.processWateringItem
import joshxviii.plantz.entity.zombie.NewspaperZombie
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.getTotalSun
import joshxviii.plantz.inventory.LetterMenu
import joshxviii.plantz.item.RollerSkatesItem
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.LetterDataComponent
import joshxviii.plantz.item.component.SunCost
import joshxviii.plantz.mixin.MobAccessor
import joshxviii.plantz.positionPlant
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.attributes.DefaultAttributes
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.TypedEntityData
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

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
        val cart = helper.spawn(PazEntities.PLANT_POT_MINECART, BlockPos(0, 1, 0))
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
            // the gravestone spawns only near a player, and the GameTest server has none; clear anyway
            helper.level.getEntities(null as Entity?, AABB(helper.absolutePos(pos)).inflate(32.0)) { it !is Player }.forEach { it.discard() }
            helper.setBlock(pos, Blocks.AIR)
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /**
     * The raid checks its raiders once every 20 ticks; the panic teleport now fires on exactly one of
     * those checks, whatever tick the wave started on (it needed the timer to hit 600 on a check tick).
     * This checks the window function `ZombieRaid.isPanicCheck` with the raid's cadence modelled here
     * (`ticksActive % 20`, `waveTimer--` in the same tick), not a running raid.
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
            val mob: net.minecraft.world.entity.Mob = bean
            val goal = (mob as MobAccessor).goalSelector.availableGoals.map { it.goal }.filterIsInstance<WakeUpSleepingPlantsGoal>().single()
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
            val mob: net.minecraft.world.entity.Mob = citron
            val goals = (mob as MobAccessor).goalSelector.availableGoals.map { it.goal }
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

    /** A plant converted by lightning (Peashooter into Electric Peashooter) keeps its pumpkin armor. */
    fun conversionKeepsPumpkin(helper: GameTestHelper) {
        val owner = helper.makeMockPlayer(GameType.SURVIVAL)
        val plant = armoredPlant(helper, owner)
        val area = AABB(helper.absolutePos(BlockPos(0, 1, 0))).inflate(2.0)
        try {
            plant.convertToPlantType(PazEntities.ELECTRIC_PEA_SHOOTER)
            val converted = helper.level.getEntitiesOfClass(Plant::class.java, area) { it.type == PazEntities.ELECTRIC_PEA_SHOOTER }.singleOrNull()
            helper.assertTrue(converted != null, "the Peashooter was not converted")
            helper.assertTrue(converted!!.hasPumpkinArmor(), "the converted plant lost its pumpkin armor")
        } finally {
            helper.level.getEntitiesOfClass(Entity::class.java, area) { it !is Player }.forEach { it.discard() }
        }
        helper.succeed()
    }

    /** Seeds come after exactly the sun a plant needs (it took one more). */
    fun seedsAfterRequiredSun(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.FARMLAND)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val plant = helper.spawn(PazEntities.SUNFLOWER, pos)
        try {
            plant.tame(player)
            plant.receivedWater = 1
            plant.seedGrowCooldown = 0
            val required = plant.sunRequiredForSeeds()
            val sun = ItemStack(PazItems.SUN, required)
            repeat(required) { plant.processSunItem(player, sun, InteractionHand.MAIN_HAND, plant.testGrowConditions()) }
            helper.assertTrue(plant.receivedSun == 0, "after $required sun (the requirement) no seeds came, ${plant.receivedSun} sun counted")
        } finally {
            helper.level.getEntities(null as Entity?, AABB(helper.absolutePos(pos)).inflate(2.0)) { it !is Player }.forEach { it.discard() }
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /** A Flag's passive healing is saved (only damage marked it for saving). */
    fun flagHealingSaved(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.PLANTZ_FLAG_BLOCK)
        try {
            val flag = helper.getBlockEntity(pos, FlagBlockEntity::class.java)
            val absolute = helper.absolutePos(pos)
            flag.hurt(10f)
            val hurtHealth = flag.health
            repeat(FlagBlockEntity.HEAL_COOLDOWN_TIME - 1) { flag.tick(helper.level, absolute, helper.getBlockState(pos)) }
            val chunk = helper.level.getChunkAt(absolute)
            chunk.tryMarkSaved()
            flag.tick(helper.level, absolute, helper.getBlockState(pos))
            helper.assertTrue(flag.health > hurtHealth, "the Flag did not heal")
            helper.assertTrue(chunk.isUnsaved, "the Flag's healing is not marked for saving")
        } finally {
            helper.setBlock(pos, Blocks.AIR)
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /**
     * The mailbox list shows a known mailbox whose chunk is not loaded without loading that chunk (building the list
     * loaded every known mailbox's chunk on the server thread, each time a mailbox was opened).
     */
    fun mailboxListLoadsNoChunks(helper: GameTestHelper) {
        val far = helper.absolutePos(BlockPos.ZERO).offset(2_000_000, 0, 0)
        helper.assertTrue(!helper.level.isLoaded(far), "the far mailbox chunk is loaded")
        val mailbox = MailboxBlockEntity(far, PazBlocks.MAILBOX.defaultBlockState())
        try {
            MailboxManager.registerMailbox(helper.level, mailbox)
            val listed = MailboxManager.getMailboxesInLevel(helper.level)
            helper.assertTrue(!helper.level.isLoaded(far), "listing the mailboxes loaded the far mailbox's chunk")
            helper.assertTrue(listed.any { it.blockPos == far }, "the far mailbox is missing from the list")
        } finally {
            MailboxManager.unregisterMailbox(helper.level, far)
        }
        helper.succeed()
    }

    /** A natural plant spawn rolls the 60% chance its rule describes (`A && B || C && D` skipped it). */
    fun plantSpawnRoll(helper: GameTestHelper) {
        val spot = spot(helper)
        prepareGround(helper, spot)
        val pos = helper.absolutePos(spot)
        val trials = 400
        val allowed = (1..trials).count { Plant.checkValidSpawn(helper.level, pos, EntitySpawnReason.NATURAL, helper.level.random) }
        helper.assertTrue(allowed in (trials * 0.45).toInt()..(trials * 0.75).toInt(), "$allowed of $trials natural spawns allowed, expected about 60%")
        helper.succeed()
    }

    /**
     * Plants are AMBIENT, the list they spawn from, so they count against that cap and not the animals'; they may
     * still spawn far from players, as CREATURE types do.
     */
    fun plantsAreAmbient(helper: GameTestHelper) {
        val wrong = BuiltInRegistries.ENTITY_TYPE.filter { it.builtInRegistryHolder().`is`(joshxviii.plantz.PazTags.EntityTypes.PLANT) }
            .filter { it.category != net.minecraft.world.entity.MobCategory.AMBIENT || !it.canSpawnFarFromPlayer() }
            .map { "${BuiltInRegistries.ENTITY_TYPE.getKey(it).path} (${it.category})" }
        helper.assertTrue(wrong.isEmpty(), "plants that are not ambient or cannot spawn far from players: ${wrong.joinToString()}")
        helper.succeed()
    }

    /** A mob's Football Helmet wears when it blocks a projectile from above, as on a player (it never wore on mobs). */
    fun mobHelmetWears(helper: GameTestHelper) {
        val wearer = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        // a projectile above the wearer's eyes (melee attacks bypass head protection, #bypasses_head_protection)
        val snowball = EntityTypes.SNOWBALL.create(helper.level, EntitySpawnReason.COMMAND)!!
        snowball.snapTo(wearer.x, wearer.eyeY + 2.0, wearer.z, 0f, 0f)
        try {
            wearer.isNoAi = true
            wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.FOOTBALL_HELMET))
            val healthBefore = wearer.health
            wearer.hurtServer(helper.level, helper.level.damageSources().thrown(snowball, null), 4f)
            val helmet = wearer.getItemBySlot(EquipmentSlot.HEAD)
            helper.assertTrue(wearer.health == healthBefore, "the Football Helmet did not block the hit from above (health ${wearer.health} of $healthBefore)")
            helper.assertTrue(helmet.damageValue > 0, "the zombie's Football Helmet blocked a hit from above without wearing")
        } finally {
            wearer.discard()
            snowball.discard()
        }
        helper.succeed()
    }

    /**
     * A Hero of the Garden wave list is mutable whatever it was set from: loaded from a save it was the codec's
     * immutable list, and the Mailbox crashed the server clearing it after ejecting the rewards.
     */
    fun heroWaveListMutable(helper: GameTestHelper) {
        val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        try {
            val hero = zombie as joshxviii.plantz.raid.GardenHeroRewards
            // as the save loads it: an immutable list
            hero.`plantz$setWaveList`(java.util.Collections.unmodifiableList(arrayListOf(joshxviii.plantz.raid.CompletedWave(joshxviii.plantz.raid.WaveType.DEFAULT))))
            try {
                hero.`plantz$getWaveList`().clear() // what the Mailbox does after ejecting the rewards
            } catch (e: UnsupportedOperationException) {
                helper.fail("the loaded wave list cannot be cleared (${e.javaClass.simpleName})")
            }
        } finally {
            zombie.discard()
        }
        helper.succeed()
    }

    /** The saved copy of a worn plant follows the live plant (it stayed as it was when put on: armor and health came back). */
    fun wornPlantSnapshotFresh(helper: GameTestHelper) {
        val pos = spot(helper)
        prepareGround(helper, pos)
        lateinit var wearer: net.minecraft.world.entity.monster.zombie.Zombie
        lateinit var plant: Plant
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                wearer = helper.spawn(EntityTypes.ZOMBIE, pos)
                wearer.isNoAi = true
                wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.PLANT_POT_HELMET))
                plant = helper.spawn(PazEntities.SUNFLOWER, pos)
                helper.assertTrue(plant.attachToEntity(wearer), "the Sunflower did not attach")
                plant.health = 3f
                val saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, helper.level.registryAccess())
                wearer.saveWithoutId(saved)
                val snapshot = saved.buildResult().getCompoundOrEmpty("sproutsquad:AttachedPlant")
                val health = snapshot.getFloatOr("Health", -1f)
                helper.assertTrue(health == 3f, "the saved worn plant has health $health, the live one 3")
                clear(helper, pos)
            }
            .thenSucceed()
    }

    /** A plant converted by lightning keeps its owner even when the owner is not online (it became wild). */
    fun conversionKeepsOfflineOwner(helper: GameTestHelper) {
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(0, 1, 0))
        val area = AABB(helper.absolutePos(BlockPos(0, 1, 0))).inflate(2.0)
        val offline = UUID.randomUUID()
        try {
            plant.setTame(true, false)
            plant.setOwnerReference(net.minecraft.world.entity.EntityReference.of(offline))
            plant.convertToPlantType(PazEntities.ELECTRIC_PEA_SHOOTER)
            val converted = helper.level.getEntitiesOfClass(Plant::class.java, area) { it.type == PazEntities.ELECTRIC_PEA_SHOOTER }.singleOrNull()
            helper.assertTrue(converted != null, "the Peashooter was not converted")
            helper.assertTrue(converted!!.isTame && converted.ownerReference?.uuid == offline, "the converted plant lost its offline owner")
        } finally {
            helper.level.getEntitiesOfClass(Entity::class.java, area) { it !is Player }.forEach { it.discard() }
        }
        helper.succeed()
    }

    /** A letter's author is limited to what the network can carry (a longer one kicked the client). */
    fun letterAuthorLimit(helper: GameTestHelper) {
        val json = com.google.gson.JsonObject().apply { addProperty("author", "x".repeat(100)) }
        val result = LetterDataComponent.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json)
        helper.assertTrue(result.isError, "a 100-character author was accepted; the network limit is 64")
        helper.succeed()
    }

    /** A mailbox removed by anything but a player (an explosion, a zombie, a command) leaves the mailbox list. */
    fun removedMailboxUnlisted(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.MAILBOX)
        val absolute = helper.absolutePos(pos)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(MailboxManager.isRegistered(helper.level, absolute), "the placed mailbox is not registered yet") }
            .thenExecute {
                helper.setBlock(pos, Blocks.AIR) // not a player's break
                helper.assertTrue(!MailboxManager.isRegistered(helper.level, absolute), "a removed mailbox stays registered: mail to it would be queued for good")
                helper.setBlock(pos.below(), Blocks.AIR)
            }
            .thenSucceed()
    }

    /** Planting into a Pumpkin follows the plant limit (planting on the ground did, into a Pumpkin did not). */
    fun pumpkinPlantingLimit(helper: GameTestHelper) {
        val limits = PazConfig.server.customPlantEntityLimits
        val key = "${PazMain.MODID}:peashooter"
        val saved = limits[key]
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val area = AABB(helper.absolutePos(BlockPos(0, 1, 0))).inflate(3.0)
        try {
            limits[key] = 1
            helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(2, 1, 0)).tame(player) // the one Peashooter allowed
            val pumpkin = helper.spawn(PazEntities.PUMPKIN, BlockPos(0, 1, 0))
            pumpkin.tame(player)
            val packet = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER)
            player.inventory.add(ItemStack(PazItems.SUN, 32))
            val result = pumpkin.processSeedPacketInteraction(player, packet, InteractionHand.MAIN_HAND)
            val peashooters = helper.level.getEntitiesOfClass(Plant::class.java, area) { it.type == PazEntities.PEA_SHOOTER }.size
            helper.assertTrue(peashooters == 1 && result != PacketInteractionResult.SUCCESS, "a Peashooter over the limit was planted into the Pumpkin ($result, $peashooters Peashooters)")
        } finally {
            if (saved != null) limits[key] = saved else limits.remove(key)
            helper.level.getEntitiesOfClass(Entity::class.java, area) { it !is Player }.forEach { it.discard() }
        }
        helper.succeed()
    }

    /** A Gardening Glove that breaks from mining while holding a plant drops the plant as a seed packet. */
    fun gloveBreakKeepsPlant(helper: GameTestHelper) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val glove = ItemStack(PazItems.GARDENING_GLOVE)
        glove.set(net.minecraft.core.component.DataComponents.ENTITY_DATA, TypedEntityData.of(PazEntities.PEA_SHOOTER, CompoundTag()))
        glove.damageValue = glove.maxDamage - 1
        player.setItemInHand(InteractionHand.MAIN_HAND, glove)
        val pos = helper.absolutePos(BlockPos(0, 1, 0))
        player.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos), 0f, 0f) // the drop lands where the player is
        val area = AABB(player.blockPosition()).inflate(3.0)
        try {
            glove.item.mineBlock(glove, helper.level, Blocks.DIRT.defaultBlockState(), pos, player)
            helper.assertTrue(glove.isEmpty, "the glove did not break")
            val dropped = helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, area) {
                SeedPacketItem.typeFromStack(it.item) == PazEntities.PEA_SHOOTER
            }
            helper.assertTrue(dropped.isNotEmpty(), "the plant held by the broken glove was lost")
        } finally {
            helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, area).forEach { it.discard() }
        }
        helper.succeed()
    }

    /** The Mailbox menu closes when the player is out of reach (it stayed valid anywhere). */
    fun mailboxMenuReach(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.STONE)
        helper.setBlock(pos, PazBlocks.MAILBOX)
        try {
            val mailbox = helper.getBlockEntity(pos, MailboxBlockEntity::class.java)
            val player = helper.makeMockPlayer(GameType.SURVIVAL)
            val menu = joshxviii.plantz.inventory.MailboxMenu(0, player.inventory, mailbox.asMailBoxData(), mailbox)
            player.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(pos)).add(40.0, 0.0, 0.0))
            helper.assertTrue(!menu.stillValid(player), "the Mailbox menu stays valid 40 blocks away")
        } finally {
            helper.setBlock(pos, Blocks.AIR)
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /** Moving a Coffee Bean with a glove puts no planting cooldown on the glove (a glove move is not planting). */
    fun gloveGetsNoCooldown(helper: GameTestHelper) {
        val config = PazConfig.server
        val wasEnabled = config.plantCooldownEnabled
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val sleeper = helper.spawn(PazEntities.SUNFLOWER, BlockPos(0, 1, 0))
        try {
            config.plantCooldownEnabled = true
            sleeper.tame(player)
            sleeper.isAsleep = true
            val glove = ItemStack(PazItems.GARDENING_GLOVE)
            glove.set(net.minecraft.core.component.DataComponents.ENTITY_DATA, TypedEntityData.of(PazEntities.COFFEE_BEAN, CompoundTag()))
            player.inventory.add(ItemStack(PazItems.SUN, 32))
            sleeper.processSeedPacketInteraction(player, glove, InteractionHand.MAIN_HAND)
            helper.assertTrue(!glove.has(net.minecraft.core.component.DataComponents.USE_COOLDOWN), "the glove got the Coffee Bean's planting cooldown")
        } finally {
            config.plantCooldownEnabled = wasEnabled
            sleeper.discard()
        }
        helper.succeed()
    }

    /** A worn blocker does not send back piercing arrows (the original's changelog promises it); others still bounce. */
    fun blockerLetsPiercingThrough(helper: GameTestHelper) {
        val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        try {
            zombie.isNoAi = true
            zombie.setItemSlot(EquipmentSlot.CHEST, ItemStack(PazItems.COFFIN))
            fun arrowInFront(pierce: Int): net.minecraft.world.entity.projectile.arrow.AbstractArrow {
                val arrow = EntityTypes.ARROW.create(helper.level, EntitySpawnReason.COMMAND)!!
                TypedEntityData.of(EntityTypes.ARROW, CompoundTag().apply { putByte("PierceLevel", pierce.toByte()) }).loadInto(arrow)
                val front = zombie.position().add(net.minecraft.world.phys.Vec3.directionFromRotation(0f, zombie.yHeadRot).scale(2.0))
                arrow.snapTo(front.x, zombie.eyeY, front.z, 0f, 0f)
                return arrow
            }
            val plain = zombie.deflection(arrowInFront(0))
            val piercing = zombie.deflection(arrowInFront(1))
            helper.assertTrue(plain == net.minecraft.world.entity.projectile.ProjectileDeflection.REVERSE, "the Coffin did not send back a plain arrow ($plain)")
            helper.assertTrue(piercing == net.minecraft.world.entity.projectile.ProjectileDeflection.NONE, "the Coffin sent back a piercing arrow")
        } finally {
            zombie.discard()
        }
        helper.succeed()
    }

    /** Blocking sends back arrows and the mod's projectiles, not fireballs or potions (they were reflected too). */
    fun blockerSendsBackOnlyShots(helper: GameTestHelper) {
        val zombie = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        try {
            zombie.isNoAi = true
            zombie.setItemSlot(EquipmentSlot.CHEST, ItemStack(PazItems.COFFIN))
            val front = zombie.position().add(net.minecraft.world.phys.Vec3.directionFromRotation(0f, zombie.yHeadRot).scale(2.0))
            for (type in listOf(EntityTypes.FIREBALL, EntityTypes.SPLASH_POTION, EntityTypes.WIND_CHARGE)) {
                val projectile = type.create(helper.level, EntitySpawnReason.COMMAND)!!
                projectile.snapTo(front.x, zombie.eyeY, front.z, 0f, 0f)
                val deflection = zombie.deflection(projectile)
                helper.assertTrue(deflection == net.minecraft.world.entity.projectile.ProjectileDeflection.NONE,
                    "blocking sent back ${BuiltInRegistries.ENTITY_TYPE.getKey(type).path}")
            }
        } finally {
            zombie.discard()
        }
        helper.succeed()
    }

    /** A Watering Can with 1 water left gives 1 water (it gave 2). */
    fun wateringCanLastDrop(helper: GameTestHelper) {
        val pos = BlockPos(0, 2, 0)
        helper.setBlock(pos.below(), Blocks.FARMLAND)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val plant = helper.spawn(PazEntities.SUNFLOWER, pos)
        try {
            plant.tame(player)
            plant.receivedWater = 0
            val can = ItemStack(PazItems.WATERING_CAN)
            can.set(PazComponents.STORED_WATER, joshxviii.plantz.item.component.StoredWater(storedWater = 1))
            player.setItemInHand(InteractionHand.MAIN_HAND, can)
            plant.processWateringItem(player, can, InteractionHand.MAIN_HAND, joshxviii.plantz.entity.plant.PlantGrowNeeds.WATER)
            helper.assertTrue(plant.receivedWater == 1, "1 water in the can gave ${plant.receivedWater}")
        } finally {
            plant.discard()
            helper.setBlock(pos.below(), Blocks.AIR)
        }
        helper.succeed()
    }

    /** A worn plant whose wearer is gone (a portal, a conversion, a despawn) drops as its seed packet (it fell through the world). */
    fun wearerGoneDropsPlant(helper: GameTestHelper) {
        val wearer = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        wearer.isNoAi = true
        wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.PLANT_POT_HELMET))
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(0, 1, 0))
        val area = AABB(helper.absolutePos(BlockPos(0, 1, 0))).inflate(4.0)
        helper.assertTrue(plant.attachToEntity(wearer), "the Peashooter did not go on the helmet")
        wearer.discard() // gone without taking the plant along
        helper.startSequence()
            .thenWaitUntil {
                val packets = helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, area) {
                    SeedPacketItem.typeFromStack(it.item) == PazEntities.PEA_SHOOTER
                }
                helper.assertTrue(plant.isRemoved && packets.isNotEmpty(),
                    "the plant of a vanished wearer did not drop (removed: ${plant.isRemoved}, at y ${"%.1f".format(plant.y)})")
            }
            .thenExecute { helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, area).forEach { it.discard() } }
            .thenSucceed()
    }

    /** A wearer riding something keeps its plant on its head in the same tick (only wearers on foot were followed). */
    fun ridingWearerPlantFollows(helper: GameTestHelper) {
        val plant = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(0, 1, 0)) // ticks before the wearer
        val cart = helper.spawn(EntityTypes.MINECART, BlockPos(0, 1, 0))
        val wearer = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        wearer.isNoAi = true
        wearer.setItemSlot(EquipmentSlot.HEAD, ItemStack(PazItems.PLANT_POT_HELMET))
        helper.assertTrue(wearer.startRiding(cart, true, false) && plant.attachToEntity(wearer), "the wearer is not riding the cart with its plant")
        helper.startSequence()
            .thenExecute { cart.deltaMovement = net.minecraft.world.phys.Vec3(0.6, 0.0, 0.0) }
            .thenIdle(1)
            .thenExecute {
                val before = plant.position()
                wearer.positionPlant(plant)
                helper.assertTrue(plant.position().distanceToSqr(before) < 1.0E-6, "the plant lagged behind its riding wearer by ${"%.2f".format(plant.position().distanceTo(before))} blocks")
                listOf(plant, wearer, cart).forEach { it.discard() }
            }
            .thenSucceed()
    }

    /** A dispenser follows the plant limit and plants only into a free spot (it planted into solid blocks). */
    fun dispenserLimitAndSpot(helper: GameTestHelper) {
        val limits = PazConfig.server.customPlantEntityLimits
        val key = BuiltInRegistries.ENTITY_TYPE.getKey(PazEntities.PUFF_SHROOM).toString()
        val saved = limits[key]
        val dispenserPos = BlockPos(0, 2, 0)
        val front = dispenserPos.east()
        val east = net.minecraft.core.Direction.EAST
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, east))
        helper.setBlock(dispenserPos.above(), PazBlocks.SUN_BATTERY_BLOCK)
        helper.setBlock(front.below(), Blocks.GRASS_BLOCK)
        // the owner the dispenser looks for: a player in the level, without a login (and removed before it ticks)
        val player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.level, com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "dispenser-owner"))
        player.snapTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(BlockPos(0, 1, 3))), 0f, 0f)
        helper.level.addNewPlayer(player)
        val area = AABB(helper.absolutePos(front)).inflate(3.0)
        fun dispense(): Boolean {
            val packet = SeedPacketItem.stackFor(PazEntities.PUFF_SHROOM)
            packet.remove(PazComponents.SUN_COST) // the fresh battery has no sun
            return SeedPacketItem.tryPlantFromDispenser(helper.level, packet, helper.absolutePos(dispenserPos), helper.absolutePos(front), east, east)
        }
        try {
            helper.setBlock(front, Blocks.STONE)
            helper.assertTrue(!dispense(), "the dispenser planted into stone")
            helper.setBlock(front, Blocks.AIR)
            limits[key] = 1
            helper.spawn(PazEntities.PUFF_SHROOM, BlockPos(0, 1, 2)).tame(player) // the one Puff-shroom allowed
            helper.assertTrue(!dispense(), "the dispenser planted over the plant limit")
            limits.remove(key)
            if (saved != null) limits[key] = saved
            helper.assertTrue(dispense(), "the dispenser did not plant into a free spot under the limit")
        } finally {
            if (saved != null) limits[key] = saved else limits.remove(key)
            helper.level.getEntitiesOfClass(Plant::class.java, area).forEach { it.discard() }
            helper.level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED)
            for (p in listOf(dispenserPos.above(), dispenserPos, front, front.below())) helper.setBlock(p, Blocks.AIR)
        }
        helper.succeed()
    }

    /** A dispenser plants nothing with no player near to own the plant (it planted a wild one, which may attack players). */
    fun dispenserNeedsOwner(helper: GameTestHelper) {
        val dispenserPos = BlockPos(0, 2, 0)
        val front = dispenserPos.east()
        helper.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.EAST))
        helper.setBlock(dispenserPos.above(), PazBlocks.SUN_BATTERY_BLOCK) // on top of the dispenser, supported by it
        helper.setBlock(front.below(), Blocks.GRASS_BLOCK)
        helper.setBlock(front, Blocks.AIR)
        val area = AABB(helper.absolutePos(front)).inflate(1.0)
        try {
            helper.assertTrue(helper.level.getNearestPlayer(helper.absolutePos(front).x.toDouble(), helper.absolutePos(front).y.toDouble(), helper.absolutePos(front).z.toDouble(), 64.0, false) == null, "a player is near the test")
            val packet = SeedPacketItem.stackFor(PazEntities.PUFF_SHROOM)
            packet.remove(PazComponents.SUN_COST) // the fresh battery has no sun
            val planted = SeedPacketItem.tryPlantFromDispenser(helper.level, packet, helper.absolutePos(dispenserPos), helper.absolutePos(front),
                net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.EAST)
            val plants = helper.level.getEntitiesOfClass(Plant::class.java, area)
            helper.assertTrue(!planted && plants.isEmpty(), "the dispenser planted a plant nobody owns (${plants.map { it.isTame }})")
        } finally {
            helper.level.getEntitiesOfClass(Plant::class.java, area).forEach { it.discard() }
            for (p in listOf(dispenserPos.above(), dispenserPos, front, front.below())) helper.setBlock(p, Blocks.AIR)
        }
        helper.succeed()
    }

    /** Taking sun out of a Solar Battery onto a slot respects what the slot takes (64 went into a 1-item slot). */
    fun batteryRespectsSlot(helper: GameTestHelper) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        val letter = ItemStack(PazItems.LETTER)
        player.inventory.setItem(0, letter)
        player.inventory.selectedSlot = 0
        val menu = LetterMenu(0, player.inventory, 0)
        val battery = ItemStack(PazItems.SUN_BATTERY)
        battery.set(PazComponents.STORED_SUN, joshxviii.plantz.item.component.StoredSun(storedSun = 100))
        battery.item.overrideStackedOnOther(battery, menu.attachmentSlot, net.minecraft.world.inventory.ClickAction.SECONDARY, player)
        val placed = menu.attachmentSlot.item.count
        val left = battery.get(PazComponents.STORED_SUN)!!.storedSun
        helper.assertTrue(placed == 1 && left == 99, "the battery put $placed sun into a 1-item slot ($left left in it)")
        helper.succeed()
    }

    /** Each flag has its own crack overlay id, never an entity's (all flags shared 0: one crack showed at a time). */
    fun flagCrackIds(helper: GameTestHelper) {
        val a = joshxviii.plantz.block.entity.FlagBlockEntity.crackId(BlockPos(10, 64, 10))
        val b = joshxviii.plantz.block.entity.FlagBlockEntity.crackId(BlockPos(12, 64, 10))
        helper.assertTrue(a != b && a < 0 && b < 0, "flag crack ids $a and $b")
        helper.succeed()
    }

    /** Villagers flee zombies, but not hypnotized ones or players' machines (they are on the players' side). */
    fun villagersTrustAllies(helper: GameTestHelper) {
        val villager = helper.spawn(EntityTypes.VILLAGER, BlockPos(0, 1, 0))
        val zombie = helper.spawn(PazEntities.BROWN_COAT, BlockPos(2, 1, 0))
        val hypnotized = helper.spawn(PazEntities.BROWN_COAT, BlockPos(0, 1, 2))
        val machine = helper.spawn(PazEntities.ZOMBIE_TURRET, BlockPos(2, 1, 2))
        try {
            listOf(villager, zombie, hypnotized, machine).forEach { it.isNoAi = true }
            hypnotized.addEffect(MobEffectInstance(PazEffects.HYPNOTIZE, 600))
            machine.setData(dev.efureev.sproutsquad.registry.SquadAttachments.PLAYER_MACHINE, true)
            val sensor = net.minecraft.world.entity.ai.sensing.SensorType.VILLAGER_HOSTILES.create()
            val matches = net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor::class.java.getDeclaredMethod(
                "isMatchingEntity", ServerLevel::class.java, LivingEntity::class.java, LivingEntity::class.java
            ).apply { isAccessible = true }
            fun fears(mob: LivingEntity) = matches.invoke(sensor, helper.level, villager, mob) as Boolean
            helper.assertTrue(fears(zombie), "a villager does not fear a Browncoat")
            helper.assertTrue(!fears(hypnotized), "a villager fears a hypnotized zombie")
            helper.assertTrue(!fears(machine), "a villager fears a player's machine")
        } finally {
            listOf(villager, zombie, hypnotized, machine).forEach { it.discard() }
        }
        helper.succeed()
    }

    /** Vanilla's leader bonus stays on zombie villagers; the mod's non-leader zombies still lose it. */
    fun leaderBonusOnlyOffModZombies(helper: GameTestHelper) {
        val calm = net.minecraft.world.DifficultyInstance(net.minecraft.world.Difficulty.PEACEFUL, 0L, 0L, 0f) // no new bonus rolled
        fun bonusKept(type: EntityType<out Zombie>): Boolean {
            val zombie = type.create(helper.level, EntitySpawnReason.COMMAND)!!
            zombie.snapTo(helper.absolutePos(BlockPos(0, 1, 0)), 0f, 0f)
            zombie.getAttribute(Attributes.MAX_HEALTH)!!.addPermanentModifier(
                AttributeModifier(PazZombie.LEADER_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))
            zombie.finalizeSpawn(helper.level, calm, EntitySpawnReason.NATURAL, Zombie.ZombieGroupData(false, false))
            val kept = PazZombie.isBornLeader(zombie)
            zombie.discard()
            return kept
        }
        helper.assertTrue(bonusKept(EntityTypes.ZOMBIE_VILLAGER), "a zombie villager lost vanilla's leader bonus")
        helper.assertTrue(!bonusKept(PazEntities.ALL_STAR), "an All-Star kept vanilla's leader bonus")
        helper.succeed()
    }

    /** A block with an item that is not the mod's keeps vanilla's knockback; the mod's own blockers cancel it. */
    fun blockKnockbackOnlyForModBlockers(helper: GameTestHelper) {
        val attacker = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 0))
        val plain = helper.spawn(EntityTypes.ZOMBIE, BlockPos(2, 1, 0))
        val coffin = helper.spawn(EntityTypes.ZOMBIE, BlockPos(0, 1, 2))
        try {
            listOf(attacker, plain, coffin).forEach { it.isNoAi = true; it.deltaMovement = net.minecraft.world.phys.Vec3.ZERO }
            coffin.setItemSlot(EquipmentSlot.CHEST, ItemStack(PazItems.COFFIN))
            val blockedByItem = LivingEntity::class.java.getDeclaredMethod("blockedByItem", LivingEntity::class.java,
                net.minecraft.world.damagesource.DamageSource::class.java, Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
            ).apply { isAccessible = true }
            val source = helper.level.damageSources().mobAttack(attacker)
            blockedByItem.invoke(attacker, plain, source, 2f, false)
            blockedByItem.invoke(attacker, coffin, source, 2f, false)
            helper.assertTrue(plain.deltaMovement.horizontalDistanceSqr() > 0.0, "a block with no mod item lost vanilla's knockback")
            helper.assertTrue(coffin.deltaMovement.horizontalDistanceSqr() == 0.0, "a block with the Coffin knocked its wearer back")
        } finally {
            listOf(attacker, plain, coffin).forEach { it.discard() }
        }
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
