package dev.efureev.sproutsquad.content.block

import dev.efureev.sproutsquad.registry.SquadBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * The stump a Needle Spruce leaves where it stood: the lower trunk with roots in four directions,
 * rings on top and a small green shoot. Breaking it gives a spruce sapling and a stick or two
 * (`loot_table/blocks/needle_spruce_stump.json`); the sapling goes into the Needle Spruce seed packet recipe.
 */
class NeedleSpruceStumpBlock(properties: Properties) : Block(properties) {
    companion object {
        private val SHAPE: VoxelShape = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 9.0, 11.0),   // trunk
            box(7.0, 0.0, 2.0, 9.0, 2.0, 5.0),     // roots: north, south, west, east
            box(7.0, 0.0, 11.0, 9.0, 2.0, 14.0),
            box(2.0, 0.0, 7.0, 5.0, 2.0, 9.0),
            box(11.0, 0.0, 7.0, 14.0, 2.0, 9.0),
        )

        /** Ground at least this high (of a block) carries a stump: farmland, dirt paths, mud; not a slab. */
        private const val MIN_GROUND_HEIGHT = 14.0 / 16.0

        /**
         * Puts a stump where a spruce standing at [pos] (its block position) had its trunk: in [pos] when that
         * is air or a replaceable block without fluid (a snow layer, grass) on ground below, or on top of
         * [pos] when the spruce stands inside a ground block a little lower than a full one (farmland, a
         * dirt path, mud). Never replaces anything else. Otherwise drops what breaking the stump would give
         * and returns false.
         */
        fun placeOrDrop(level: ServerLevel, pos: BlockPos): Boolean {
            val stump = SquadBlocks.NEEDLE_SPRUCE_STUMP.get().defaultBlockState()
            val target = if (hasRoom(level, pos)) pos else pos.above()
            if (hasRoom(level, target) && isGround(level, target.below())) {
                level.setBlockAndUpdate(target, stump)
                return true
            }
            dropResources(stump, level, pos)
            return false
        }

        private fun hasRoom(level: ServerLevel, pos: BlockPos): Boolean {
            val state = level.getBlockState(pos)
            return state.isAir || (state.canBeReplaced() && state.fluidState.isEmpty)
        }

        private fun isGround(level: ServerLevel, pos: BlockPos): Boolean {
            val state = level.getBlockState(pos)
            return state.isFaceSturdy(level, pos, Direction.UP)
                || state.getCollisionShape(level, pos).max(Direction.Axis.Y) >= MIN_GROUND_HEIGHT
        }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = SHAPE
}
