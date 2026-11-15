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

        /**
         * Puts a stump at [pos] if there is room: air, or a replaceable block without fluid (a snow layer,
         * grass), on a block with a sturdy top. Otherwise drops what breaking the stump would give and
         * returns false.
         */
        fun placeOrDrop(level: ServerLevel, pos: BlockPos): Boolean {
            val stump = SquadBlocks.NEEDLE_SPRUCE_STUMP.get().defaultBlockState()
            val here = level.getBlockState(pos)
            val room = here.isAir || (here.canBeReplaced() && here.fluidState.isEmpty)
            if (room && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                level.setBlockAndUpdate(pos, stump)
                return true
            }
            dropResources(stump, level, pos)
            return false
        }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = SHAPE
}
