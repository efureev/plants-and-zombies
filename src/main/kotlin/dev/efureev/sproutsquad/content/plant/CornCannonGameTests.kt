package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.projectile.CornCob
import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.SunCost
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.Entity
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
        helper.succeed()
    }

    /** The face squints while aiming and firing, and is plain again while dying (the renderer adds `sleep`). */
    fun face(helper: GameTestHelper) {
        val cannon = SquadEntities.CORN_CANNON.get().create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Corn Cannon does not spawn")
        helper.assertValueEqual(cannon.faceSuffix(null), "", "no action")
        helper.assertValueEqual(cannon.faceSuffix(100L), "aim", "100 ms into a shot")
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
