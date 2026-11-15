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
import net.minecraft.util.Mth
import joshxviii.plantz.renderer.entity.plant.PlantRenderState

/**
 * Poppy Rattle: a poppy seed head with a face, a crown-shaped lid and four red petals.
 *
 * Parts: body → stem, leaf_left, leaf_right, pod (base, body, shoulder, neck) → lid, packet, packet2,
 * petal_fl/fr/bl/br. Texture 64×64, drawn by `art/poppy_rattle/draw_poppy_rattle.py`.
 */
class PoppyRattleModel(root: ModelPart) : SquadPlantModel(
    root,
    init = PoppyRattleAnimation.init,
    idle = PoppyRattleAnimation.idle,
    action = PoppyRattleAnimation.action,
    sleep = PoppyRattleAnimation.sleep,
) {
    private val pod = root.getChild("body").getChild("pod")
    private val lid = pod.getChild("lid")
    private val packet = pod.getChild("packet")
    private val packet2 = pod.getChild("packet2")
    private val petals = listOf("petal_fl", "petal_fr", "petal_bl", "petal_br").map { pod.getChild(it) }

    /**
     * The held packets and the night on top of the animations: the lid opens wider with each packet,
     * the packets show, a full head swells a little; at night the petals are pulled up to the "chin".
     */
    override fun setupAnim(state: PlantRenderState) {
        super.setupAnim(state)
        val count = if (state.deathTime > 0) 0 else state.squadPose.coerceIn(0, 2)
        lid.xRot += LID_OPEN[count] * Mth.DEG_TO_RAD
        packet.visible = count >= 1
        packet2.visible = count >= 2
        if (count >= 2) {
            packet.zRot += FAN * Mth.DEG_TO_RAD
            pod.xScale *= FULL_SWELL
            pod.zScale *= FULL_SWELL
        }
        val night = "night" in state.textureExtra
        petals.forEach {
            if (night) {
                it.xRot += PETAL_NIGHT_LIFT * Mth.DEG_TO_RAD
                it.zScale *= PETAL_NIGHT_LENGTH
            }
        }
    }

    companion object {
        /** Lid angle (degrees, the front lifts) with 0, 1 and 2 packets held. */
        private val LID_OPEN = floatArrayOf(0f, -30f, -50f)

        /** With two packets the front one leans aside, a fan. */
        private const val FAN = 14f
        private const val FULL_SWELL = 1.08f

        /** At night the petals rise from −30° to −95° and look shorter, hugging the head. */
        private const val PETAL_NIGHT_LIFT = -65f
        private const val PETAL_NIGHT_LENGTH = 0.75f

        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("poppy_rattle"), "main")

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val body = mesh.root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f))

            body.addOrReplaceChild("stem", CubeListBuilder.create()
                .texOffs(56, 8).addBox(-1f, -3f, -1f, 2f, 3f, 2f), PartPose.ZERO)
            // Leaves are flat planes at the foot of the stem, spread forward to the sides.
            body.addOrReplaceChild("leaf_right", CubeListBuilder.create()
                .texOffs(0, 33).addBox(0f, 0f, -2f, 7f, 0f, 4f), PartPose.offsetAndRotation(0.5f, -0.5f, 0f, 0f, 0.7f, -0.6f))
            body.addOrReplaceChild("leaf_left", CubeListBuilder.create()
                .texOffs(22, 33).addBox(-7f, 0f, -2f, 7f, 0f, 4f), PartPose.offsetAndRotation(-0.5f, -0.5f, 0f, 0f, -0.7f, 0.6f))

            val pod = body.addOrReplaceChild("pod", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-3f, -1f, -3f, 6f, 1f, 6f)      // base
                .texOffs(0, 0).addBox(-4f, -8f, -4f, 8f, 7f, 8f)       // body with the face
                .texOffs(0, 40).addBox(-3f, -9f, -3f, 6f, 1f, 6f)      // shoulder
                .texOffs(32, 8).addBox(-2f, -10f, -2f, 4f, 1f, 4f),    // neck with the seed pores
                PartPose.offset(0f, -3f, 0f))

            // The crown lid hinges on the back edge of the neck: a negative X rotation lifts its front.
            pod.addOrReplaceChild("lid", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.5f, -1f, -7f, 7f, 1f, 7f)
                .texOffs(48, 8).addBox(-1f, -2f, -4.5f, 2f, 1f, 2f),
                PartPose.offset(0f, -10f, 3.5f))

            // Seed packets inside the head, seen when the lid is open (PoppyRattleModel.setupAnim shows them).
            pod.addOrReplaceChild("packet", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2f, -6f, -0.5f, 4f, 6f, 1f), PartPose.offset(0f, -6f, 0f))
            pod.addOrReplaceChild("packet2", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2f, -6f, -0.5f, 4f, 6f, 1f), PartPose.offsetAndRotation(1.5f, -6f, 1f, 0f, 0f, 0.3f))

            // Petals point out of the base's corners (yaw ±45°, ±135°), opened as a cup by day (X −30°).
            // Spelled out one by one: tools/docs/render_character.py reads the literal numbers.
            pod.addOrReplaceChild("petal_fl", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2.5f, 0f, -5f, 5f, 0f, 5f), PartPose.offsetAndRotation(-3f, 0f, -3f, -0.5236f, 0.7854f, 0f))
            pod.addOrReplaceChild("petal_fr", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2.5f, 0f, -5f, 5f, 0f, 5f), PartPose.offsetAndRotation(3f, 0f, -3f, -0.5236f, -0.7854f, 0f))
            pod.addOrReplaceChild("petal_bl", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2.5f, 0f, -5f, 5f, 0f, 5f), PartPose.offsetAndRotation(-3f, 0f, 3f, -0.5236f, 2.3562f, 0f))
            pod.addOrReplaceChild("petal_br", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-2.5f, 0f, -5f, 5f, 0f, 5f), PartPose.offsetAndRotation(3f, 0f, 3f, -0.5236f, -2.3562f, 0f))
            return LayerDefinition.create(mesh, 64, 64)
        }
    }
}

/** Keyframes for [PoppyRattleModel]: rotations in degrees (added to the pose), scales as multipliers. */
object PoppyRattleAnimation {
    private fun rot(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), CATMULLROM)
    private fun lin(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), LINEAR)
    private fun scale(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, scaleVec(x.toDouble(), y.toDouble(), z.toDouble()), CATMULLROM)
    private fun scale(t: Float, s: Float) = scale(t, s, s, s)
    private val PETALS = listOf("petal_fl", "petal_fr", "petal_bl", "petal_br")

    /** Swells out of a bud; the petals unfold last. */
    val init: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f)
        .addAnimation("pod", AnimationChannel(SCALE,
            scale(0f, 0.2f), scale(0.35f, 1.15f), scale(0.6f, 0.95f), scale(1f, 1f)))
        .apply {
            PETALS.forEach {
                addAnimation(it, AnimationChannel(SCALE,
                    Keyframe(0f, scaleVec(0.0, 0.0, 0.0), LINEAR), Keyframe(0.5f, scaleVec(0.0, 0.0, 0.0), LINEAR),
                    scale(0.8f, 1.15f), scale(1f, 1f)))
            }
        }
        .build()

    /** Sways gently; once every 10 s it shakes like a maraca (the seeds rattle). */
    val idle: AnimationDefinition = AnimationDefinition.Builder.withLength(10.0f).looping()
        .addAnimation("pod", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(2f, 0f, 0f, 3f), rot(4f, 0f, 0f, 0f), rot(6f, 0f, 0f, -3f), rot(8f, 0f, 0f, 0f),
            lin(8.5f, 0f, 0f, 0f), lin(8.6f, 0f, 0f, 8f), lin(8.7f, 0f, 0f, -8f), lin(8.8f, 0f, 0f, 8f),
            lin(8.9f, 0f, 0f, -8f), lin(9.0f, 0f, 0f, 6f), lin(9.1f, 0f, 0f, -6f), lin(9.2f, 0f, 0f, 3f),
            lin(9.5f, 0f, 0f, 0f), rot(10f, 0f, 0f, 0f)))
        .addAnimation("lid", AnimationChannel(ROTATION,
            lin(0f, 0f, 0f, 0f), lin(8.6f, 0f, 0f, 0f), lin(8.7f, -6f, 0f, 0f), lin(8.8f, 0f, 0f, 0f),
            lin(8.9f, -6f, 0f, 0f), lin(9.0f, 0f, 0f, 0f), lin(10f, 0f, 0f, 0f)))
        .build()

    /** A seed packet is ready: the head jolts and the lid hops. */
    val action: AnimationDefinition = AnimationDefinition.Builder.withLength(0.8f)
        .addAnimation("pod", AnimationChannel(SCALE,
            scale(0f, 1f), scale(0.15f, 1.12f, 0.9f, 1.12f), scale(0.35f, 0.94f, 1.08f, 0.94f), scale(0.55f, 1.03f, 0.97f, 1.03f), scale(0.8f, 1f)))
        .addAnimation("lid", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.25f, -35f, 0f, 0f), rot(0.55f, 4f, 0f, 0f), rot(0.8f, 0f, 0f, 0f)))
        .build()

    /** It never sleeps: this is only its dying pose — petals drop, the head sags. */
    val sleep: AnimationDefinition = AnimationDefinition.Builder.withLength(4.0f).looping()
        .apply {
            PETALS.forEach { addAnimation(it, AnimationChannel(ROTATION, rot(0f, 40f, 0f, 0f), rot(4f, 40f, 0f, 0f))) }
        }
        .addAnimation("pod", AnimationChannel(SCALE, scale(0f, 0.95f), scale(4f, 0.95f)))
        .build()
}
