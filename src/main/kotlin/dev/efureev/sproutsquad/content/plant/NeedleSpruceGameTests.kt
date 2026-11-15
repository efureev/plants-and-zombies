package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.entity.projectile.Needle
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

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
     * 850 ms of a volley, and the plain sleep texture while dying (there is no `_angry_sleep`).
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
