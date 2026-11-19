package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.entity.plant.PlantRenderState
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
import net.minecraft.util.Mth

/**
 * Double Sunflower: the Sunflower's parts (head, petals, stem, leaves; SunflowerModel) on two stems.
 *
 * Parts: body → stem (common) → stem_tall_1..3 → head_tall (the Sunflower's face) and
 * stem_short_1..3 → head_short (the winking face, texture block at 0, 50); body → leaves → leaf_1..6 → leaf_tip_1..6.
 * The poses are in the parts themselves, so the animations only add to them.
 * Texture 64×64, drawn by .
 */
class DoubleSunflowerModel(root: ModelPart) : SquadPlantModel(
    root,
    init = DoubleSunflowerAnimation.init,
    idle = DoubleSunflowerAnimation.idle,
    action = DoubleSunflowerAnimation.action,
    sleep = DoubleSunflowerAnimation.sleep,
) {
    private val stem: ModelPart = root.getChild("body").getChild("stem")
    private val headTall: ModelPart = stem.getChild("stem_tall_1").getChild("stem_tall_2").getChild("stem_tall_3").getChild("head_tall")
    private val headShort: ModelPart = stem.getChild("stem_short_1").getChild("stem_short_2").getChild("stem_short_3").getChild("head_short")

    /** Both heads look up and down at the target, the stems turn to it, like the Sunflower. */
    override fun setupAnim(state: PlantRenderState) {
        super.setupAnim(state)
        headTall.xRot += state.xRot * Mth.DEG_TO_RAD
        headShort.xRot += state.xRot * Mth.DEG_TO_RAD
        stem.yRot += state.yRot * Mth.DEG_TO_RAD
    }

    companion object {
        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("double_sunflower"), "main")

        // Spelled out part by part: tools/docs/render_character.py reads the literal numbers.
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val body = mesh.root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f))
            val stem = body.addOrReplaceChild("stem", CubeListBuilder.create()
                .texOffs(4, 25).addBox(-1f, -5f, -1f, 2f, 5f, 2f),
                PartPose.offsetAndRotation(0f, 0f, 0f, 0.1396f, 0f, 0f))
            // the tall stem stands behind with the Sunflower's face
            val stem_tall_1 = stem.addOrReplaceChild("stem_tall_1", CubeListBuilder.create()
                .texOffs(4, 18).addBox(-1f, -6f, -1f, 2f, 6f, 2f),
                PartPose.offsetAndRotation(0f, -5f, 0f, -0.1396f, 0f, 0.5236f))
            val stem_tall_2 = stem_tall_1.addOrReplaceChild("stem_tall_2", CubeListBuilder.create()
                .texOffs(4, 18).addBox(-1f, -7f, -1f, 2f, 7f, 2f),
                PartPose.offsetAndRotation(0f, -6f, 0f, -0.1396f, 0f, -0.3491f))
            stem_tall_2.addOrReplaceChild("leaf_tall", CubeListBuilder.create()
                .texOffs(25, 4).mirror().addBox(-5f, 0f, -2.5f, 5f, 0f, 5f).mirror(false),
                PartPose.offsetAndRotation(-1f, -1f, 0f, 1.0457f, -0.3766f, 0.2495f))
            val stem_tall_3 = stem_tall_2.addOrReplaceChild("stem_tall_3", CubeListBuilder.create()
                .texOffs(12, 18).addBox(-1f, -5f, -1f, 2f, 5f, 2f),
                PartPose.offsetAndRotation(0f, -7f, 0f, -0.0698f, 0f, -0.1396f))
            val head_tall = stem_tall_3.addOrReplaceChild("head_tall", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6f, -5.0071f, -1.9867f, 12f, 9f, 2f)
                .texOffs(3, 13).addBox(-2f, -2.0071f, 0.0133f, 4f, 4f, 1f),
                PartPose.offsetAndRotation(0f, -3.9929f, -1.0133f, 0.2618f, 0.2094f, 0.0349f))
            head_tall.addOrReplaceChild("tall_petal_left", CubeListBuilder.create()
                .texOffs(38, 33).addBox(-7f, -8f, 0f, 7f, 16f, 0f),
                PartPose.offsetAndRotation(-2f, -0.0071f, 0.0133f, 0f, -0.2182f, 0f))
            head_tall.addOrReplaceChild("tall_petal_right", CubeListBuilder.create()
                .texOffs(38, 33).mirror().addBox(0f, -8f, 0f, 7f, 16f, 0f).mirror(false),
                PartPose.offsetAndRotation(2f, -0.0071f, 0.0133f, 0f, 0.2182f, 0f))
            head_tall.addOrReplaceChild("tall_petal_top", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4f, 0f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -2.0071f, 0.0133f, -0.7854f, 0f, 0f))
            head_tall.addOrReplaceChild("tall_petal_bottom", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4f, 0f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, 1.9929f, 0.0133f, -2.3562f, 0f, 0f))
            head_tall.addOrReplaceChild("tall_petal_side_top", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4.4142f, -2.4142f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -0.0071f, 1.4275f, -0.7854f, 0f, -1.5708f))
            head_tall.addOrReplaceChild("tall_petal_side_bottom", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4.4142f, 2.4142f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -0.0071f, 1.4275f, -2.3562f, 0f, -1.5708f))
            head_tall.addOrReplaceChild("tall_petal_ring_lower", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-9f, 1f, -1f, 18f, 5f, 0f),
                PartPose.offsetAndRotation(0f, 1.9929f, 1.0133f, -0.3054f, 0f, 0f))
            head_tall.addOrReplaceChild("tall_petal_ring_upper", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-9f, -7f, 0f, 18f, 7f, 0f),
                PartPose.offsetAndRotation(0f, -2.0071f, 0.0133f, 0.3054f, 0f, 0f))
            // the short stem leans forward, its head winks (texture block at 0, 50)
            val stem_short_1 = stem.addOrReplaceChild("stem_short_1", CubeListBuilder.create()
                .texOffs(4, 18).addBox(-1f, -5f, -1f, 2f, 5f, 2f),
                PartPose.offsetAndRotation(0f, -5f, 0f, 0.3491f, 0f, -1.117f))
            val stem_short_2 = stem_short_1.addOrReplaceChild("stem_short_2", CubeListBuilder.create()
                .texOffs(4, 18).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                PartPose.offsetAndRotation(0f, -5f, 0f, -0.1396f, 0f, 0.4538f))
            stem_short_2.addOrReplaceChild("leaf_short", CubeListBuilder.create()
                .texOffs(25, 4).addBox(0f, 0f, -2.5f, 5f, 0f, 5f),
                PartPose.offsetAndRotation(1f, -1f, 0f, 0.7648f, 0.3376f, -0.2222f))
            val stem_short_3 = stem_short_2.addOrReplaceChild("stem_short_3", CubeListBuilder.create()
                .texOffs(12, 18).addBox(-1f, -4f, -1f, 2f, 4f, 2f),
                PartPose.offsetAndRotation(0f, -4f, 0f, -0.1047f, 0f, 0.4189f))
            val head_short = stem_short_3.addOrReplaceChild("head_short", CubeListBuilder.create()
                .texOffs(0, 50).addBox(-6f, -5.0071f, -1.9867f, 12f, 9f, 2f)
                .texOffs(3, 13).addBox(-2f, -2.0071f, 0.0133f, 4f, 4f, 1f),
                PartPose.offsetAndRotation(0f, -3.9929f, -1.0133f, -0.192f, -0.0698f, 0.2443f))
            head_short.addOrReplaceChild("short_petal_left", CubeListBuilder.create()
                .texOffs(38, 33).addBox(-7f, -8f, 0f, 7f, 16f, 0f),
                PartPose.offsetAndRotation(-2f, -0.0071f, 0.0133f, 0f, -0.2182f, 0f))
            head_short.addOrReplaceChild("short_petal_right", CubeListBuilder.create()
                .texOffs(38, 33).mirror().addBox(0f, -8f, 0f, 7f, 16f, 0f).mirror(false),
                PartPose.offsetAndRotation(2f, -0.0071f, 0.0133f, 0f, 0.2182f, 0f))
            head_short.addOrReplaceChild("short_petal_top", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4f, 0f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -2.0071f, 0.0133f, -0.7854f, 0f, 0f))
            head_short.addOrReplaceChild("short_petal_bottom", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4f, 0f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, 1.9929f, 0.0133f, -2.3562f, 0f, 0f))
            head_short.addOrReplaceChild("short_petal_side_top", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4.4142f, -2.4142f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -0.0071f, 1.4275f, -0.7854f, 0f, -1.5708f))
            head_short.addOrReplaceChild("short_petal_side_bottom", CubeListBuilder.create()
                .texOffs(29, 0).addBox(-3f, -4.4142f, 2.4142f, 6f, 4f, 0f),
                PartPose.offsetAndRotation(0f, -0.0071f, 1.4275f, -2.3562f, 0f, -1.5708f))
            head_short.addOrReplaceChild("short_petal_ring_lower", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-9f, 1f, -1f, 18f, 5f, 0f),
                PartPose.offsetAndRotation(0f, 1.9929f, 1.0133f, -0.3054f, 0f, 0f))
            head_short.addOrReplaceChild("short_petal_ring_upper", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-9f, -7f, 0f, 18f, 7f, 0f),
                PartPose.offsetAndRotation(0f, -2.0071f, 0.0133f, 0.3054f, 0f, 0f))
            val leaves = body.addOrReplaceChild("leaves", CubeListBuilder.create(), PartPose.offset(0f, 0f, 0f))
            val leaf_1 = leaves.addOrReplaceChild("leaf_1", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0.7071f, 0f, -0.6464f, -0.7854f, -0.7854f, 0f))
            leaf_1.addOrReplaceChild("leaf_tip_1", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))
            val leaf_2 = leaves.addOrReplaceChild("leaf_2", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0.7071f, 0f, 0.7678f, -0.7854f, -2.3562f, 0f))
            leaf_2.addOrReplaceChild("leaf_tip_2", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))
            val leaf_3 = leaves.addOrReplaceChild("leaf_3", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(-0.7071f, 0f, -0.6464f, -0.7854f, 0.7854f, 0f))
            leaf_3.addOrReplaceChild("leaf_tip_3", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))
            val leaf_4 = leaves.addOrReplaceChild("leaf_4", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(-0.7071f, 0f, 0.7678f, -0.7854f, 2.3562f, 0f))
            leaf_4.addOrReplaceChild("leaf_tip_4", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))
            val leaf_5 = leaves.addOrReplaceChild("leaf_5", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(-1f, 0f, 0f, -0.7854f, 1.5708f, 0f))
            leaf_5.addOrReplaceChild("leaf_tip_5", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))
            val leaf_6 = leaves.addOrReplaceChild("leaf_6", CubeListBuilder.create()
                .texOffs(31, 10).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(1f, 0f, 0f, -0.7854f, -1.5708f, 0f))
            leaf_6.addOrReplaceChild("leaf_tip_6", CubeListBuilder.create()
                .texOffs(31, 16).addBox(-3f, 0f, -6f, 6f, 0f, 6f),
                PartPose.offsetAndRotation(0f, 0f, -6f, 1.1781f, 0f, 0f))


            return LayerDefinition.create(mesh, 64, 64)
        }
    }
}

/** Keyframes for [DoubleSunflowerModel]: rotations in degrees added to the parts' poses, scales as multipliers. */
object DoubleSunflowerAnimation {
    private fun rot(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), CATMULLROM)
    private fun scale(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, scaleVec(x.toDouble(), y.toDouble(), z.toDouble()), CATMULLROM)
    private fun scale(t: Float, s: Float) = scale(t, s, s, s)
    private val LEAVES = (1..6).map { "leaf_$it" }

    /** Grows out of the ground like the Sunflower: the common stem, then the tall stem, then the short one. */
    val init: AnimationDefinition = AnimationDefinition.Builder.withLength(1.2f)
        .addAnimation("stem", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(0.0, 0.0, 1.0), LINEAR), scale(0.16f, 0.8f, 1.3f, 0.8f), scale(0.24f, 1.1f, 0.9f, 1.1f), scale(0.32f, 1f)))
        .addAnimation("stem_tall_1", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(1.0, 0.0, 1.0), LINEAR), Keyframe(0.2f, scaleVec(1.0, 0.0, 1.0), LINEAR),
            scale(0.4f, 0.9f, 1.2f, 0.9f), scale(0.56f, 1f)))
        .addAnimation("stem_short_1", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(1.0, 0.0, 1.0), LINEAR), Keyframe(0.35f, scaleVec(1.0, 0.0, 1.0), LINEAR),
            scale(0.55f, 0.9f, 1.2f, 0.9f), scale(0.7f, 1f)))
        .addAnimation("head_tall", AnimationChannel(SCALE,
            scale(0f, 0.5f, 0.5f, 1f), scale(0.44f, 0.6f, 0.7f, 1.6f), scale(0.72f, 1.1f, 1.22f, 0.8f), scale(0.84f, 1f), scale(1.2f, 1f)))
        .addAnimation("head_short", AnimationChannel(SCALE,
            scale(0f, 0.5f, 0.5f, 1f), scale(0.6f, 0.6f, 0.7f, 1.6f), scale(0.88f, 1.1f, 1.22f, 0.8f), scale(1f, 1f), scale(1.2f, 1f)))
        .addAnimation("head_tall", AnimationChannel(ROTATION,
            rot(0f, -60f, 0f, 0f), rot(0.6f, -80f, 0f, 0f), rot(0.8f, -20f, 0f, 0f), rot(1f, 0f, 0f, 0f)))
        .addAnimation("head_short", AnimationChannel(ROTATION,
            rot(0f, -60f, 0f, 0f), rot(0.76f, -80f, 0f, 0f), rot(0.96f, -20f, 0f, 0f), rot(1.2f, 0f, 0f, 0f)))
        .apply {
            LEAVES.forEach {
                addAnimation(it, AnimationChannel(ROTATION,
                    rot(0f, -15f, 0f, 0f), rot(0.08f, -15f, 0f, 0f), rot(0.12f, 10f, 0f, 0f), rot(0.24f, -5f, 0f, 0f), rot(0.32f, 0f, 0f, 0f)))
                addAnimation(it.replace("leaf_", "leaf_tip_"), AnimationChannel(ROTATION,
                    rot(0f, -130f, 0f, 0f), rot(0.08f, -130f, 0f, 0f), rot(0.16f, 0f, 0f, 0f)))
            }
        }
        .build()

    /** The heads sway in turn: when the tall one leans one way, the short one leans the other. */
    val idle: AnimationDefinition = AnimationDefinition.Builder.withLength(2.0f).looping()
        .addAnimation("head_tall", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.5f, -10f, 2f, 5f), rot(1f, 0f, 0f, 0f), rot(1.5f, -10f, -2f, -5f), rot(2f, 0f, 0f, 0f)))
        .addAnimation("head_short", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.5f, -8f, -2f, -6f), rot(1f, 0f, 0f, 0f), rot(1.5f, -8f, 2f, 6f), rot(2f, 0f, 0f, 0f)))
        .addAnimation("stem_tall_2", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.5f, 0f, 0f, 3f), rot(1f, 0f, 0f, 0f), rot(1.5f, 0f, 0f, -3f), rot(2f, 0f, 0f, 0f)))
        .addAnimation("stem_short_2", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.5f, 0f, 0f, -4f), rot(1f, 0f, 0f, 0f), rot(1.5f, 0f, 0f, 4f), rot(2f, 0f, 0f, 0f)))
        .addAnimation("stem", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(1f, 3f, 0f, 0f), rot(2f, 0f, 0f, 0f)))
        .build()

    /** Making sun: both heads bob and squash like the Sunflower's, the short one a beat later. */
    val action: AnimationDefinition = AnimationDefinition.Builder.withLength(1.1f)
        .addAnimation("head_tall", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.16f, 20f, -3f, -3f), rot(0.4f, -40f, -10f, -10f), rot(0.52f, -45f, 5f, 0f), rot(1f, 0f, 0f, 0f)))
        .addAnimation("head_short", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.26f, 20f, 3f, 3f), rot(0.5f, -40f, 10f, 10f), rot(0.62f, -45f, -5f, 0f), rot(1.1f, 0f, 0f, 0f)))
        .addAnimation("head_tall", AnimationChannel(SCALE,
            scale(0f, 1f), scale(0.24f, 0.9f, 0.8f, 1.9f), scale(0.48f, 1.2f, 1.2f, 0.8f), scale(0.64f, 0.9f), scale(0.72f, 1f)))
        .addAnimation("head_short", AnimationChannel(SCALE,
            scale(0f, 1f), scale(0.1f, 1f), scale(0.34f, 0.9f, 0.8f, 1.9f), scale(0.58f, 1.2f, 1.2f, 0.8f), scale(0.74f, 0.9f), scale(0.82f, 1f)))
        .addAnimation("stem", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.12f, 15f, 0f, 0f), rot(0.3f, 2f, 0f, 0f), rot(0.7f, 2f, 0f, 0f), rot(1.1f, 0f, 0f, 0f)))
        .build()

    /** Asleep: both stems bend, the heads hang down and breathe slowly, out of step. */
    val sleep: AnimationDefinition = AnimationDefinition.Builder.withLength(5.0f).looping()
        .addAnimation("stem_tall_2", AnimationChannel(ROTATION,
            rot(0f, -8f, 0f, 0f), rot(2.5f, -12f, 0f, 0f), rot(5f, -8f, 0f, 0f)))
        .addAnimation("head_tall", AnimationChannel(ROTATION,
            rot(0f, -18f, 0f, 8f), rot(2.5f, -24f, 0f, 8f), rot(5f, -18f, 0f, 8f)))
        .addAnimation("stem_short_2", AnimationChannel(ROTATION,
            rot(0f, -6f, 0f, 0f), rot(1.5f, -3f, 0f, 0f), rot(3.5f, -10f, 0f, 0f), rot(5f, -6f, 0f, 0f)))
        .addAnimation("head_short", AnimationChannel(ROTATION,
            rot(0f, -16f, 0f, -10f), rot(1.5f, -12f, 0f, -10f), rot(3.5f, -22f, 0f, -10f), rot(5f, -16f, 0f, -10f)))
        .build()
}
