package joshxviii.plantz.mixin.client;

import joshxviii.plantz.PazNetwork;
import joshxviii.plantz.RaidMusicManager;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Josh
 */
@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "reload", at = @At("TAIL"))
    public void restartRaidMusic(CallbackInfo ci) {
        if (PazNetwork.ZombieRaidClientCache.INSTANCE.isActive()) {
            RaidMusicManager.INSTANCE.restart();
        }
    }
}
