package joshxviii.plantz.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import joshxviii.plantz.*;
import joshxviii.plantz.effect.PaintedMobEffect;
import joshxviii.plantz.entity.plant.Plant;
import joshxviii.plantz.item.DuckTubeItem;
import joshxviii.plantz.raid.CompletedWave;
import joshxviii.plantz.raid.GardenHeroRewards;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import joshxviii.plantz.PazEffectFlags;
import joshxviii.plantz.neoforge.PazSyncedData;

@Mixin(LivingEntity.class)
abstract public class LivingEntityMixin implements PlantHeadAttachment, GardenHeroRewards, PazEffectFlags {



    @Shadow
    public abstract boolean hasEffect(Holder<MobEffect> effect);

    @Shadow
    public abstract @org.jspecify.annotations.Nullable MobEffectInstance getEffect(Holder<MobEffect> effect);

    @Unique
    private CompoundTag plantData = new CompoundTag();

    @Unique
    private List<CompletedWave> completedRaidWaveList = List.of();
    @Unique
    private @Nullable Plant plantEntity = null;

    @Override
    public @Nullable Plant plantz$getPlant() {
        return plantEntity;
    }

    @Override
    public void plantz$setPlant(@Nullable Plant value) {
        plantEntity = value;
        PazSyncedData.HEAD_PLANT_ID.set(((Entity) (Object) this), value == null ? -1 : value.getId());
    }

    @Override
    public int plantz$getPlantId() {
        return PazSyncedData.HEAD_PLANT_ID.get(((Entity) (Object) this));
    }

    @Override
    public @NotNull CompoundTag plantz$getPlantData() {
        return plantData;
    }

    @Override
    public void plantz$setPlantData(@NotNull CompoundTag value) {
        plantData = value;
    }

    @Override
    public boolean plantz$hasPlantOnHead() {
        return plantEntity != null &&  plantEntity.isAlive() && !plantEntity.isRemoved();
    }

    @Override
    public @NotNull List<CompletedWave> plantz$getWaveList() {
        return completedRaidWaveList;
    }

    @Override
    public void plantz$setWaveList(@NotNull List<CompletedWave> value) {
        completedRaidWaveList = value;
    }

    @Override
    public boolean plantz$getHypnoId() {
        return PazSyncedData.HYPNOTIZED.get(((Entity) (Object) this));
    }
    @Override
    public boolean plantz$getChillId() {
        return PazSyncedData.CHILLED.get(((Entity) (Object) this));
    }
    @Override
    public boolean plantz$getFrozenId() {
        return PazSyncedData.FROZEN.get(((Entity) (Object) this));
    }
    @Override
    public boolean plantz$getButterId() {
        return PazSyncedData.BUTTERED.get(((Entity) (Object) this));
    }
    @Override
    public Map<Integer, Integer> plantz$getPaintedColors() {
        return PazSyncedData.PAINTED_COLORS.get(((Entity) (Object) this));
    }

    @Unique
    private boolean prevFloatTag = false;
    @Unique
    private float prevWaterMalus = 0f;

    @Inject(method = "onEquipItem", at = @At("TAIL"))
    private void plantz$checkFloatTag(EquipmentSlot slot, ItemStack oldStack, ItemStack stack, CallbackInfo ci) {
        if ((LivingEntity) (Object) this instanceof PathfinderMob mob) {
            if (stack.is(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
                prevFloatTag = mob.getNavigation().canFloat();
                prevWaterMalus = mob.getPathfindingMalus(PathType.WATER);
                mob.getNavigation().setCanFloat(true);
            }
            else if (oldStack.is(PazItems.DUCKY_TUBE) && slot == EquipmentSlot.LEGS) {
                mob.getNavigation().setCanFloat(prevFloatTag);
                mob.setPathfindingMalus(PathType.WATER, 0.0F);
            }
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void plantz$applyDuckyTubeBuoyancy(CallbackInfo ci) {
        DuckTubeItem.Companion.floatOnWater((LivingEntity) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveCustomFlags(ValueOutput output, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        output.putBoolean("sproutsquad:IsHypnotized", PazSyncedData.HYPNOTIZED.get(self));
        output.putBoolean("sproutsquad:IsChilled", PazSyncedData.CHILLED.get(self));
        output.putBoolean("sproutsquad:IsFrozen", PazSyncedData.FROZEN.get(self));
        output.putBoolean("sproutsquad:IsButtered", PazSyncedData.BUTTERED.get(self));
        output.store("sproutsquad:PaintedColor", Codec.unboundedMap(Codec.INT, Codec.INT), PazSyncedData.PAINTED_COLORS.get(self));
        if (!this.plantz$getPlantData().isEmpty()) {
            output.store("sproutsquad:AttachedPlant", CompoundTag.CODEC, this.plantz$getPlantData());
        }
        if (self instanceof ServerPlayer) output.store("sproutsquad:CompletedWaveList", Codec.list(CompletedWave.Companion.getCODEC()), this.plantz$getWaveList());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadCustomFlags(ValueInput input, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        PazSyncedData.HYPNOTIZED.set(self, input.getBooleanOr("sproutsquad:IsHypnotized", false));
        PazSyncedData.CHILLED.set(self, input.getBooleanOr("sproutsquad:IsChilled", false));
        PazSyncedData.FROZEN.set(self, input.getBooleanOr("sproutsquad:IsFrozen", false));
        PazSyncedData.BUTTERED.set(self, input.getBooleanOr("sproutsquad:IsButtered", false));
        PazSyncedData.PAINTED_COLORS.set(self, input.read("sproutsquad:PaintedColor", Codec.unboundedMap(Codec.INT, Codec.INT)).orElseGet(HashMap::new));
        plantz$setPlantData(input.read("sproutsquad:AttachedPlant", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        if (self instanceof ServerPlayer) {
            plantz$setWaveList(input.read("sproutsquad:CompletedWaveList", Codec.list(CompletedWave.Companion.getCODEC())).orElseGet(List::of));
        }

        if (self instanceof PathfinderMob mob) {
            prevFloatTag = mob.getNavigation().canFloat();
            prevWaterMalus = mob.getPathfindingMalus(PathType.WATER);
            if (mob.getItemBySlot(EquipmentSlot.LEGS).is(PazItems.DUCKY_TUBE)) {
                mob.getNavigation().setCanFloat(true);
                mob.setPathfindingMalus(PathType.WATER, 0.0F);
            }
        }
    }

    @Inject(method = "onEffectAdded", at = @At(value = "TAIL"))
    public void onHypnoAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        updateEffects();
    }
    @Inject(method = "onEffectsRemoved", at = @At(value = "TAIL"))
    public void onHypnoRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        updateEffects();
    }
    @Unique
    public void updateEffects() {
        var self = (LivingEntity) (Object) this;

        PazSyncedData.HYPNOTIZED.set(self, this.hasEffect(PazEffects.HYPNOTIZE));
        PazSyncedData.CHILLED.set(self, this.hasEffect(PazEffects.CHILLED));
        PazSyncedData.FROZEN.set(self, this.hasEffect(PazEffects.FROZEN));
        PazSyncedData.BUTTERED.set(self, this.hasEffect(PazEffects.BUTTERED));
        PazSyncedData.PAINTED_COLORS.set(self, PaintedMobEffect.getPaintColors(self));
    }

    @Inject(method = "canBeAffected", at = @At(value = "RETURN"), cancellable = true)
    public void immuneToHypnosisOrFreeze(MobEffectInstance newEffect, CallbackInfoReturnable<Boolean> cir) {
        var entity = (LivingEntity) (Object) this;
        if (newEffect.is(PazEffects.HYPNOTIZE)) {
            cir.setReturnValue(!entity.is(PazTags.EntityTypes.CANNOT_HYPNOTIZE));
        }
        if (newEffect.is(PazEffects.CHILLED) || newEffect.is(PazEffects.FROZEN)) {
            cir.setReturnValue(entity.canFreeze());
        }
        updateEffects();
    }

    @Inject(method = "canAttack", at = @At(value = "RETURN"), cancellable = true)
    public void noStopAttackingEffects(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (this.hasEffect(PazEffects.FROZEN) || this.hasEffect(PazEffects.BUTTERED)) {
            cir.setReturnValue(false);
        }
        if (this.hasEffect(PazEffects.HYPNOTIZE) && (target instanceof Plant || target instanceof Player || target.hasEffect(PazEffects.HYPNOTIZE))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    public void updateEffect(CallbackInfo ci) {
        updateEffects();
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void ownerIgnorePlantAttacks(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        var sourceEntity = source.getEntity();
        var directEntity = source.getDirectEntity();
        if (source.is(DamageTypes.LIGHTNING_BOLT)) self.addEffect(new MobEffectInstance(PazEffects.ELECTRIFIED, 300, 1));
        if (source.is(PazTags.DamageTypes.IS_ELECTRIC) && self.is(PazTags.EntityTypes.IMMUNE_TO_ELECTRICITY)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        if (sourceEntity instanceof Plant plant) {
            if (plant.hasSameOwner(self)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
        else if (directEntity instanceof Plant plant) {
            if (plant.hasSameOwner(self)) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getItemBlockingWith", at = @At("RETURN"), cancellable = true)
    private void plantz$wornBodyBlockerAlwaysBlocks(CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue() != null) return;
        var chest = ((LivingEntity) (Object) this).getItemBySlot(EquipmentSlot.CHEST);
        if (chest.has(PazComponents.WORN_BODY_BLOCKER) && chest.has(DataComponents.BLOCKS_ATTACKS)) cir.setReturnValue(chest);
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
    private void plantz$applyWornBodyBlockerAfterHeldItem(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        var self = (LivingEntity) (Object) this;
        ItemStack chest = self.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.has(PazComponents.WORN_BODY_BLOCKER)) return;
        if (self.getItemBlockingWith() == chest) return;

        var blocksAttacks = chest.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) return;
        if (blocksAttacks.bypassedBy().map(t -> t.contains(source.typeHolder())).orElse(false)) return;
        if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow && arrow.getPierceLevel() > 0) return;

        float alreadyBlocked = cir.getReturnValue();
        float remaining = damage - alreadyBlocked;
        if (remaining <= 0.0F) return;

        double angle;
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition != null) {
            Vec3 view = self.calculateViewVector(0.0F, self.getYHeadRot());
            Vec3 to = sourcePosition.subtract(self.position());
            to = new Vec3(to.x, 0.0, to.z).normalize();
            angle = Math.acos(Math.max(-1.0, Math.min(1.0, to.dot(view))));
        } else {
            angle = Math.PI;
        }

        float blocked = blocksAttacks.resolveBlockedDamage(source, remaining, angle);
        if (blocked <= 0.0F) return;
        blocksAttacks.hurtBlockingItem(self.level(), chest, self, net.minecraft.world.InteractionHand.MAIN_HAND, blocked);
        cir.setReturnValue(alreadyBlocked + blocked);
    }

    @ModifyVariable(method = "hurtServer", at = @At("STORE"), name = "itemInUse")
    private ItemStack plantz$useWornBlockerForBlockFeedback(ItemStack itemInUse) {
        if (itemInUse.has(DataComponents.BLOCKS_ATTACKS)) return itemInUse;
        ItemStack chest = ((LivingEntity) (Object) this).getItemBySlot(EquipmentSlot.CHEST);
        return chest.has(PazComponents.WORN_BODY_BLOCKER) ? chest : itemInUse;
    }

    @WrapOperation(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;playHurtSound(Lnet/minecraft/world/damagesource/DamageSource;)V"
        )
    )
    private void plantz$suppressHurtSoundWhenFullyBlocked(
        LivingEntity entity,
        DamageSource source,
        Operation<Void> original,
        @Local(name = "damage", argsOnly = true) float damage,
        @Local(name = "blocked") boolean blocked
    ) {
        if (!blocked || damage > 0.0F) original.call(entity, source);
    }

    @Inject(method = "blockedByItem", at = @At("HEAD"), cancellable = true)
    private void plantz$preventKnockbackWhileBlocking(LivingEntity defender, DamageSource source, float damage, boolean fullyBlocked, CallbackInfo ci) {
        ci.cancel();
    }

}
