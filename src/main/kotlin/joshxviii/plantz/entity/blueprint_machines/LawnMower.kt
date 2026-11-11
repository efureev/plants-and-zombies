package joshxviii.plantz.entity.blueprint_machines

import dev.efureev.sproutsquad.machines.LawnMowerDrive
import joshxviii.plantz.item.BlueprintItem
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class LawnMower(type: EntityType<out LawnMower>, level: Level) : LivingEntity(type, level) {
    companion object {
        /** True while the mower drives (synced for the "mow" animation). */
        val RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(LawnMower::class.java, EntityDataSerializers.BOOLEAN)
    }

    val mowAnimation: AnimationState = AnimationState()
    private val drive = LawnMowerDrive(this)

    var isRunning: Boolean
        get() = entityData.get(RUNNING)
        set(value) = entityData.set(RUNNING, value)

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(RUNNING, false)
    }

    override fun tick() {
        super.tick()
        if (level().isClientSide) {
            if (isRunning) mowAnimation.startIfStopped(tickCount) else mowAnimation.stop()
        } else drive.tick()
    }

    override fun getPickResult(): ItemStack = BlueprintItem.stackFor(this.type)

    // Only a player (or /kill and the void) can remove the mower; monsters cannot break the last line of defence.
    override fun hurtServer(
        level: ServerLevel,
        source: DamageSource,
        damage: Float
    ): Boolean {
        if (source.entity !is Player && !source.`is`(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false
        setRemoved(RemovalReason.KILLED)
        return true
    }

    // never pushed: idle it holds its spot, running it must keep its line
    override fun isPushable(): Boolean = false

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT
}
