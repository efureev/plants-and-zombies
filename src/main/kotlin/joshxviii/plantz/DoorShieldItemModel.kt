package joshxviii.plantz

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.item.ItemModel
import net.minecraft.client.renderer.item.ItemModelResolver
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.item.ModelRenderProperties
import net.minecraft.client.resources.model.ResolvableModel
import net.minecraft.client.resources.model.geometry.BakedQuad
import net.minecraft.client.resources.model.geometry.ItemQuads
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.ItemOwner
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f

private val DIRECTIONS_AND_NULL: List<Direction?> = Direction.entries + listOf(null)

class DoorShieldItemModel(
    private val displayProperties: ModelRenderProperties
) : ItemModel {
    override fun update(
        output: ItemStackRenderState,
        item: ItemStack,
        resolver: ItemModelResolver,
        displayContext: ItemDisplayContext,
        level: ClientLevel?,
        owner: ItemOwner?,
        seed: Int
    ) {
        val door = item.get(PazComponents.DOOR_SHIELD)?.doorType?.takeIf { it is DoorBlock } ?: return
        output.appendModelIdentityElement(door)
        val layer = output.newLayer()
        layer.setLocalTransform(Matrix4f().translate(0f, 0f, -8/16f))
        displayProperties.applyToLayer(layer, displayContext)
        if (item.hasFoil()) {
            val foilType = ItemStackRenderState.FoilType.STANDARD
            layer.setFoilType(foilType)
            output.setAnimated()
            output.appendModelIdentityElement(foilType)
        }
        val lowerState = door.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
        val upperState = lowerState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
        val blockStateModels = Minecraft.getInstance().modelManager.blockStateModelSet
        layer.setParticleMaterial(blockStateModels.get(lowerState).particleMaterial())
        val quads = ArrayList<BakedQuad>()
        collectBlockQuads(quads, lowerState, 0f, seed.toLong())
        collectBlockQuads(quads, upperState, 1f, seed.toLong() + 1L)
        layer.setQuads(ItemQuads.split(quads))
    }

    /** Collects all quads of [state]'s block model, shifted up by [yOffset] blocks. */
    private fun collectBlockQuads(
        output: MutableList<BakedQuad>,
        state: BlockState,
        yOffset: Float,
        seed: Long
    ) {
        val model: BlockStateModel = Minecraft.getInstance().modelManager.blockStateModelSet.get(state)
        val parts = ArrayList<BlockStateModelPart>()
        model.collectParts(RandomSource.create(seed), parts)
        for (part in parts) {
            for (direction in DIRECTIONS_AND_NULL) {
                for (quad in part.getQuads(direction)) {
                    output += if (yOffset == 0f) quad else quad.shiftedUp(yOffset)
                }
            }
        }
    }

    private fun BakedQuad.shiftedUp(dy: Float): BakedQuad = BakedQuad(
        Vector3f(position0()).add(0f, dy, 0f),
        Vector3f(position1()).add(0f, dy, 0f),
        Vector3f(position2()).add(0f, dy, 0f),
        Vector3f(position3()).add(0f, dy, 0f),
        packedUV0(), packedUV1(), packedUV2(), packedUV3(),
        direction(),
        materialInfo(),
        bakedNormals(),
        bakedColors(),
    )

    class Unbaked : ItemModel.Unbaked {
        private val model: Identifier

        private constructor(model: Identifier) {
            this.model = model
        }

        companion object {
            val MAP_CODEC: MapCodec<Unbaked> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model)
                ).apply(instance, ::Unbaked)
            }
        }

        override fun type(): MapCodec<Unbaked> = MAP_CODEC

        override fun bake(context: ItemModel.BakingContext, transformation: Matrix4fc): ItemModel {
            val resolvedModel = context.blockModelBaker.getModel(model)
            val properties = ModelRenderProperties.fromResolvedModel(
                context.blockModelBaker,
                resolvedModel,
                resolvedModel.topTextureSlots
            )
            return DoorShieldItemModel(properties)
        }

        override fun resolveDependencies(resolver: ResolvableModel.Resolver) {
            resolver.markDependency(model)
        }
    }
}
