package joshxviii.plantz.mixin.neoforge;

import joshxviii.plantz.neoforge.MobEffectInstanceHooks;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forwards effect start/add callbacks with the effect instance (Fabric API behaviour). */
@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
    @Shadow
    public abstract Holder<MobEffect> getEffect();

    @Inject(method = "onEffectStarted", at = @At("TAIL"))
    private void plantz$onEffectStarted(LivingEntity mob, CallbackInfo ci) {
        if (this.getEffect().value() instanceof MobEffectInstanceHooks hooks) {
            hooks.onEffectStarted((MobEffectInstance) (Object) this, mob);
        }
    }

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void plantz$onEffectAdded(LivingEntity mob, CallbackInfo ci) {
        if (this.getEffect().value() instanceof MobEffectInstanceHooks hooks) {
            hooks.onEffectAdded((MobEffectInstance) (Object) this, mob);
        }
    }
}
