package joshxviii.plantz.mixin;

import joshxviii.plantz.PlantHeadAttachment;
import joshxviii.plantz.UtilsKt;
import joshxviii.plantz.entity.plant.Plant;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Shadow
    protected abstract void tickPassenger(Entity vehicle, Entity entity);

    @Shadow
    @Final
    private EntityTickList entityTickList;

    @Shadow
    public abstract void tickNonPassenger(Entity entity);

    // Sprout Squad: this used to tick the wearer from the plant's tick, so the wearer ticked twice per
    // game tick (a mob wearing a plant moved and attacked twice as fast, a player's server timers ran
    // double). Now the plant only follows the wearer once the wearer has moved, whichever of the two
    // the level ticks first.
    @Inject(method = "tickNonPassenger", at = @At(value = "TAIL"))
    public void tickSyncPlant(Entity entity, CallbackInfo ci) {
        if (entity instanceof PlantHeadAttachment wearer && wearer.plantz$hasPlantOnHead()) {
            Plant plant = wearer.plantz$getPlant();
            if (plant != null) UtilsKt.positionPlant(entity, plant);
        }
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "HEAD"), cancellable = true)
    public void tickCancel(Entity entity, CallbackInfo ci) {
//        if (entity instanceof LivingEntity && ((PlantHeadAttachment) entity).plantz$hasPlantOnHead()) {
//            ci.cancel();
//        }
    }

}
