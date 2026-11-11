package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.entity.plant.Citron
import joshxviii.plantz.entity.plant.ExplosivePlant
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.zombie.DiggerZombie
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FarmlandBlock
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.EventHooks

/**
 * GameTests for bugs of the original fixed in the fork (docs/en/UPSTREAM.md).
 * Registered in [dev.efureev.sproutsquad.debug.SquadGameTests].
 *
 * The 1x1x1 "empty" test structure sits in barriers; tests that need entities to tick use open
 * ground 5 blocks beside it, inside the structure's own chunk.
 */
object OriginalGapsGameTests {
    private fun spot(helper: GameTestHelper): BlockPos {
        val origin = helper.absolutePos(BlockPos.ZERO)
        val dx = if (Math.floorMod(origin.x, 16) <= 12) 0 else -3
        val dz = if (Math.floorMod(origin.z, 16) <= 9) 5 else -5
        return BlockPos(dx, 0, dz)
    }

    private fun clear(helper: GameTestHelper, pos: BlockPos) {
        helper.level.getEntities(null as Entity?, AABB(helper.absolutePos(pos)).inflate(4.0)) { it !is Player }.forEach { it.discard() }
    }

    /** The seed timer counts down once per tick, as configured (it used to run several times per tick). */
    fun seedTimer(helper: GameTestHelper) {
        val pos = spot(helper)
        clear(helper, pos)
        // wet farmland: dry farmland may turn to dirt on its first random tick, and the timer stops on dirt
        helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE))
        helper.setBlock(pos, Blocks.AIR)
        helper.setBlock(pos.above(), Blocks.AIR)
        lateinit var plant: Plant
        var start = 0
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                plant = helper.spawn(PazEntities.SUNFLOWER, pos)
                plant.setTame(true, true)
                plant.receivedWater = 1
                plant.seedGrowCooldown = 1000
                start = plant.seedGrowCooldown
            }
            .thenIdle(40)
            .thenExecute {
                val passed = start - plant.seedGrowCooldown
                helper.assertTrue(passed in 35..45, "the seed timer ran $passed ticks in 40 game ticks")
                clear(helper, pos)
            }
            .thenSucceed()
    }

    /** Browncoats can spawn as babies again; zombies without baby models never do. */
    fun babyZombies(helper: GameTestHelper) {
        val level = helper.level
        val pos = Vec3.atBottomCenterOf(helper.absolutePos(BlockPos(0, 1, 0)))
        val difficulty = level.getCurrentDifficultyAt(BlockPos.containing(pos))
        fun babies(type: net.minecraft.world.entity.EntityType<out net.minecraft.world.entity.monster.zombie.Zombie>): Int = (1..200).count {
            val zombie = type.create(level, EntitySpawnReason.NATURAL)!!
            zombie.snapTo(pos, 0f, 0f)
            EventHooks.finalizeMobSpawn(zombie, level, difficulty, EntitySpawnReason.NATURAL, null)
            val baby = zombie.isBaby
            zombie.passengers.forEach { it.discard() }
            zombie.vehicle?.discard()
            zombie.discard()
            baby
        }
        val browncoats = babies(PazEntities.BROWN_COAT)
        val newspaper = babies(PazEntities.NEWSPAPER_ZOMBIE)
        // leaders bring followers into the level: remove them, all tests run at the same spot
        level.getEntities(null as Entity?, AABB(BlockPos.containing(pos)).inflate(32.0)) { it !is Player }.forEach { it.discard() }
        helper.assertTrue(browncoats in 1..40, "$browncoats of 200 Browncoats spawned as babies, expected a few")
        helper.assertTrue(newspaper == 0, "$newspaper Newspaper Zombies spawned as babies")
        helper.succeed()
    }

    /** Flint and steel lights an idle explosive plant (it required an action in progress). */
    fun flintAndSteel(helper: GameTestHelper) {
        val bomb = PazEntities.CHERRY_BOMB.create(helper.level, EntitySpawnReason.COMMAND)!! as ExplosivePlant
        bomb.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(BlockPos(0, 1, 0))), 0f, 0f)
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.FLINT_AND_STEEL))
        bomb.cooldown = 0 // a planted bomb whose start-up cooldown has run out
        helper.assertTrue(!bomb.isAsleep, "the test bomb is asleep")
        bomb.mobInteract(player, InteractionHand.MAIN_HAND)
        helper.assertTrue(bomb.swellDir == 2, "flint and steel did not light the Cherry Bomb (swellDir ${bomb.swellDir})")
        bomb.discard()
        helper.succeed()
    }

    /** With plantCooldownEnabled, planting from a seed packet puts that plant type on cooldown. */
    fun plantCooldown(helper: GameTestHelper) {
        val pos = spot(helper)
        clear(helper, pos)
        for (dx in 0..1) {
            helper.setBlock(pos.below().east(dx), Blocks.GRASS_BLOCK)
            helper.setBlock(pos.east(dx), Blocks.AIR)
            helper.setBlock(pos.above().east(dx), Blocks.AIR)
        }
        val config = PazConfig.server
        val wasEnabled = config.plantCooldownEnabled
        try {
            // option off (the default): planting leaves the rest of the stack untouched, so it still stacks
            config.plantCooldownEnabled = false
            val first = helper.makeMockPlayer(GameType.SURVIVAL)
            first.inventory.add(ItemStack(PazItems.SUN, 32))
            val stack = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER).copyWithCount(2)
            SeedPacketItem.tryPlant(helper.level, first, stack, helper.absolutePos(pos.below()), Direction.UP, Direction.NORTH)
            helper.assertTrue(stack.count == 1 && ItemStack.isSameItemSameComponents(stack, SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER)),
                "planting changed the remaining packets: ${stack.componentsPatch}")
            clear(helper, pos)

            config.plantCooldownEnabled = true
            val player = helper.makeMockPlayer(GameType.SURVIVAL)
            player.inventory.add(ItemStack(PazItems.SUN, 32))
            val packet = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER) // a single packet: the stack is used up
            SeedPacketItem.tryPlant(helper.level, player, packet, helper.absolutePos(pos.below()), Direction.UP, Direction.NORTH)
            // another, fresh stack of the same plant must wait too
            val another = SeedPacketItem.stackFor(PazEntities.PEA_SHOOTER)
            val second = SeedPacketItem.tryPlant(helper.level, player, another, helper.absolutePos(pos.below().east()), Direction.UP, Direction.NORTH)
            helper.assertTrue(second == InteractionResult.FAIL, "a second Peashooter was planted during the cooldown ($second)")
        } finally {
            config.plantCooldownEnabled = wasEnabled
            clear(helper, pos)
        }
        helper.succeed()
    }

    /** The Digger digs through a dirt wall in a closed obsidian corridor between it and its target. */
    fun diggerMines(helper: GameTestHelper) {
        val pos = spot(helper)
        clear(helper, pos)
        // corridor x 0..4: obsidian floor, side walls and roof, so going around the wall is impossible
        for (x in -1..5) for (y in -1..2) for (z in -1..1) {
            val inside = x in 0..4 && y in 0..1 && z == 0
            helper.setBlock(pos.offset(x, y, z), if (inside) Blocks.AIR else Blocks.OBSIDIAN)
        }
        val wall = pos.offset(2, 0, 0)
        lateinit var digger: DiggerZombie
        lateinit var plant: Plant
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                helper.setBlock(wall, Blocks.DIRT)
                helper.setBlock(wall.above(), Blocks.DIRT)
                plant = helper.spawn(PazEntities.SUNFLOWER, pos.offset(4, 0, 0))
                digger = helper.spawn(PazEntities.DIGGER_ZOMBIE, pos)
                digger.tunnelCooldown = 100_000 // no burrowing past the wall
            }
            .thenWaitUntil {
                if (digger.target == null) digger.target = plant
                helper.assertTrue(helper.getBlockState(wall).isAir || helper.getBlockState(wall.above()).isAir, "the Digger has not dug through the wall")
            }
            .thenExecute {
                clear(helper, pos)
                for (x in -1..5) for (y in -1..2) for (z in -1..1) helper.setBlock(pos.offset(x, y, z), Blocks.AIR)
            }
            .thenSucceed()
    }

    /** A wall the Digger can walk around stays whole: it only digs when the target is unreachable. */
    fun diggerWalksAround(helper: GameTestHelper) {
        val pos = spot(helper)
        clear(helper, pos)
        // open floor with a 1-wide, 2-high dirt wall in the middle row
        for (x in -1..5) for (z in -1..1) {
            helper.setBlock(pos.offset(x, -1, z), Blocks.STONE)
            helper.setBlock(pos.offset(x, 0, z), Blocks.AIR)
            helper.setBlock(pos.offset(x, 1, z), Blocks.AIR)
        }
        val wall = pos.offset(2, 0, 0)
        lateinit var digger: DiggerZombie
        lateinit var plant: Plant
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(helper.level.isPositionEntityTicking(helper.absolutePos(pos)), "spot is not ticking yet") }
            .thenExecute {
                helper.setBlock(wall, Blocks.DIRT)
                helper.setBlock(wall.above(), Blocks.DIRT)
                plant = helper.spawn(PazEntities.SUNFLOWER, pos.offset(4, 0, 0))
                digger = helper.spawn(PazEntities.DIGGER_ZOMBIE, pos)
                digger.tunnelCooldown = 100_000
            }
            .thenIdle(160)
            .thenExecute {
                helper.assertTrue(!helper.getBlockState(wall).isAir && !helper.getBlockState(wall.above()).isAir, "the Digger dug a wall it could walk around")
                clear(helper, pos)
            }
            .thenSucceed()
    }

    /** A dye recolours the player's own Marigold and is used up; a wild one stays as it is. */
    fun marigoldDye(helper: GameTestHelper) {
        val marigold = helper.spawn(PazEntities.MARIGOLD, BlockPos(0, 1, 0))
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.DYE.red(), 2))
        // a wild Marigold is not dyed (same ownership rules as the other plant interactions)
        player.interactOn(marigold, InteractionHand.MAIN_HAND, marigold.position())
        helper.assertTrue(MarigoldColors.color(marigold) == null, "a wild Marigold was dyed")
        marigold.tame(player)
        player.interactOn(marigold, InteractionHand.MAIN_HAND, marigold.position())
        helper.assertTrue(MarigoldColors.color(marigold) == DyeColor.RED, "the Marigold is ${MarigoldColors.color(marigold)}, not red")
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).count == 1, "the dye was not used up")
        marigold.discard()
        helper.succeed()
    }

    /** A Citron pushed with a gardening glove switches to its ball form. */
    fun citronBall(helper: GameTestHelper) {
        val citron = helper.spawn(PazEntities.CITRON, BlockPos(0, 1, 0))
        val player = helper.makeMockPlayer(GameType.SURVIVAL)
        citron.attackedWithGlove(player, ItemStack(PazItems.GARDENING_GLOVE), InteractionHand.MAIN_HAND)
        val speed = citron.getAttribute(Attributes.MOVEMENT_SPEED)!!
        helper.assertTrue(speed.hasModifier(Citron.TRANSFORMED_ID), "the pushed Citron did not switch to the ball form")
        citron.discard()
        helper.succeed()
    }
}
