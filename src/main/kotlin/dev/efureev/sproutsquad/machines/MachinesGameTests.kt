package dev.efureev.sproutsquad.machines

import dev.efureev.sproutsquad.registry.SquadAttachments
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.entity.blueprint_machines.LawnMower
import joshxviii.plantz.entity.zombie.ZombieRobot
import joshxviii.plantz.item.BlueprintItem
import joshxviii.plantz.raid.ignoresFlag
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * GameTests for player-built machines ([PlayerMachines], [LawnMowerDrive]).
 * Registered in [dev.efureev.sproutsquad.debug.SquadGameTests].
 *
 * The 1x1x1 "empty" test structure sits in barriers, so every test uses a strip of open ground
 * 5 blocks beside it, kept inside the structure's own chunk: only that chunk ticks entities.
 */
object MachinesGameTests {
    private const val STRIP = 7

    /** Relative x/z of the start of a [STRIP]-long strip along +x inside the test's chunk. */
    private fun strip(helper: GameTestHelper): Pair<Int, Int> {
        val origin = helper.absolutePos(BlockPos.ZERO)
        val dx = if (Math.floorMod(origin.x, 16) <= 15 - STRIP) 0 else -STRIP
        val dz = if (Math.floorMod(origin.z, 16) <= 9) 5 else -5
        return dx to dz
    }

    /**
     * Removes every mob around the strip. The runner only cleans the test structure itself, and all
     * tests stand side by side: a player's turret left behind would shoot the next test's zombie.
     */
    private fun clearMobs(helper: GameTestHelper, dx: Int, dz: Int) {
        val area = AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos(dx, 0, dz)), helper.absolutePos(BlockPos(dx + STRIP - 1, 1, dz))).inflate(3.0)
        helper.level.getEntities(null as Entity?, area) { it !is Player }.forEach { it.discard() }
    }

    /** Clears the strip: mobs left over from other tests, then the blocks. */
    private fun prepare(helper: GameTestHelper, dx: Int, dz: Int) {
        clearMobs(helper, dx, dz)
        for (x in 0 until STRIP) {
            helper.setBlock(BlockPos(dx + x, -1, dz), Blocks.STONE)
            helper.setBlock(BlockPos(dx + x, 0, dz), Blocks.AIR)
            helper.setBlock(BlockPos(dx + x, 1, dz), Blocks.AIR)
        }
    }

    private fun ticking(helper: GameTestHelper, dx: Int, dz: Int): Boolean =
        (0 until STRIP).all { helper.level.isPositionEntityTicking(helper.absolutePos(BlockPos(dx + it, 0, dz))) }

    /** Entities of [type] around a relative position (GameTestHelper.getEntities only looks inside the tiny structure). */
    private fun <T : Entity> near(helper: GameTestHelper, type: EntityType<T>, pos: BlockPos): List<T> =
        helper.level.getEntities(type, AABB(helper.absolutePos(pos)).inflate(1.5)) { true }

    /** Builds [type] from a blueprint at a relative position on the strip, as a (mock) survival player. */
    private fun <T : Entity> buildWithBlueprint(helper: GameTestHelper, player: Player, type: EntityType<T>, pos: BlockPos): T? {
        player.inventory.add(ItemStack(PazItems.BRAINZ_ALLOY, PazConfig.getAlloyCost(type)))
        val blueprint = BlueprintItem.stackFor(type)
        (blueprint.item as BlueprintItem).tryBuild(helper.level, player, blueprint, helper.absolutePos(pos.below()), Direction.UP, Direction.EAST, InteractionHand.MAIN_HAND)
        return near(helper, type, pos).singleOrNull()
    }

    /**
     * Building from a blueprint marks the machine as the player's and spends the alloy. No machine
     * can come as a reinforcement (MachineReinforcements): on Hard a hit machine would call a copy of
     * itself, in the air or underground, and a hostile one for a player's machine.
     */
    fun build(helper: GameTestHelper) {
        val (dx, dz) = strip(helper)
        prepare(helper, dx, dz)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute { checkNoReinforcements(helper, BlockPos(dx, 0, dz)) }
            .thenExecute { checkBuild(helper, dx, dz) }
            .thenExecute { clearMobs(helper, dx, dz) }
            .thenSucceed()
    }

    /** Every machine type of the mod: its spawn rule refuses a reinforcement, whatever the chance to call one. */
    private fun checkNoReinforcements(helper: GameTestHelper, pos: BlockPos) {
        val level = helper.level
        val machines = BuiltInRegistries.ENTITY_TYPE.filter {
            BuiltInRegistries.ENTITY_TYPE.getKey(it).namespace == PazMain.MODID && it.create(level, EntitySpawnReason.COMMAND) is ZombieRobot
        }
        helper.assertTrue(machines.size >= 3, "found only ${machines.size} machine types")
        for (type in machines) {
            @Suppress("UNCHECKED_CAST")
            val reinforcement = SpawnPlacements.checkSpawnRules(type as EntityType<Entity>, level, EntitySpawnReason.REINFORCEMENT, helper.absolutePos(pos), level.random)
            helper.assertTrue(!reinforcement, "${type.toShortString()} may come as a reinforcement")
        }
    }

    private fun checkBuild(helper: GameTestHelper, dx: Int, dz: Int) {
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.inventory.add(ItemStack(PazItems.BRAINZ_ALLOY, 3)) // left over after both builds

        val turret = buildWithBlueprint(helper, player, PazEntities.ZOMBIE_TURRET, BlockPos(dx, 0, dz))
        val mower = buildWithBlueprint(helper, player, PazEntities.LAWN_MOWER, BlockPos(dx + 3, 0, dz))
        helper.assertTrue(turret != null, "no turret was built")
        turret!!
        helper.assertTrue(turret.getData(SquadAttachments.PLAYER_MACHINE), "turret is not marked as a player machine")
        helper.assertTrue(turret.owner === player, "turret owner is not the builder")
        helper.assertTrue(turret.ignoresFlag(), "turret would go for the raid flag")
        helper.assertTrue(player.inventory.countItem(PazItems.BRAINZ_ALLOY) == 3, "alloy not spent: ${player.inventory.countItem(PazItems.BRAINZ_ALLOY)} left")

        helper.assertTrue(mower != null, "no lawn mower was built")
        val ahead = Vec3.directionFromRotation(0f, mower!!.yRot)
        helper.assertTrue(ahead.x > 0.9, "the mower should face where the player looks (east), faces $ahead")
    }

    /** A player's turret and a plant ignore each other. */
    fun allies(helper: GameTestHelper) {
        val (dx, dz) = strip(helper)
        prepare(helper, dx, dz)
        lateinit var turret: ZombieRobot
        lateinit var plant: Mob
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute {
                turret = buildWithBlueprint(helper, helper.makeMockPlayer(GameType.SURVIVAL), PazEntities.ZOMBIE_TURRET, BlockPos(dx, 0, dz))!!
                plant = helper.spawn(PazEntities.PEA_SHOOTER, BlockPos(dx + 3, 0, dz))
            }
            .thenIdle(80)
            .thenExecute {
                helper.assertTrue(turret.target == null, "the turret targets ${turret.target?.type?.toShortString()}")
                helper.assertTrue(plant.target == null, "the peashooter targets ${plant.target?.type?.toShortString()}")
                helper.assertTrue(turret.health == turret.maxHealth && plant.health == plant.maxHealth, "someone got hurt")
            }
            .thenExecute { clearMobs(helper, dx, dz) }
            .thenSucceed()
    }

    /**
     * A player's turret shoots a mod zombie dead. Its shots must not count as zombie friendly fire, even
     * though the owner (a mock player, like a player who logged out) cannot be found in the level.
     */
    fun turret(helper: GameTestHelper) = killsZombie(helper, PazEntities.ZOMBIE_TURRET, distance = 4)

    /** The electro turret zaps a zombie next to it. */
    fun electroTurret(helper: GameTestHelper) = killsZombie(helper, PazEntities.ELECTRO_TURRET, distance = 1)

    private fun <T : ZombieRobot> killsZombie(helper: GameTestHelper, type: EntityType<T>, distance: Int) {
        val (dx, dz) = strip(helper)
        prepare(helper, dx, dz)
        lateinit var machine: ZombieRobot
        lateinit var zombie: LivingEntity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute {
                machine = buildWithBlueprint(helper, helper.makeMockPlayer(GameType.SURVIVAL), type, BlockPos(dx, 0, dz))!!
                // a wall behind the zombie: knockback would slide it off the strip into a chunk that does not tick
                helper.setBlock(BlockPos(dx + distance + 1, 0, dz), Blocks.STONE)
                helper.setBlock(BlockPos(dx + distance + 1, 1, dz), Blocks.STONE)
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(dx + distance, 0, dz))
            }
            .thenWaitUntil {
                helper.assertTrue(zombie.isDeadOrDying || zombie.isRemoved,
                    "the zombie is still alive (${zombie.health} hp), ${machine.type.toShortString()} target: ${machine.target?.type?.toShortString()}")
            }
            .thenExecute { clearMobs(helper, dx, dz) }
            .thenSucceed()
    }

    /** The mower waits for a zombie, drives over it, spares the plant in its way and stops at the wall. */
    fun mower(helper: GameTestHelper) {
        val (dx, dz) = strip(helper)
        prepare(helper, dx, dz)
        lateinit var mower: LawnMower
        lateinit var zombie: LivingEntity
        lateinit var plant: LivingEntity
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(ticking(helper, dx, dz), "strip is not ticking yet") }
            .thenExecute {
                helper.setBlock(BlockPos(dx + 5, 0, dz), Blocks.STONE)
                helper.setBlock(BlockPos(dx + 5, 1, dz), Blocks.STONE)
                plant = helper.spawn(PazEntities.SUNFLOWER, BlockPos(dx + 3, 0, dz))
                mower = helper.spawn(PazEntities.LAWN_MOWER, BlockPos(dx, 0, dz))
                mower.yRot = -90f // drive towards +x
                mower.yBodyRot = -90f
                mower.yHeadRot = -90f
                zombie = helper.spawnWithNoFreeWill(PazEntities.BROWN_COAT, BlockPos(dx + 1, 0, dz))
            }
            .thenWaitUntil {
                helper.assertTrue(zombie.isDeadOrDying || zombie.isRemoved, "the zombie survived the mower")
                helper.assertTrue(mower.isRemoved, "the mower is still driving")
            }
            .thenExecute { helper.assertTrue(plant.isAlive && plant.health == plant.maxHealth, "the mower hurt the plant") }
            .thenExecute { clearMobs(helper, dx, dz) }
            .thenSucceed()
    }
}
