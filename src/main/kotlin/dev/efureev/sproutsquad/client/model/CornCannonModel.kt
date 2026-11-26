package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.pazResource
import joshxviii.plantz.renderer.entity.plant.PlantRenderState
import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationChannel.Interpolations.CATMULLROM
import net.minecraft.client.animation.AnimationChannel.Interpolations.LINEAR
import net.minecraft.client.animation.AnimationChannel.Targets.POSITION
import net.minecraft.client.animation.AnimationChannel.Targets.ROTATION
import net.minecraft.client.animation.AnimationChannel.Targets.SCALE
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.animation.KeyframeAnimations.degreeVec
import net.minecraft.client.animation.KeyframeAnimations.posVec
import net.minecraft.client.animation.KeyframeAnimations.scaleVec
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.util.Mth

/**
 * Corn Cannon: a long husk lying on a rosette of leaves, its leaves hugging a big corn cob that sticks out
 * of the front (north), a curly tendril at the back.
 *
 * Parts: body (turns with the head, like the Melon Pult) → husk (tilts about its bottom near the tail) →
 * cob, tendril; ground (the leaf rosette, does not turn). Texture 128×64 and the boxes below come from
 * `art/corn_cannon/draw_corn_cannon.py` (its `--kotlin` output: keep the texOffs in step).
 *
 * The cob is gone after a shot and grows back while the plant cools down (CornCannon.COOLDOWN ticks):
 * it slides out of the husk from deep inside, growing from half size (CornCannon.cobGrowth, passed as the pose).
 * While it is small, the dark mouth painted on the front of the husk shows.
 */
class CornCannonModel(root: ModelPart) : SquadPlantModel(
    root,
    init = CornCannonAnimation.init,
    idle = CornCannonAnimation.idle,
    action = CornCannonAnimation.action,
    sleep = CornCannonAnimation.sleep,
) {
    private val body = root.getChild("body")
    private val husk = body.getChild("husk")
    private val cob = husk.getChild("cob")

    override fun setupAnim(state: PlantRenderState) {
        super.setupAnim(state)
        body.yRot += state.yRot * Mth.DEG_TO_RAD
        val grown = state.squadPose / 1000f // CornCannon.cobGrowth(), by the cooldown the shot started
        val size = 0.5f + 0.5f * grown
        cob.z += (1f - grown) * COB_RETRACT
        cob.xScale *= size
        cob.yScale *= size
        cob.zScale *= size
        cob.visible = grown > 0f
    }

    companion object {
        @JvmField val LAYER_LOCATION = ModelLayerLocation(pazResource("corn_cannon"), "main")

        /** How far back into the husk an empty cob sits, in pixels. */
        private const val COB_RETRACT = 12f

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val body = mesh.root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0f, 24f, 0f))

            val husk = body.addOrReplaceChild("husk", CubeListBuilder.create()
                .texOffs(64, 19).addBox(-3f, -8f, 4f, 6f, 6f, 2f)      // husk_back
                .texOffs(40, 19).addBox(-4f, -9f, 0f, 8f, 8f, 4f)      // husk_rear
                .texOffs(38, 0).addBox(-5f, -10f, -8f, 10f, 10f, 8f)   // husk_mid, with the eyes
                .texOffs(74, 0).addBox(-5f, -10f, -12f, 10f, 10f, 4f)  // husk_front, the dark mouth painted on its front
                .texOffs(96, 19).addBox(-5f, -11f, -17f, 10f, 2f, 5f)  // lid over the cob
                .texOffs(12, 32).addBox(-3f, -12f, -20f, 6f, 2f, 3f)   // lid_tip
                .texOffs(88, 32).addBox(-1f, -13f, -22f, 2f, 1f, 2f)   // lid_curl
                .texOffs(30, 32).addBox(-4f, -1f, -16f, 8f, 1f, 4f)    // cradle under the cob
                .texOffs(0, 32).addBox(-5f, -5f, -15f, 1f, 4f, 3f)     // leaf_left
                .texOffs(0, 32).addBox(4f, -5f, -15f, 1f, 4f, 3f),     // leaf_right
                PartPose.offset(0f, -1f, 8f))
            husk.addOrReplaceChild("cob", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4f, -4f, -11f, 8f, 8f, 11f)
                .texOffs(80, 19).addBox(-3f, -3f, -13f, 6f, 6f, 2f)
                .texOffs(54, 32).addBox(-2f, -2f, -14f, 4f, 4f, 1f),
                PartPose.offset(0f, -5f, -9f))
            husk.addOrReplaceChild("tendril", CubeListBuilder.create()
                .texOffs(80, 32).addBox(-1f, -1f, 0f, 1f, 1f, 3f)
                .texOffs(8, 32).addBox(-1f, -5f, 3f, 1f, 5f, 1f)
                .texOffs(96, 32).addBox(-1f, -5f, 1f, 1f, 1f, 2f)
                .texOffs(102, 32).addBox(-1f, -4f, 1f, 1f, 2f, 1f)
                .texOffs(106, 32).addBox(-1f, -6f, 3f, 3f, 1f, 2f),
                PartPose.offset(0f, -5f, 6f))
            // the rosette stays put on the ground while the body turns its head
            mesh.root.addOrReplaceChild("ground", CubeListBuilder.create()
                .texOffs(0, 19).addBox(-4f, -1f, -15f, 8f, 1f, 12f)
                .texOffs(64, 32).addBox(-8f, -1f, -17f, 4f, 1f, 4f)
                .texOffs(64, 32).addBox(4f, -1f, -17f, 4f, 1f, 4f)
                .texOffs(64, 32).addBox(-8f, -1f, -7f, 4f, 1f, 4f)
                .texOffs(64, 32).addBox(4f, -1f, -7f, 4f, 1f, 4f),
                PartPose.offset(0f, 24f, 14f))

            return LayerDefinition.create(mesh, 128, 64)
        }
    }
}

/** Keyframes for [CornCannonModel]: rotations in degrees (negative X lifts the nose), positions in pixels. */
object CornCannonAnimation {
    private fun rot(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, degreeVec(x, y, z), CATMULLROM)
    private fun pos(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, posVec(x, y, z), CATMULLROM)
    private fun scale(t: Float, x: Float, y: Float, z: Float) = Keyframe(t, scaleVec(x.toDouble(), y.toDouble(), z.toDouble()), CATMULLROM)

    /** The husk swells up out of its leaves (the cob then grows out of it, see the model). */
    val init: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f)
        .addAnimation("husk", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(0.2, 0.0, 0.2), LINEAR), scale(0.4f, 1.1f, 1.15f, 1.05f), scale(0.7f, 0.97f, 0.95f, 1f), scale(1f, 1f, 1f, 1f)))
        .addAnimation("ground", AnimationChannel(SCALE,
            Keyframe(0f, scaleVec(0.0, 1.0, 0.0), LINEAR), scale(0.3f, 1.1f, 1f, 1.1f), scale(0.5f, 1f, 1f, 1f)))
        .build()

    /** It breathes a little and its tendril sways. */
    val idle: AnimationDefinition = AnimationDefinition.Builder.withLength(3.0f).looping()
        .addAnimation("husk", AnimationChannel(SCALE,
            scale(0f, 1f, 1f, 1f), scale(1.5f, 1.02f, 1.03f, 1f), scale(3f, 1f, 1f, 1f)))
        .addAnimation("tendril", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.75f, 0f, 0f, 12f), rot(1.5f, 0f, 0f, 0f), rot(2.25f, 0f, 0f, -12f), rot(3f, 0f, 0f, 0f)))
        .build()

    /**
     * A shot: the nose lifts, the cob leaves about 0.5 s in (CornCannon.ACTION_DELAY), the husk kicks back
     * and settles.
     */
    val action: AnimationDefinition = AnimationDefinition.Builder.withLength(1.1f)
        .addAnimation("husk", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.45f, -18f, 0f, 0f), rot(0.6f, -22f, 0f, 0f), rot(0.8f, -6f, 0f, 0f), rot(1.1f, 0f, 0f, 0f)))
        .addAnimation("husk", AnimationChannel(POSITION,
            pos(0f, 0f, 0f, 0f), pos(0.5f, 0f, 0f, 0f), pos(0.6f, 0f, 0f, 2f), pos(1.1f, 0f, 0f, 0f)))
        .addAnimation("tendril", AnimationChannel(ROTATION,
            rot(0f, 0f, 0f, 0f), rot(0.55f, 25f, 0f, 0f), rot(0.8f, -10f, 0f, 0f), rot(1.1f, 0f, 0f, 0f)))
        .build()

    /** It does not sleep at night; this pose (the nose droops) only completes the set. */
    val sleep: AnimationDefinition = AnimationDefinition.Builder.withLength(1.0f).looping()
        .addAnimation("husk", AnimationChannel(ROTATION, rot(0f, 6f, 0f, 0f), rot(1f, 6f, 0f, 0f)))
        .addAnimation("tendril", AnimationChannel(ROTATION, rot(0f, 30f, 0f, 0f), rot(1f, 30f, 0f, 0f)))
        .build()
}
