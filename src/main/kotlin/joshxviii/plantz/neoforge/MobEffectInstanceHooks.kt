package joshxviii.plantz.neoforge

import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity

/**
 * Effect callbacks that receive the [MobEffectInstance], as provided by Fabric API.
 * Called from `joshxviii.plantz.mixin.neoforge.MobEffectInstanceMixin` and
 * `joshxviii.plantz.mixin.neoforge.LivingEntityEffectsRemovedMixin`.
 */
interface MobEffectInstanceHooks {
    /** Every time the effect is applied (new or refreshed). */
    fun onEffectStarted(effectInstance: MobEffectInstance, entity: LivingEntity) {}

    /** When the effect is newly added to the entity. */
    fun onEffectAdded(effectInstance: MobEffectInstance, entity: LivingEntity) {}

    /** When the effect is removed or expires. */
    fun onEffectRemoved(effectInstance: MobEffectInstance, entity: LivingEntity) {}
}
