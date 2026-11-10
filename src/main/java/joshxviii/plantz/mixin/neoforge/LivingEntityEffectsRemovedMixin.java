package joshxviii.plantz.mixin.neoforge;

import joshxviii.plantz.neoforge.MobEffectInstanceHooks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/** Forwards effect removal/expiry with the effect instance (Fabric API behaviour). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectsRemovedMixin {
    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void plantz$onEffectsRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect().value() instanceof MobEffectInstanceHooks hooks) {
                hooks.onEffectRemoved(effect, (LivingEntity) (Object) this);
            }
        }
    }
}
