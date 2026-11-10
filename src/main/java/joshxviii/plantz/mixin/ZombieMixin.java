package joshxviii.plantz.mixin;

import joshxviii.plantz.*;
import joshxviii.plantz.entity.zombie.PazZombie;
import joshxviii.plantz.raid.ZombieRaider;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import joshxviii.plantz.neoforge.PazSyncedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * @author Josh
 */
@Mixin(Zombie.class)
public class ZombieMixin implements ZombieRaider {


    @Override
    public boolean plantz$getIsFromRaid() {
        return PazSyncedData.FROM_RAID.get(((Entity) (Object) this));
    }

    @Override
    public void plantz$setIsFromRaid(boolean value) {
        PazSyncedData.FROM_RAID.set(((Entity) (Object) this), value);
    }

    @Override
    public boolean plantz$getAddsHealthToRaid() {
        return PazSyncedData.ADDS_HEALTH_TO_RAID.get(((Entity) (Object) this));
    }

    @Override
    public void plantz$setAddsHealthToRaid(boolean value) {
        PazSyncedData.ADDS_HEALTH_TO_RAID.set(((Entity) (Object) this), value);
    }

    @Override
    public void plantz$setIsLeader(boolean value) {
        PazSyncedData.RAID_LEADER.set(((Entity) (Object) this), value);
    }

    @Override
    public boolean plantz$getIsLeader() {
        return PazSyncedData.RAID_LEADER.get(((Entity) (Object) this));
    }

    @Override
    public boolean plantz$getIsBoss() {
        return PazSyncedData.RAID_BOSS.get(((Entity) (Object) this));
    }

    @Override
    public void plantz$setIsBoss(boolean value) {
        PazSyncedData.RAID_BOSS.set(((Entity) (Object) this), value);
    }

    @Override
    public boolean plantz$getIgnoreFlag() {
        return PazSyncedData.IGNORE_FLAG.get(((Entity) (Object) this));
    }

    @Override
    public void plantz$setIgnoreFlag(boolean value) {
        PazSyncedData.IGNORE_FLAG.set(((Entity) (Object) this), value);
    }


    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveCustomFlags(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("sproutsquad:IsFromRaid", this.plantz$getIsFromRaid());
        output.putBoolean("sproutsquad:AddsHealthToRaid", this.plantz$getAddsHealthToRaid());
        output.putBoolean("sproutsquad:IsLeader", this.plantz$getIsLeader());
        output.putBoolean("sproutsquad:IsBoss", this.plantz$getIsBoss());
        output.putBoolean("sproutsquad:IgnoreFlag", this.plantz$getIgnoreFlag());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadCustomFlags(ValueInput input, CallbackInfo ci) {
        this.plantz$setIsFromRaid(input.getBooleanOr("sproutsquad:IsFromRaid", false));
        this.plantz$setAddsHealthToRaid(input.getBooleanOr("sproutsquad:AddsHealthToRaid", true));
        this.plantz$setIsLeader(input.getBooleanOr("sproutsquad:IsLeader", false));
        this.plantz$setIsBoss(input.getBooleanOr("sproutsquad:IsBoss", false));
        this.plantz$setIgnoreFlag(input.getBooleanOr("sproutsquad:IgnoreFlag", false));
    }

    @ModifyArgs(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Monster;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void updateDamage(Args args) {
        DamageSource source = args.get(1);
        float damage = args.get(2);
        if (source.is(PazDamageTypes.PLANT_EXPLODE)) args.set(2, damage*3f);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    public void zombieAoeIgnore(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        Zombie entity = (Zombie) (Object) this;
        if (source.is(PazDamageTypes.ZOMBIE_SMASH) && !entity.hasEffect(PazEffects.HYPNOTIZE)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }


    @Unique
    private static final float BONUS_LEADER_CHANCE = 0.0015F;
    @Unique
    private static final int LEADER_GROUP_SIZE = 3;

    @Inject( method = "finalizeSpawn", at = @At("RETURN"))
    public void checkForLeader(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, SpawnGroupData groupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (spawnReason == EntitySpawnReason.REINFORCEMENT || spawnReason == EntitySpawnReason.EVENT) return;

        Zombie entity = (Zombie) (Object) this;
        if (!entity.is(PazTags.EntityTypes.CAN_BE_LEADER)) {
            PazZombie.Companion.removeLeaderModifiers(entity);
            return;
        }

        boolean isBornLeader = PazZombie.Companion.isBornLeader(entity);
        boolean bonusPromotionChance = level.getRandom().nextFloat() < BONUS_LEADER_CHANCE;

        if (isBornLeader || bonusPromotionChance) promoteToLeader(entity, spawnReason);
    }

    @Unique
    private void promoteToLeader(Zombie entity, EntitySpawnReason spawnReason) {
        entity.setItemSlot(EquipmentSlot.OFFHAND, PazBlocks.BRAINZ_FLAG_BLOCK.asItem().getDefaultInstance());
        entity.setDropChance(EquipmentSlot.OFFHAND, 1.0F);

        if (spawnReason == EntitySpawnReason.NATURAL) PazZombie.Companion.spawnZombieGroup(entity, LEADER_GROUP_SIZE);
    }
}
