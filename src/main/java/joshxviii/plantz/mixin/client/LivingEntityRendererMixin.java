package joshxviii.plantz.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import joshxviii.plantz.item.RollerSkatesItem;
import joshxviii.plantz.renderer.*;
import joshxviii.plantz.PazEffectFlags;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static joshxviii.plantz.PazModels.*;

@Mixin(LivingEntityRenderer.class)
abstract public class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements RenderLayerParent<S, M> {

    @Shadow
    protected abstract boolean addLayer(RenderLayer<S, M> layer);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void plantz$addDuckyTubeLayer(EntityRendererProvider.Context context, M model, float shadow, CallbackInfo ci) {
        //TODO try to consolidate these into one render layer
        this.addLayer(new DuckyTubeRenderLayer<>(this));
        this.addLayer(new DyeVatRenderLayer<>(this));
        this.addLayer(new BodyBlockerRenderLayer<>(this));
        this.addLayer(new RollerSkateRenderLayer<>(this));
        this.addLayer(new PlantHeadAttachmentRenderLayer<>(this));

        this.addLayer(new SpecialEffectsLayer<>(this));
    }

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void checkForHypnoEffect(@Coerce Entity entity, @Coerce EntityRenderState entityRenderState, float v, CallbackInfo ci) {
        boolean hasHypno = ((PazEffectFlags) entity).plantz$getHypnoId();
        entityRenderState.setRenderData(HAS_HYPNO_KEY, hasHypno);
        boolean hasChill = ((PazEffectFlags) entity).plantz$getChillId();
        entityRenderState.setRenderData(HAS_CHILL_KEY, hasChill);
        boolean hasFrozen = ((PazEffectFlags) entity).plantz$getFrozenId();
        entityRenderState.setRenderData(HAS_FROZEN_KEY, hasFrozen);
        boolean hasButter = ((PazEffectFlags) entity).plantz$getButterId();
        entityRenderState.setRenderData(HAS_BUTTER_KEY, hasButter);
        Map<Integer, Integer> paintColors = ((PazEffectFlags) entity).plantz$getPaintedColors();
        entityRenderState.setRenderData(PAINT_COLORS_KEY, paintColors);

        if (entity instanceof LivingEntity living && entityRenderState instanceof LivingEntityRenderState livingState) {
            PlantHeadAttachmentRenderLayer.extractWornPlant(living, livingState, v, this.getModel());
            entityRenderState.setRenderData(IS_SKATING, RollerSkatesItem.Companion.isSkating(living));
        }

        // Sprout Squad: the pose is kept per entity. It was kept per renderer, i.e. per entity type, so every
        // frozen zombie of a type took the pose and the facing of the last one of that type drawn unfrozen.
        if (entityRenderState instanceof LivingEntityRenderState state) {
            if (hasFrozen) {
                float[] pose = plantz$frozenPoses.get(entity);
                if (pose == null) {
                    plantz$frozenPoses.put(entity, new float[] {
                        state.ageInTicks, state.walkAnimationPos, state.walkAnimationSpeed, state.bodyRot, state.yRot, state.xRot
                    });
                } else {
                    state.ageInTicks = pose[0];
                    state.walkAnimationPos = pose[1];
                    state.walkAnimationSpeed = pose[2];
                    state.bodyRot = pose[3];
                    state.yRot = pose[4];
                    state.xRot = pose[5];
                }
            } else {
                plantz$frozenPoses.remove(entity);
            }
        }
    }

    /**
     * Pose each frozen entity had when it froze; render thread only, weak keys drop unloaded entities. Keys are
     * compared by identity: Entity.equals compares ids, and a new entity with a reused id (another world) took the
     * stale pose of the old one.
     */
    @Unique
    private static final Map<Entity, float[]> plantz$frozenPoses = new com.google.common.collect.MapMaker().weakKeys().makeMap();

    @Unique
    private static final int PLANTZ_HYPNO_TINT = ARGB.opaque(0xD036FF);
    @Unique
    private static final int PLANTZ_FREEZE_TINT = ARGB.opaque(0x0098DC);
    @Unique
    private static final int PLANTZ_BUTTER_TINT = ARGB.opaque(0xdbdc8c);

    @ModifyExpressionValue(
        method = "submit*",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;multiply(II)I")
    )
    private int plantz$applyHypnoTint(int tintedColor, S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        AtomicInteger finalColor = new AtomicInteger(tintedColor);

        if (state.getRenderDataOrDefault(HAS_HYPNO_KEY, false)) {
            finalColor.set(ARGB.multiply(finalColor.get(), PLANTZ_HYPNO_TINT));
        }
        if (state.getRenderDataOrDefault(HAS_CHILL_KEY, false) || state.getRenderDataOrDefault(HAS_FROZEN_KEY, false)) {
            finalColor.set(ARGB.multiply(finalColor.get(), PLANTZ_FREEZE_TINT));
        }
        if (state.getRenderDataOrDefault(HAS_BUTTER_KEY, false)) {
            finalColor.set(ARGB.multiply(finalColor.get(), PLANTZ_BUTTER_TINT));
        }

        // multiple colors end up just looking black. not gonna use this for now.
//        Map<Integer, Integer> colors = state.getRenderDataOrDefault(PAINT_COLORS_KEY, new HashMap<>());
//        colors.forEach( (color, amplifier) -> {
//            if (color != -1) finalColor.set(ARGB.multiply(finalColor.get(), ARGB.opaque(color)));
//        });

        return finalColor.get();
    }
}
