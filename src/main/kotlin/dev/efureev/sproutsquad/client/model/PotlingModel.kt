package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.pazResource
import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationChannel.Interpolations.CATMULLROM
import net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR
import net.minecraft.client.animation.AnimationChannel.Targets.ROTATION
import net.minecraft.client.animation.AnimationChannel.Targets.SCALE
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations.degreeVec
import net.minecraft.client.animation.KeyframeAnimations.scaleVec
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition

/**
 * Potling: a terracotta pot with a face and a two-leaf sprout.
 *
 * Parts: body → pot (base, walls, rim) → stem → leaf_left, leaf_right.
 * Texture 64×64, drawn by `art/potling/draw_potling.py`.
 */
class PotlingModel(root: ModelPart) : SquadPlantModel(
    root,
    init = PotlingAnimation.init,
    idle = PotlingAnimation.idle,
    action = PotlingAnimation.action,
    sleep = PotlingAnimation.sleep,
) {
    companion object {
        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("potling"), "main")

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val body = mesh.root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f))

            val pot = body.addOrReplaceChild("pot", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -8f, -4f, 8f, 6f, 8f)       // walls with the face
                .texOffs(0, 14).addBox(-5f, -10f, -5f, 10f, 2f, 10f)   // rim, soil on top
                .texOffs(32, 0).addBox(-3f, -2f, -3f, 6f, 2f, 6f),     // narrow base
                PartPose.ZERO)

            val stem = pot.addOrReplaceChild("stem", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-0.5f, -5f, -0.5f, 1f, 5f, 1f),
                PartPose.offset(0f, -10f, 0f))

            // Leaves are flat planes; the smaller one points left, the bigger one waves up to the right.
            stem.addOrReplaceChild("leaf_left", CubeListBuilder.create()
                .texOffs(40, 8).addBox(-5f, 0f, -1.5f, 5f, 0f, 3f),
                PartPose.offsetAndRotation(-0.5f, -4f, 0f, 0f, 0f, 0.45f))
            stem.addOrReplaceChild("leaf_right", CubeListBuilder.create()
                .texOffs(40, 12).addBox(0f, 0f, -2f, 6f, 0f, 4f),
                PartPose.offsetAndRotation(0.5f, -5f, 0f, 0f, 0f, -0.6f))

            return LayerDefinition.create(mesh, 64, 64)
        }
    }
}

/** Keyframes for [PotlingModel]: rotations in degrees, scales as multipliers. */
object PotlingAnimation {
    private fun rot(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), CATMULLROM)
    private fun scale(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, scaleVec(x.toDouble(), y.toDouble(), z.toDouble()), CATMULLROM)
    private fun scale(t: Float, s: Float) = scale(t, s, s, s)

    /** Pops out of the ground: pot squashes in, the sprout shoots up, leaves unfold last. */
    val init: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f)
        .addAnimation("pot", AnimationChannel(SCALE,
            scale(0f, 0.2f), scale(0.3f, 1.2f, 0.8f, 1.2f), scale(0.5f, 0.9f, 1.15f, 0.9f),
            scale(0.7f, 1.05f, 0.95f, 1.05f), scale(1f, 1f)))
        .addAnimation("stem", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(1.0, 0.0, 1.0), LINEAR), Keyframe(0.4f, scaleVec(1.0, 0.0, 1.0), LINEAR),
            scale(0.6f, 1f, 1.3f, 1f), scale(0.8f, 1f, 0.9f, 1f), scale(1f, 1f)))
        .addAnimation("leaf_left", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(0.0, 0.0, 0.0), LINEAR), Keyframe(0.55f, scaleVec(0.0, 0.0, 0.0), LINEAR),
            scale(0.75f, 1.2f), scale(1f, 1f)))
        .addAnimation("leaf_right", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(0.0, 0.0, 0.0), LINEAR), Keyframe(0.6f, scaleVec(0.0, 0.0, 0.0), LINEAR),
            scale(0.8f, 1.2f), scale(1f, 1f)))
        .build()

    /** Sprout sways, leaves flap a little, the pot "breathes". */
    val idle: AnimationDefinition = AnimationDefinition.Builder.withLength(3.0f).looping()
        .addAnimation("stem", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.75f, 0f, 0f, 6f), rot(1.5f, 0f, 0f, 0f), rot(2.25f, 0f, 0f, -6f), rot(3f, 0f, 0f, 0f)))
        .addAnimation("leaf_left", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.75f, 0f, 0f, 10f), rot(1.5f, 0f, 0f, 0f), rot(2.25f, 0f, 0f, 6f), rot(3f, 0f, 0f, 0f)))
        .addAnimation("leaf_right", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.75f, 0f, 0f, -6f), rot(1.5f, 0f, 0f, 0f), rot(2.25f, 0f, 0f, -12f), rot(3f, 0f, 0f, 0f)))
        .addAnimation("pot", AnimationChannel(SCALE,
            scale(0f, 1f), scale(1.5f, 1.03f, 0.97f, 1.03f), scale(3f, 1f)))
        .build()

    /** Making a sun: squash and stretch, the sprout whips back and forth, leaves clap. */
    val action: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f)
        .addAnimation("pot", AnimationChannel(SCALE,
            scale(0f, 1f), scale(0.15f, 1.15f, 0.8f, 1.15f), scale(0.35f, 0.92f, 1.12f, 0.92f),
            scale(0.55f, 1.04f, 0.96f, 1.04f), scale(0.75f, 1f), scale(1f, 1f)))
        .addAnimation("stem", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.2f, 15f, 0f, 0f), rot(0.45f, -20f, 0f, 0f), rot(0.7f, 8f, 0f, 0f), rot(1f, 0f, 0f, 0f)))
        .addAnimation("leaf_left", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.3f, 0f, 0f, 35f), rot(0.6f, 0f, 0f, -10f), rot(1f, 0f, 0f, 0f)))
        .addAnimation("leaf_right", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.3f, 0f, 0f, -35f), rot(0.6f, 0f, 0f, 10f), rot(1f, 0f, 0f, 0f)))
        .build()

    /** Asleep: the sprout nods forward, leaves hang down, slow breathing. */
    val sleep: AnimationDefinition = AnimationDefinition.Builder.withLength(4.0f).looping()
        .addAnimation("stem", AnimationChannel(ROTATION,
            rot(0f, 25f, 0f, 0f), rot(2f, 30f, 0f, 0f), rot(4f, 25f, 0f, 0f)))
        .addAnimation("leaf_left", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, -45f), rot(2f, 0f, 0f, -40f), rot(4f, 0f, 0f, -45f)))
        .addAnimation("leaf_right", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 50f), rot(2f, 0f, 0f, 45f), rot(4f, 0f, 0f, 50f)))
        .addAnimation("pot", AnimationChannel(SCALE,
            scale(0f, 1f), scale(2f, 1.04f, 0.96f, 1.04f), scale(4f, 1f)))
        .build()
}
