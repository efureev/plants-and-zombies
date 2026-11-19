package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazComponents
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.api.SeedMutationManager
import joshxviii.plantz.api.SeedMutationResult
import joshxviii.plantz.entity.Sun
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.item.component.SunCost
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

/** GameTests of the Double Sunflower (docs/en/characters/double_sunflower.md). Registered in SquadGameTests. */
object DoubleSunflowerGameTests {
    /** Health, price, size, sleeping at night like the Sunflower. */
    fun stats(helper: GameTestHelper) {
        val type = SquadEntities.DOUBLE_SUNFLOWER.get()
        val plant = type.create(helper.level, EntitySpawnReason.COMMAND)
            ?: return helper.fail("the Double Sunflower does not spawn")
        helper.assertValueEqual(plant.maxHealth, 30f, "max health")
        helper.assertValueEqual(PazConfig.getSunCost(type), 8, "sun cost")
        helper.assertValueEqual(type.width, 0.9f, "width")
        helper.assertValueEqual(type.height, 1.5f, "height")
        helper.assertTrue(plant.sleepsDuringNight(), "awake at night")
        helper.assertFalse(plant.sleepsDuringDay(), "asleep by day")
        helper.succeed()
    }

    /** One sun from each head (2), a baby only 1; no glow while asleep or dying; the sun follows the head's turn. */
    fun twoSuns(helper: GameTestHelper) {
        val type = SquadEntities.DOUBLE_SUNFLOWER.get()
        val adult = helper.spawn(type, BlockPos(1, 1, 1))
        val baby = helper.spawn(type, BlockPos(5, 1, 5)).also { it.isBaby = true }
        helper.assertValueEqual(adult.dropSun(helper.level), 2, "sun from an adult")
        helper.assertValueEqual(baby.dropSun(helper.level), 1, "sun from a baby")
        helper.assertValueEqual(sunAround(helper, adult.position().x, adult.position().z), 2, "sun in the world by the adult")
        helper.assertValueEqual(sunAround(helper, baby.position().x, baby.position().z), 1, "sun in the world by the baby")
        val (tall, short) = adult.headPositions()
        helper.assertTrue(tall.y > short.y && short.y > adult.y + 0.6, "the heads' sun spots: tall $tall, short $short")

        helper.assertValueEqual(adult.lookGlow(0), 1f, "glow right before the sun")
        helper.assertValueEqual(adult.lookGlow(500), 0f, "glow long before the sun")
        adult.deathTime = 1
        helper.assertValueEqual(adult.lookGlow(0), 0f, "glow while dying")
        adult.deathTime = 0
        adult.isAsleep = true
        helper.assertValueEqual(adult.lookGlow(0), 0f, "glow while asleep")

        // the sun comes from the heads as drawn: the model turns them with the head, not the body
        adult.yBodyRot = 0f
        adult.yHeadRot = 90f
        val turned = adult.headPositions()[0].subtract(adult.position())
        val expected = DoubleSunflower.TALL_HEAD.yRot(-90f * net.minecraft.util.Mth.DEG_TO_RAD)
        helper.assertTrue(turned.distanceTo(expected) < 1e-6, "tall head with the head turned 90°: $turned, expected $expected")
        helper.succeed()
    }

    /** Looking diagonally at a greenhouse wall, the short head reaches into the next block: its sun still comes out in the open. */
    fun sunNotInWall(helper: GameTestHelper) {
        val plant = helper.spawn(SquadEntities.DOUBLE_SUNFLOWER.get(), BlockPos(2, 1, 2))
        // pushed flush against the wall (its 0.9-wide box touching it): the short head goes about 0.15 into the wall,
        // too deep for the sun to push itself out (it moves at most an eighth of a block, half its own size)
        plant.setPos(plant.blockPosition().x + 0.45, plant.y, plant.blockPosition().z + 0.5)
        // the turn at which the short head (0.6 blocks out) points straight along -x, past the plant's box
        plant.yBodyRot = 40.9f
        plant.yHeadRot = 40.9f
        val short = plant.headPositions()[1]
        val wall = BlockPos.containing(short)
        helper.assertTrue(wall != plant.blockPosition(), "the short head stays in the plant's own block: $short")
        // two blocks high: a sun starts a little above the head and would hop over a one-block wall
        helper.level.setBlockAndUpdate(wall, Blocks.GLASS.defaultBlockState())
        helper.level.setBlockAndUpdate(wall.above(), Blocks.GLASS.defaultBlockState())
        helper.assertValueEqual(plant.dropSun(helper.level), 2, "sun made")
        val suns = helper.level.getEntitiesOfClass(Sun::class.java, AABB(plant.blockPosition()).inflate(2.0))
        helper.assertValueEqual(suns.sumOf { it.value }, 2, "sun in the world")
        suns.forEach { helper.assertTrue(helper.level.noCollision(it.boundingBox), "a sun inside a block at ${it.position()}") }
        helper.succeed()
    }

    /** Raised half a block (as on a slab) under a ceiling two blocks up: the tall head's sun would start in the ceiling. */
    fun sunUnderLowCeiling(helper: GameTestHelper) {
        val plant = helper.spawn(SquadEntities.DOUBLE_SUNFLOWER.get(), BlockPos(2, 1, 2))
        plant.setPos(plant.x, plant.y + 0.5, plant.z)
        val ceiling = plant.blockPosition().above(2)
        BlockPos.betweenClosed(ceiling.offset(-1, 0, -1), ceiling.offset(1, 0, 1))
            .forEach { helper.level.setBlockAndUpdate(it, Blocks.GLASS.defaultBlockState()) }
        helper.assertTrue(BlockPos.containing(plant.headPositions()[0].add(0.0, 0.2, 0.0)) == ceiling,
            "the tall head's sun would start in the ceiling: ${plant.headPositions()[0]}")
        helper.assertValueEqual(plant.dropSun(helper.level), 2, "sun made")
        val suns = helper.level.getEntitiesOfClass(Sun::class.java, AABB(plant.blockPosition()).inflate(2.0))
        helper.assertValueEqual(suns.sumOf { it.value }, 2, "sun in the world")
        suns.forEach { helper.assertTrue(helper.level.noCollision(it.boundingBox), "a sun inside a block at ${it.position()}") }
        helper.succeed()
    }

    /** Two Sunflower seed packets in the middle column under a crown of 7 sun make a Double Sunflower packet. */
    fun recipe(helper: GameTestHelper) {
        fun craft(packet: EntityType<*>): ItemStack {
            val sun = { ItemStack(PazItems.SUN) }
            // a packet from the creative tab or a shovel carries its price as well
            val seed = { SeedPacketItem.stackFor(packet).also { it.set(PazComponents.SUN_COST, SunCost(5)) } }
            val input = CraftingInput.of(3, 3, listOf(
                sun(), sun(), sun(),
                sun(), seed(), sun(),
                sun(), seed(), sun(),
            ))
            return helper.level.server.recipeManager.getRecipeFor(RecipeType.CRAFTING, input, helper.level)
                .map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
        }
        val made = craft(PazEntities.SUNFLOWER)
        helper.assertTrue(SeedPacketItem.typeFromStack(made) == SquadEntities.DOUBLE_SUNFLOWER.get(), "Sunflower packets made $made")
        helper.assertTrue(craft(PazEntities.PEA_SHOOTER).isEmpty, "Peashooter packets made ${craft(PazEntities.PEA_SHOOTER)}")
        helper.succeed()
    }

    /** Sunflower seeds turn into Double Sunflower seeds one time in ten. */
    fun mutation(helper: GameTestHelper) {
        val rules = SeedMutationManager.get(helper.level, PazEntities.SUNFLOWER)?.mutations
            ?: return helper.fail("no seed mutation rules for the Sunflower")
        val rule = rules.singleOrNull() ?: return helper.fail("expected one rule, got $rules")
        helper.assertValueEqual(rule.chance, 0.1f, "chance")
        val result = (rule.result as? SeedMutationResult.EntityTypeResult)?.entityType?.value()
        helper.assertTrue(result == SquadEntities.DOUBLE_SUNFLOWER.get(), "the mutation gives $result")
        helper.assertTrue(rule.biomes.isEmpty && rule.weather.isEmpty && rule.time.isEmpty, "the rule has conditions: $rule")
        helper.succeed()
    }

    private fun sunAround(helper: GameTestHelper, x: Double, z: Double): Int =
        helper.level.getEntitiesOfClass(Sun::class.java, AABB(x - 1.5, helper.absolutePos(BlockPos.ZERO).y - 1.0, z - 1.5, x + 1.5, helper.absolutePos(BlockPos.ZERO).y + 4.0, z + 1.5))
            .sumOf { it.value }
}
