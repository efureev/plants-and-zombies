package joshxviii.plantz.renderer

import com.mojang.blaze3d.vertex.PoseStack
import joshxviii.plantz.PazEffects
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.effect.GardenHeroEffect
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SpellParticleOption
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class MailboxRenderer() : BlockEntityRenderer<MailboxBlockEntity, MailboxRenderState> {
    override fun createRenderState(): MailboxRenderState {
        return MailboxRenderState()
    }

    override fun extractRenderState(
        mailbox: MailboxBlockEntity,
        state: MailboxRenderState,
        partialTicks: Float,
        cameraPosition: Vec3,
        breakProgress: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        super.extractRenderState(mailbox, state, partialTicks, cameraPosition, breakProgress)
        // display particles on mail boxes when local player is the hero
        val player = Minecraft.getInstance().player ?: return
        state.level = player.level()
        state.tickCount = player.tickCount
        state.isClientPlayerHero = player.hasEffect(PazEffects.GARDEN_HERO)
        // Sprout Squad: rolled per mailbox, once every 5th tick. One flag on the renderer, which all mailboxes share,
        // let only one mailbox show particles, at a rate that followed the frame rate
        state.emitHeroParticle = false
        if (state.isClientPlayerHero && player.tickCount % 5 == 0 && mailbox.heroParticleTick != player.tickCount) {
            mailbox.heroParticleTick = player.tickCount
            state.emitHeroParticle = player.level().random.nextFloat() < .1f
        }
    }

    override fun submit(
        state: MailboxRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (state.emitHeroParticle) state.level?.let {
            val speed = 0.1
            val distance = 0.4
            val direction = Vec3(it.random.nextDouble() - 0.5, it.random.nextDouble() - 0.5, it.random.nextDouble() - 0.5).normalize()
            val xd = direction.scale(speed).x
            val yd = direction.scale(speed).y
            val zd = direction.scale(speed).z
            val x = state.blockPos.x + 0.5 + direction.scale(distance).x
            val y = state.blockPos.y + 0.33 + direction.scale(distance).y
            val z = state.blockPos.z + 0.5 + direction.scale(distance).z
            it.addParticle(SpellParticleOption.create(ParticleTypes.EFFECT, GardenHeroEffect.EFFECT_COLOR, 0f), x, y, z, xd, yd + .1, zd)
        }
    }
}

class MailboxRenderState : BlockEntityRenderState() {
    var isClientPlayerHero: Boolean = false
    var emitHeroParticle: Boolean = false
    var level: Level? = null
    var tickCount: Int = 0
}