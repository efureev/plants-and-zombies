package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.entity.ProjectileRenderState
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition

/**
 * The Corn Cannon's flying cob: the same cone at both ends, no flame or smoke. Built along y with the nose
 * at -y and turned like the original's MissileModel, so ProjectileRenderer points it along the flight.
 * Texture 64×32 from `art/corn_cannon/draw_corn_cannon.py`.
 */
class CornCobModel(root: ModelPart) : EntityModel<ProjectileRenderState>(root) {
    companion object {
        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("corn_cob"), "main")

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            mesh.root.addOrReplaceChild("cob", CubeListBuilder.create()
                .texOffs(0, 20).addBox(-2f, -9f, -2f, 4f, 1f, 4f)      // nose end
                .texOffs(32, 0).addBox(-3f, -8f, -3f, 6f, 2f, 6f)      // nose cone
                .texOffs(0, 0).addBox(-4f, -6f, -4f, 8f, 12f, 8f)
                .texOffs(32, 0).addBox(-3f, 6f, -3f, 6f, 2f, 6f)       // back cone
                .texOffs(0, 20).addBox(-2f, 8f, -2f, 4f, 1f, 4f),      // back end
                PartPose.offsetAndRotation(0f, 24f, 0f, 0f, -1.5708f, 1.5708f))
            return LayerDefinition.create(mesh, 64, 32)
        }
    }
}
