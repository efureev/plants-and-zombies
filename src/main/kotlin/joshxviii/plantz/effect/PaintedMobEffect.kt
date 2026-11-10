package joshxviii.plantz.effect

import joshxviii.plantz.neoforge.MobEffectInstanceHooks

import io.netty.buffer.ByteBuf
import joshxviii.plantz.PazEffects.PAINTED
import joshxviii.plantz.PazServerParticles
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.DyeColor
import org.spongepowered.asm.mixin.Unique

class PaintedMobEffect(
    category: MobEffectCategory,
    var paintColor: Int = DyeColor.WHITE.fireworkColor,
    private var random: RandomSource = RandomSource.create()
) : MobEffect(category, paintColor, PazServerParticles.PAINT_DRIP), MobEffectInstanceHooks {
    companion object {

        @JvmStatic
        fun getPaintEffects(mob: LivingEntity?, exclude: PaintedMobEffect? = null): List<MobEffectInstance> {
            val effects = PAINTED.entries
                .filter { (_, effect) -> !effect.value().equals(exclude) }
                .mapNotNull { (_, effect) -> mob?.getEffect(effect) }
            return effects
        }

        @JvmStatic
        fun getPaintColors(mob: LivingEntity?): Map<Int, Int> {
            val colors =
                PAINTED.entries
                    .mapNotNull { (_, effect) -> mob?.getEffect(effect) }
                    .associateBy( {(it.effect.value() as PaintedMobEffect).paintColor}, {it.amplifier})
            return colors
        }
    }

    override fun shouldApplyEffectTickThisTick(tickCount: Int, amplification: Int): Boolean {
        return true
    }

    override fun applyEffectTick(serverLevel: ServerLevel, mob: LivingEntity, amplification: Int): Boolean {
        if (mob.isEyeInFluid(FluidTags.WATER)) return false
        return super.applyEffectTick(serverLevel, mob, amplification)
    }

    override fun onEffectAdded(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<MobEffectInstanceHooks>.onEffectAdded(effectInstance, entity)
    }

    override fun onEffectStarted(effectInstance: MobEffectInstance, entity: LivingEntity) {
        random = RandomSource.create(entity.level().gameTime)
        super<MobEffectInstanceHooks>.onEffectStarted(effectInstance, entity)
    }

    override fun onEffectRemoved(effectInstance: MobEffectInstance, entity: LivingEntity) {
        super<MobEffectInstanceHooks>.onEffectRemoved(effectInstance, entity)
    }

    fun getRandomness(): RandomSource {
        return RandomSource.create(random.hashCode().toLong())
    }

    override fun createParticleOptions(mobEffectInstance: MobEffectInstance): ParticleOptions {
        return super.createParticleOptions(mobEffectInstance)
    }

}