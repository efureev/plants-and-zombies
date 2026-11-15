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
import kotlin.math.sin

/**
 * Needle Spruce: a trunk that bends in two places and a three-tier crown with eyes on the front of
 * the bottom tier and four sharp fangs hanging under its front edge. Three blocks tall.
 *
 * Parts: body → trunk_lower → trunk_upper → crown → tier1 (face on the north side), tier2, tier3, top,
 * fang_0..fang_3. Texture 128×64, drawn by `art/needle_spruce/draw_needle_spruce.py` (the UV origins
 * there must match the texOffs here). Positive X rotation leans forward, towards the target.
 *
 * Death: the trunk snaps off at the bend and falls back while the renderer shrinks it (all plants do).
 * When a stump block took the lower trunk's place (`useSpecialAction`, NeedleSpruce.standsOnItsStump),
 * the lower trunk is not drawn.
 */
class NeedleSpruceModel(root: ModelPart) : SquadPlantModel(
    root,
    init = NeedleSpruceAnimation.init,
    idle = NeedleSpruceAnimation.idle,
    action = NeedleSpruceAnimation.action,
    sleep = NeedleSpruceAnimation.sleep,
) {
    private val trunkLower = root.getChild("body").getChild("trunk_lower")
    private val trunkUpper = trunkLower.getChild("trunk_upper")
    private val crown = trunkUpper.getChild("crown")

    override fun setupAnim(state: PlantRenderState) {
        super.setupAnim(state)
        val dying = state.deathTime > 0
        trunkLower.skipDraw = dying && state.useSpecialAction // the model is shared: reset it for the others
        if (!dying) return
        val fall = (state.deathTime / DEATH_TICKS).coerceAtMost(1f)
        trunkLower.xRot = 0f
        trunkUpper.xRot = FALL_ANGLE * fall * fall
        crown.zRot = if (state.deathTime < SHUDDER_TICKS) sin(state.ageInTicks * 3f) * 3f * Mth.DEG_TO_RAD else 0f
    }

    companion object {
        private const val DEATH_TICKS = 20f
        private const val SHUDDER_TICKS = 6f
        private const val FALL_ANGLE = -100f * Mth.DEG_TO_RAD

        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("needle_spruce"), "main")

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

            // Fangs under the front edge of tier1, 3 pixels in: three boxes narrowing down to a one-pixel
            // tip; the middle pair, under the eyes, is longer. Spelled out (no loop) so that
            // tools/docs/render_character.py, which reads this code, draws them too.
            crown.addOrReplaceChild("fang_0", CubeListBuilder.create()
                .texOffs(96, 24).addBox(-1.5f, 0f, -1.5f, 3f, 2f, 3f)
                .texOffs(108, 24).addBox(-1f, 2f, -1f, 2f, 2f, 2f)
                .texOffs(116, 24).addBox(-0.5f, 4f, -0.5f, 1f, 2f, 1f),
                PartPose.offset(-8f, 0f, -7f))
            crown.addOrReplaceChild("fang_1", CubeListBuilder.create()
                .texOffs(96, 16).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f)
                .texOffs(108, 16).addBox(-1f, 3f, -1f, 2f, 2f, 2f)
                .texOffs(116, 16).addBox(-0.5f, 5f, -0.5f, 1f, 3f, 1f),
                PartPose.offset(-4f, 0f, -7f))
            crown.addOrReplaceChild("fang_2", CubeListBuilder.create()
                .texOffs(96, 16).addBox(-1.5f, 0f, -1.5f, 3f, 3f, 3f)
                .texOffs(108, 16).addBox(-1f, 3f, -1f, 2f, 2f, 2f)
                .texOffs(116, 16).addBox(-0.5f, 5f, -0.5f, 1f, 3f, 1f),
                PartPose.offset(4f, 0f, -7f))
            crown.addOrReplaceChild("fang_3", CubeListBuilder.create()
                .texOffs(96, 24).addBox(-1.5f, 0f, -1.5f, 3f, 2f, 3f)
                .texOffs(108, 24).addBox(-1f, 2f, -1f, 2f, 2f, 2f)
                .texOffs(116, 24).addBox(-0.5f, 4f, -0.5f, 1f, 2f, 1f),
                PartPose.offset(8f, 0f, -7f))

            return LayerDefinition.create(mesh, 128, 64)
        }
    }
}

/** Keyframes for [NeedleSpruceModel]: rotations in degrees (positive X leans forward), scales as multipliers. */
object NeedleSpruceAnimation {
    private fun rot(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), CATMULLROM)
    private fun scale(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, scaleVec(x.toDouble(), y.toDouble(), z.toDouble()), CATMULLROM)
    private fun scale(t: Float, s: Float) = scale(t, s, s, s)
    private fun hidden(t: Float) = Keyframe(t, scaleVec(0.0, 0.0, 0.0), LINEAR)

    /** Grows out of the ground: the trunk shoots up, the tiers open from the bottom, the fangs come last. */
    val init: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f).apply {
        addAnimation("trunk_lower", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(1.0, 0.0, 1.0), LINEAR), scale(0.3f, 1f, 1.1f, 1f), scale(0.45f, 1f)))
        listOf("tier1" to 0.3f, "tier2" to 0.42f, "tier3" to 0.54f, "top" to 0.64f).forEach { (part, start) ->
            addAnimation(part, AnimationChannel(SCALE, hidden(0f), hidden(start), scale(start + 0.15f, 1.2f), scale(start + 0.3f, 1f)))
        }
        (0..3).forEach { i ->
            addAnimation("fang_$i", AnimationChannel(SCALE, hidden(0f), hidden(0.75f), scale(0.9f, 1.1f), scale(1f, 1f)))
        }
    }.build()

    /** The crown sways a little. */
    val idle: AnimationDefinition = AnimationDefinition.Builder.withLength(3.0f).looping()
        .addAnimation("crown", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.75f, 0f, 0f, 1.5f), rot(1.5f, 0f, 0f, 0f), rot(2.25f, 0f, 0f, -1.5f), rot(3f, 0f, 0f, 0f)))
        .build()

    /**
     * A volley, like a catapult: the trunk winds back, whips forward, overshoots back and settles. The
     * needles leave about 0.45 s in (NeedleSpruce.ACTION_DELAY: the client starts this animation a tick
     * after the action starts), at the peak of the whip.
     */
    val action: AnimationDefinition = AnimationDefinition.Builder.withLength(1.05f)
        .addAnimation("trunk_lower", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.25f, -8f, 0f, 0f), rot(0.45f, 22f, 0f, 0f), rot(0.7f, -6f, 0f, 0f), rot(1.05f, 0f, 0f, 0f)))
        .addAnimation("trunk_upper", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.25f, -6f, 0f, 0f), rot(0.45f, 24f, 0f, 0f), rot(0.7f, -4f, 0f, 0f), rot(1.05f, 0f, 0f, 0f)))
        .build()

    /** The spruce does not sleep; this pose (the idle sway) only completes the set. */
    val sleep: AnimationDefinition = idle
}
