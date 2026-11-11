package joshxviii.plantz.entity.blueprint_machines

import dev.efureev.sproutsquad.machines.PlayerMachines
import net.minecraft.world.entity.ai.attributes.Attributes
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.projectile.LaserBullet
import joshxviii.plantz.entity.zombie.ZombieRobot
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2

class ZombieTurret(type: EntityType<out ZombieTurret>, level: Level) : ZombieRobot(type, level) {

    override fun tick() {
        super.tick()

        if (actionTime>0) {
            actionAnimation.startIfStopped(tickCount)
            if (actionTime++>14) {
                actionAnimation.stop()
                actionTime=0
            }
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(1, ProjectileAttackGoal(
            usingEntity = this,
            velocity = 1.1,
            actionDelay = 9,
            inaccuracy = 5.2f,
            leadShots = false,
            soundEvent = PazSounds.ZOMBIE_TURRET_SHOOT,
            projectileFactory = {
                LaserBullet(
                    level(),
                    this,
                    spawnOffset = Vec2(0.2f, 0.4f),
                    damageType = if (PlayerMachines.isPlayerMachine(this)) PazDamageTypes.ENERGY else PazDamageTypes.ZOMBIE_ENERGY,
                    // a player's turret hits with its attack damage (2), hostile ones keep the weak 0.5 shots
                    damage = if (PlayerMachines.isPlayerMachine(this)) getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat() else 0.5f,
                )
            },
            usePredicate = { actionTime<=0 },
            actionStartEffect = { actionTime=1 },
        ))
    }

}