package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.pazResource
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition

/**
 * Needle Spruce: a trunk that bends in two places and a three-tier crown with eyes on the front of
 * the bottom tier and four sharp fangs hanging under its front edge. Three blocks tall.
 *
 * Parts: body → trunk_lower → trunk_upper → crown → tier1 (face on the north side), tier2, tier3, top,
 * fang_0..fang_3. Texture 128×64, drawn by `art/needle_spruce/draw_needle_spruce.py` (the UV origins
 * there must match the texOffs here).
 */
class NeedleSpruceModel(root: ModelPart) : SquadPlantModel(root) {
    companion object {
        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("needle_spruce"), "main")

        /** Fang x positions under the front edge; the middle pair, under the eyes, is longer. */
        private val FANGS = listOf(-8f to false, -4f to true, 4f to true, 8f to false)

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val body = mesh.root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f))

            val trunkLower = body.addOrReplaceChild("trunk_lower", CubeListBuilder.create()
                .texOffs(80, 0).addBox(-3f, -9f, -3f, 6f, 9f, 6f),
                PartPose.ZERO)
            val trunkUpper = trunkLower.addOrReplaceChild("trunk_upper", CubeListBuilder.create()
                .texOffs(104, 0).addBox(-3f, -7f, -3f, 6f, 7f, 6f),
                PartPose.offset(0f, -9f, 0f))
            val crown = trunkUpper.addOrReplaceChild("crown", CubeListBuilder.create(), PartPose.offset(0f, -7f, 0f))

            crown.addOrReplaceChild("tier1", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10f, -12f, -10f, 20f, 12f, 20f), PartPose.ZERO)
            crown.addOrReplaceChild("tier2", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-8f, -21f, -8f, 16f, 9f, 16f), PartPose.ZERO)
            crown.addOrReplaceChild("tier3", CubeListBuilder.create()
                .texOffs(64, 32).addBox(-5.5f, -28f, -5.5f, 11f, 7f, 11f), PartPose.ZERO)
            crown.addOrReplaceChild("top", CubeListBuilder.create()
                .texOffs(80, 16).addBox(-2f, -32f, -2f, 4f, 4f, 4f), PartPose.ZERO)

            FANGS.forEachIndexed { i, (x, long) -> fang(crown, "fang_$i", x, long) }

            return LayerDefinition.create(mesh, 128, 64)
        }

        /** Three boxes narrowing down to a one-pixel tip, 3 pixels in from the front edge of tier1. */
        private fun fang(crown: PartDefinition, name: String, x: Float, long: Boolean) {
            val cubes = CubeListBuilder.create()
            if (long) cubes
                .texOffs(96, 16).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f)
                .texOffs(108, 16).addBox(-1f, 3f, -1f, 2f, 2f, 2f)
                .texOffs(116, 16).addBox(-0.5f, 5f, -0.5f, 1f, 3f, 1f)
            else cubes
                .texOffs(96, 24).addBox(-1.5f, 0f, -1.5f, 3f, 2f, 3f)
                .texOffs(108, 24).addBox(-1f, 2f, -1f, 2f, 2f, 2f)
                .texOffs(116, 24).addBox(-0.5f, 4f, -0.5f, 1f, 2f, 1f)
            crown.addOrReplaceChild(name, cubes, PartPose.offset(x, 0f, -7f))
        }
    }
}
