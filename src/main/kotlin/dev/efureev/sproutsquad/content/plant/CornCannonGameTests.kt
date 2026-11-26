package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.projectile.CornCob
import dev.efureev.sproutsquad.registry.SquadEntities
import dev.efureev.sproutsquad.registry.SquadAttachments
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.SunCost
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.util.Mth
import net.minecraft.util.ProblemReporter
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * GameTests of the Corn Cannon (docs/en/characters/corn_cannon.md). Registered in SquadGameTests.
 * The lane tests run in `gametest/lane` (26x6x3), as the Needle Spruce's do.
 */
object CornCannonGameTests {
    /** Length of the test strip along +x: the cannon at 0, targets further on. */
    private const val STRIP = 24

    /** Health, range, price, limit and the splash falloff from the design. */
    fun stats(helper: GameTestHelper) {
        val type = SquadEntities.CORN_CANNON.get()
        val cannon = type.create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Corn Cannon does not spawn")
        helper.assertValueEqual(cannon.maxHealth, 90f, "max health")
        helper.assertValueEqual(cannon.getAttributeValue(Attributes.FOLLOW_RANGE), 40.0, "range")
        helper.assertValueEqual(PazConfig.getSunCost(type), 14, "sun cost")
        helper.assertValueEqual(PazConfig.getEntityLimit(type), 10, "entity limit")
        helper.assertValueEqual(CornCob.splashDamage(0.0), 8f, "damage at the centre")
        helper.assertValueEqual(CornCob.splashDamage(1.25), 6f, "damage half way out")
        helper.assertValueEqual(CornCob.splashDamage(2.5), 4f, "damage at the edge")
        helper.assertValueEqual(CornCob.splashDamage(2.6), 0f, "damage past the edge")
        helper.assertTrue(CornCannon.ACTION_DELAY + CornCannon.SETTLE_TICKS >= CornCannon.SHOT_ANIMATION_TICKS,
            "a called-off shot settles before its animation ends")
        helper.succeed()
    }

    /** The face squints while aiming and firing (the first 700 ms), and is plain again after it and while dying (the renderer adds `sleep`). */
    fun face(helper: GameTestHelper) {
        val cannon = SquadEntities.CORN_CANNON.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Corn Cannon does not spawn")
        helper.assertValueEqual(cannon.faceSuffix(null), "", "no action")
        helper.assertValueEqual(cannon.faceSuffix(100L), "aim", "100 ms into a shot")
        helper.assertValueEqual(cannon.faceSuffix(900L), "", "900 ms into a shot, the cob long gone")
        cannon.deathTime = 5
        helper.assertValueEqual(cannon.faceSuffix(100L), "", "dying")
        helper.succeed()
    }

    /** A zombie 10 blocks away gets corn cobs, one shot per 70 ticks, and loses health. */
    fun fires(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: Entity
        val shots = mutableListOf<Long>()
        val seen = mutableSetOf<Int>()
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                wall(helper, 12)
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenWaitUntil {
                if (cobsOf(helper, cannon).any { seen.add(it.id) }) shots += helper.level.gameTime
                helper.assertTrue(shots.size >= 2, "waiting for two shots, seen ${shots.size}")
            }
            .thenExecute {
                helper.assertTrue(shots[1] - shots[0] >= 70, "shots ${shots[1] - shots[0]} ticks apart, expected 70")
            }
            .thenWaitUntil {
                val brownCoat = zombie as net.minecraft.world.entity.LivingEntity
                helper.assertTrue(!brownCoat.isAlive || brownCoat.health < brownCoat.maxHealth, "the zombie was not hurt")
            }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /**
     * It does not see zombies within 5 blocks: no shot at one 4 blocks away, and a zombie that comes
     * within 3 blocks after the first shot gets no more and is dropped as a target.
     */
    fun deadZone(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: Entity
        val seen = mutableSetOf<Int>()
        var afterMove = 0
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(5, 0, 1))
            }
            .thenExecuteFor(100) {
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "fired at a zombie 4 blocks away")
            }
            .thenExecute {
                helper.assertTrue(cannon.target == null, "targets a zombie 4 blocks away")
                clearMobs(helper)
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                wall(helper, 12)
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenWaitUntil {
                cobsOf(helper, cannon).forEach { seen.add(it.id) }
                helper.assertTrue(seen.isNotEmpty(), "waiting for the first shot")
            }
            .thenExecute {
                val pos = helper.absoluteVec(Vec3(4.5, 0.0, 1.5))
                zombie.teleportTo(pos.x, pos.y, pos.z)
            }
            .thenExecuteFor(100) {
                afterMove += cobsOf(helper, cannon).count { seen.add(it.id) }
            }
            .thenExecute {
                helper.assertValueEqual(afterMove, 0, "shots at a zombie 3 blocks away")
                helper.assertTrue(cannon.target == null, "still targets a zombie 3 blocks away")
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /**
     * The explosion hurts only what the cannon would shoot at: a zombie, not a villager, a wolf or a plant
     * next to it, and a player is not even a valid hit. The floor stays.
     */
    fun sparesFriends(helper: GameTestHelper) {
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                val cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(20, 0, 1))
                val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(5, 0, 1))
                val villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, BlockPos(6, 0, 1))
                val wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, BlockPos(4, 0, 1))
                val peashooter = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(5, 0, 2))
                val player: Player = helper.makeMockPlayer(GameType.SURVIVAL)
                val cob = CornCob(helper.level, cannon)
                val centre = helper.absoluteVec(Vec3(5.5, 0.5, 1.5))
                cob.setPos(centre)
                helper.assertFalse(cob.hits(player), "a player is a valid hit")
                helper.assertFalse(cob.hits(villager), "a villager is a valid hit")
                helper.assertTrue(cob.hits(zombie), "a zombie is not a valid hit")
                cob.explodeAt(centre)
                helper.assertTrue(zombie.health < zombie.maxHealth, "the zombie was not hurt")
                helper.assertValueEqual(villager.health, villager.maxHealth, "villager health")
                helper.assertValueEqual(wolf.health, wolf.maxHealth, "wolf health")
                helper.assertValueEqual(peashooter.health, peashooter.maxHealth, "peashooter health")
                for (x in 3..7) helper.assertBlockPresent(Blocks.GRASS_BLOCK, BlockPos(x, -1, 1))
                cob.discard()
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /**
     * A one-block wall right in front of the cannon: the cob still leaves the husk and hits the zombie
     * behind it (a cob that started inside the wall block vanished on its first tick).
     */
    fun wallInFront(helper: GameTestHelper) {
        lateinit var zombie: LivingEntity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                helper.setBlock(BlockPos(2, 0, 1), Blocks.STONE)
                wall(helper, 12)
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenWaitUntil { helper.assertTrue(!zombie.isAlive || zombie.health < zombie.maxHealth, "the zombie behind the wall was not hurt") }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /**
     * A cob keeps knowing its cannon after a save and load (a chunk unloaded mid-flight): it still spares a
     * villager and the cannon itself. A cob with no cannon at all hits only zombies.
     */
    fun cobAfterReload(helper: GameTestHelper) {
        val cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
        val villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, BlockPos(3, 0, 1))
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(5, 0, 1))
        val fired = CornCob(helper.level, cannon)
        val saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.level.registryAccess())
        fired.saveWithoutId(saved)
        val loaded = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.LOAD)
            ?: return helper.fail("no corn cob")
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), saved.buildResult()))
        helper.assertFalse(loaded.hits(villager), "a reloaded cob hits a villager")
        helper.assertFalse(loaded.hits(cannon), "a reloaded cob hits its own cannon")
        helper.assertTrue(loaded.hits(zombie), "a reloaded cob misses a zombie")
        val stray = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("no corn cob")
        helper.assertFalse(stray.hits(villager), "a cob without a cannon hits a villager")
        helper.assertTrue(stray.hits(zombie), "a cob without a cannon misses a zombie")
        listOf(fired, loaded, stray).forEach { it.discard() }
        clearMobs(helper) // a live cannon left behind would shoot at the next tests' zombies
        helper.succeed()
    }

    /**
     * A zombie under a roof (in a cave, a house) is not a target: the cob would only hit the roof. Once the
     * roof is gone, the cannon fires at it.
     */
    fun coveredTarget(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                wall(helper, 12)
                helper.setBlock(BlockPos(11, 3, 1), Blocks.STONE)
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenExecuteFor(100) {
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "fired at a zombie under a roof")
            }
            .thenExecute {
                helper.assertTrue(cannon.target == null, "targets a zombie under a roof")
                helper.setBlock(BlockPos(11, 3, 1), Blocks.AIR)
            }
            .thenWaitUntil { helper.assertTrue(cobsOf(helper, cannon).isNotEmpty(), "no shot once the roof is gone") }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /**
     * A hypnotized zombie and a player's machine are allies: never a target, and never hit, also by a cob that
     * was reloaded or has no cannon.
     */
    fun sparesAllies(helper: GameTestHelper) {
        val cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
        val zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(5, 0, 1))
        val hypnotized = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(7, 0, 1))
        hypnotized.addEffect(MobEffectInstance(PazEffects.HYPNOTIZE, 600))
        val machine = helper.spawnWithNoFreeWill(PazEntities.ZOMBIE_TURRET, BlockPos(9, 0, 1))
        machine.setData(SquadAttachments.PLAYER_MACHINE, true)
        helper.assertFalse(cannon.isValidTarget(hypnotized), "targets a hypnotized zombie")
        helper.assertFalse(cannon.isValidTarget(machine), "targets a player's machine")
        val fired = CornCob(helper.level, cannon)
        val saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.level.registryAccess())
        fired.saveWithoutId(saved)
        val loaded = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.LOAD)
            ?: return helper.fail("no corn cob")
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), saved.buildResult()))
        val stray = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("no corn cob")
        listOf("fired" to fired, "reloaded" to loaded, "stray" to stray).forEach { (name, cob) ->
            helper.assertTrue(cob.hits(zombie), "a $name cob misses a zombie")
            helper.assertFalse(cob.hits(hypnotized), "a $name cob hits a hypnotized zombie")
            helper.assertFalse(cob.hits(machine), "a $name cob hits a player's machine")
            cob.discard()
        }
        clearMobs(helper) // a live cannon left behind would shoot at the next tests' zombies
        helper.succeed()
    }

    /**
     * A shot called off during the aim (the zombie came within 5 blocks) uses up no cob: no cob leaves, the cob
     * stays full and the cannon only waits a moment while its nose comes down.
     */
    fun cancelledShot(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: Entity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                cannon.cooldown = 0
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenWaitUntil { helper.assertValueEqual(cannon.cooldown, -1, "aiming") }
            .thenExecute {
                val pos = helper.absoluteVec(Vec3(4.5, 0.0, 1.5))
                zombie.teleportTo(pos.x, pos.y, pos.z)
            }
            .thenWaitUntil { helper.assertTrue(cannon.cooldown != -1, "still aiming") }
            .thenExecute {
                helper.assertTrue(cannon.cooldown in 1..CornCannon.SETTLE_TICKS, "a wait of ${cannon.cooldown} ticks after a called-off shot, expected a short settle")
                helper.assertValueEqual(cannon.cobGrowth(), 1f, "cob after a called-off shot")
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "a cob left on a called-off shot")
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /**
     * The cob grows back over the reload the shot really started (a powered-up one is shorter), even if the power-up
     * comes or goes during it; after a called-off shot the cob is full.
     */
    fun cobGrowth(helper: GameTestHelper) {
        val cannon = SquadEntities.CORN_CANNON.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Corn Cannon does not spawn")
        cannon.cooldown = 0
        helper.assertValueEqual(cannon.cobGrowth(), 1f, "idle")
        cannon.cooldown = -1
        helper.assertValueEqual(cannon.cobGrowth(), 1f, "aiming")
        cannon.reloadStarted(CornCannon.COOLDOWN)
        cannon.cooldown = CornCannon.COOLDOWN
        helper.assertValueEqual(cannon.cobGrowth(), 0f, "right after a shot")
        cannon.cooldown = CornCannon.COOLDOWN / 2
        helper.assertValueEqual(cannon.cobGrowth(), 0.5f, "half way")
        cannon.poweredUp = true
        helper.assertValueEqual(cannon.cobGrowth(), 0.5f, "half way, powered up during the reload")
        val powered = Mth.floor(CornCannon.COOLDOWN * PazConfig.POWERED_UP_COOLDOWN_REDUCTION)
        cannon.reloadStarted(powered)
        cannon.cooldown = powered
        helper.assertValueEqual(cannon.cobGrowth(), 0f, "right after a powered-up shot")
        cannon.cooldown = 8
        cannon.reloadStarted(0)
        helper.assertValueEqual(cannon.cobGrowth(), 1f, "settling after a called-off shot")
        helper.succeed()
    }

    /** A wall between the blast and a zombie shields it; a zombie in the open on the other side is hurt. */
    fun blastBehindWall(helper: GameTestHelper) {
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                val cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(20, 0, 1))
                for (y in 0..2) helper.setBlock(BlockPos(5, y, 1), Blocks.STONE)
                val shielded = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(6, 0, 1))
                val open = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(3, 0, 1))
                val cob = CornCob(helper.level, cannon)
                cob.explodeAt(helper.absoluteVec(Vec3(4.7, 0.5, 1.5)))
                helper.assertValueEqual(shielded.health, shielded.maxHealth, "health behind the wall")
                helper.assertTrue(open.health < open.maxHealth, "the zombie in the open was not hurt")
                cob.discard()
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /** A bigger cannon (the scale attribute) makes a bigger blast: radius times the square root of the scale. */
    fun scaledBlast(helper: GameTestHelper) {
        val cob = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("no corn cob")
        helper.assertValueEqual(cob.blastRadius(), CornCob.RADIUS, "radius at scale 1")
        cob.scale = 4f
        helper.assertValueEqual(cob.blastRadius(), 2 * CornCob.RADIUS, "radius at scale 4")
        helper.assertValueEqual(CornCob.splashDamage(2.5, 5.0), 6f, "half way out of a radius of 5")
        helper.assertValueEqual(CornCob.splashDamage(5.1, 5.0), 0f, "past a radius of 5")
        cob.discard()
        helper.succeed()
    }

    /** A cob that lands on a target block powers it, as other projectiles do. */
    fun hitsTargetBlock(helper: GameTestHelper) {
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                helper.setBlock(BlockPos(5, 0, 1), Blocks.TARGET)
                val cob = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)!!
                cob.setPos(helper.absoluteVec(Vec3(5.5, 3.0, 1.5)))
                cob.deltaMovement = Vec3(0.0, -0.6, 0.0)
                helper.level.addFreshEntity(cob)
            }
            .thenWaitUntil {
                val power = helper.getBlockState(BlockPos(5, 0, 1)).getValue(BlockStateProperties.POWER)
                helper.assertTrue(power > 0, "the target block is not powered")
            }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /** Leaves over a zombie are a roof too: the cob would burst on the canopy. Not a target. */
    fun leafRoof(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                helper.setBlock(BlockPos(11, 3, 1), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true))
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenExecuteFor(100) {
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "fired at a zombie under leaves")
            }
            .thenExecute {
                helper.assertTrue(cannon.target == null, "targets a zombie under leaves")
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /** Scaffolding over a zombie is a roof: the cob comes down on its top plate. Not a target. */
    fun scaffoldingRoof(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: LivingEntity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                // a scaffolding post next to the zombie and an arm over it (unsupported scaffolding falls)
                for (y in 0..2) helper.setBlock(BlockPos(12, y, 1), Blocks.SCAFFOLDING)
                helper.setBlock(BlockPos(11, 2, 1), Blocks.SCAFFOLDING)
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
                helper.assertFalse(cannon.canShootAt(zombie), "a zombie under scaffolding is a target")
            }
            .thenExecuteFor(100) {
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "fired at a zombie under scaffolding")
            }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /**
     * Blasts that keep missing the target (the cob bursts on an overhang next to it) make the cannon leave that
     * target alone for a while: three misses in a row; a hit in between starts the count again.
     */
    fun missesDropTarget(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: LivingEntity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper) // open sky: the test structure is closed on top
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
                cannon.target = zombie
                cannon.blastMissed(zombie)
                cannon.blastMissed(zombie)
                cannon.blastHit(zombie)
                cannon.blastMissed(zombie)
                cannon.blastMissed(zombie)
                helper.assertTrue(cannon.target === zombie, "dropped the target after a hit and two misses")
                cannon.blastMissed(zombie)
                helper.assertTrue(cannon.target == null, "kept the target after three misses in a row")
                helper.assertFalse(cannon.canShootAt(zombie), "shoots again right after three misses")
            }
            .thenIdle(CornCannon.MISS_IGNORE_TICKS + 1)
            .thenExecute {
                helper.assertTrue(cannon.canShootAt(zombie), "still ignores the target after the pause")
                clearMobs(helper)
            }
            .thenSucceed()
    }

    /**
     * Water is no roof: the cob flies through it without slowing down. A zombie held at the bottom of a pool
     * eight blocks deep is a target, and gets hurt.
     */
    fun zombieInWater(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        lateinit var zombie: LivingEntity
        val pool = (-9..-1).flatMap { y -> listOf(BlockPos(10, y, 1), BlockPos(12, y, 1), BlockPos(11, y, 0), BlockPos(11, y, 2), BlockPos(11, y, 1)) }
        val before = pool.associateWith { helper.getBlockState(it) }
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                pool.forEach { helper.setBlock(it, Blocks.STONE) }
                for (y in -8..-1) helper.setBlock(BlockPos(11, y, 1), Blocks.WATER)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, -8, 1))
                zombie.isNoGravity = true
                helper.assertTrue(cannon.canShootAt(zombie), "a zombie at the bottom of a pool is not a target")
            }
            .thenWaitUntil { helper.assertTrue(zombie.health < zombie.maxHealth, "the zombie in the water was not hurt") }
            .thenExecute {
                clearMobs(helper)
                before.forEach { (pos, state) -> helper.setBlock(pos, state) } // the pool reaches below the structure
            }
            .thenSucceed()
    }

    /**
     * The cob keeps its speed in water (the original's projectiles lose a fifth of it every tick there): fired at
     * a zombie on the bottom of a lake, it would otherwise stall at the surface and sink short of it.
     */
    fun cobKeepsSpeedInWater(helper: GameTestHelper) {
        lateinit var cob: CornCob
        lateinit var start: Vec3
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                // a closed glass trough: water must not flow out into the neighbouring tests
                for (x in 1..15) for (y in 0..3) for (z in 0..2) helper.setBlock(BlockPos(x, y, z), Blocks.GLASS)
                for (x in 2..14) for (y in 0..2) helper.setBlock(BlockPos(x, y, 1), Blocks.WATER)
                cob = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)!!
                start = helper.absoluteVec(Vec3(3.5, 1.0, 1.5))
                cob.setPos(start)
                cob.isNoGravity = true
                cob.deltaMovement = Vec3(0.5, 0.0, 0.0)
                helper.level.addFreshEntity(cob)
            }
            .thenIdle(6)
            .thenExecute {
                val travelled = cob.x - start.x
                helper.assertTrue(travelled >= 2.5, "the cob travelled $travelled blocks in 6 ticks of water, expected 3")
                cob.discard()
                clearMobs(helper)
                for (x in 1..15) for (y in 0..3) for (z in 0..2) helper.setBlock(BlockPos(x, y, z), Blocks.AIR)
            }
            .thenSucceed()
    }

    /** A cannon under a roof does not shoot (every cob would burst on its own ceiling); without the roof it does. */
    fun cannonUnderRoof(helper: GameTestHelper) {
        lateinit var cannon: CornCannon
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper), "strip is not ticking yet") }
            .thenExecute {
                prepare(helper)
                cannon = helper.spawn(SquadEntities.CORN_CANNON.get(), BlockPos(1, 0, 1))
                helper.setBlock(BlockPos(1, 2, 1), Blocks.STONE)
                wall(helper, 12)
                helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(11, 0, 1))
            }
            .thenExecuteFor(100) {
                helper.assertTrue(cobsOf(helper, cannon).isEmpty(), "a cannon under a roof fired")
            }
            .thenExecute {
                helper.assertTrue(cannon.target == null, "a cannon under a roof has a target")
                helper.setBlock(BlockPos(1, 2, 1), Blocks.AIR)
            }
            .thenWaitUntil { helper.assertTrue(cobsOf(helper, cannon).isNotEmpty(), "no shot once the roof is gone") }
            .thenExecute { clearMobs(helper) }
            .thenSucceed()
    }

    /** A cob keeps its size through a save and load, and with it its blast radius. */
    fun scaleSaved(helper: GameTestHelper) {
        val cob = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("no corn cob")
        cob.scale = 4f
        val saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.level.registryAccess())
        cob.saveWithoutId(saved)
        val loaded = SquadEntities.CORN_COB.get().create(helper.level, EntitySpawnReason.LOAD)
            ?: return helper.fail("no corn cob")
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.level.registryAccess(), saved.buildResult()))
        helper.assertValueEqual(loaded.scale, 4f, "scale after a reload")
        helper.assertValueEqual(loaded.blastRadius(), 2 * CornCob.RADIUS, "radius after a reload")
        listOf(cob, loaded).forEach { it.discard() }
        helper.succeed()
    }

    /** A Kernel Pult seed packet in the middle of 8 sun makes a Corn Cannon packet; a Peashooter packet does not. */
    fun recipe(helper: GameTestHelper) {
        fun craft(packet: EntityType<*>): ItemStack {
            val sun = { ItemStack(PazItems.SUN) }
            // a packet from the creative tab or a shovel carries its price as well
            val seed = SeedPacketItem.stackFor(packet).also { it.set(PazComponents.SUN_COST, SunCost(6)) }
            val input = CraftingInput.of(3, 3, listOf(
                sun(), sun(), sun(),
                sun(), seed, sun(),
                sun(), sun(), sun(),
            ))
            return helper.level.server.recipeManager.getRecipeFor(RecipeType.CRAFTING, input, helper.level)
                .map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
        }
        val made = craft(PazEntities.KERNEL_PULT)
        helper.assertTrue(SeedPacketItem.typeFromStack(made) == SquadEntities.CORN_CANNON.get(), "a Kernel Pult packet made $made")
        helper.assertTrue(craft(PazEntities.PEA_SHOOTER).isEmpty, "a Peashooter packet made ${craft(PazEntities.PEA_SHOOTER)}")
        helper.succeed()
    }

    /** The Poppy Rattle never hands out a Corn Cannon: it comes only from the recipe. */
    fun notFromPoppyRattle(helper: GameTestHelper) {
        val type = SquadEntities.CORN_CANNON.get()
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).`is`(PoppyRattle.EXCLUDED), "not in poppy_rattle_excluded")
        listOf(false, true).forEach { night ->
            helper.assertFalse(type in PoppyRattlePool.candidates(helper.level, night), "a candidate (night = $night)")
        }
        helper.succeed()
    }

    // region lane helpers (as in NeedleSpruceGameTests)
    private fun ticking(helper: GameTestHelper): Boolean =
        (0 until STRIP).all { helper.level.isPositionEntityTicking(helper.absolutePos(BlockPos(1 + it, 0, 1))) }

    private fun area(helper: GameTestHelper): AABB =
        AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos(1, 0, 1)), helper.absolutePos(BlockPos(STRIP, 3, 1)))
            .inflate(4.0, 40.0, 4.0)

    private fun clearMobs(helper: GameTestHelper) {
        helper.level.getEntities(null as Entity?, area(helper)) { it !is Player }.forEach { it.discard() }
    }

    /**
     * Grass floor and open sky: the cob flies in a high arc, and the test structure is closed on top (a cob
     * hit that ceiling 6 blocks up), so the air goes up to [SKY].
     */
    private fun prepare(helper: GameTestHelper) {
        clearMobs(helper)
        for (x in 0 until STRIP) {
            helper.setBlock(BlockPos(1 + x, -1, 1), Blocks.GRASS_BLOCK)
            for (y in 0..SKY) helper.setBlock(BlockPos(1 + x, y, 1), Blocks.AIR)
        }
    }

    private const val SKY = 16

    /** A stone wall 4 blocks high across the lane: stops knockback. */
    private fun wall(helper: GameTestHelper, x: Int) {
        for (y in 0..3) helper.setBlock(BlockPos(x, y, 1), Blocks.STONE)
    }

    private fun cobsOf(helper: GameTestHelper, cannon: CornCannon): List<CornCob> =
        helper.level.getEntities(SquadEntities.CORN_COB.get(), area(helper)) { it.entityOwner == cannon }
    // endregion
}
