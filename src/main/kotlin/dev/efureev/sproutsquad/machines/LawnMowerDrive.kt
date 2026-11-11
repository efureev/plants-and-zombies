package dev.efureev.sproutsquad.machines

import joshxviii.plantz.entity.blueprint_machines.LawnMower
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * The lawn mower as the last line of defence, like in the original game: it waits until a monster
 * comes right up to its front, then drives straight ahead, mowing down every monster in its way, and is used up.
 * Stops at a wall or after [MAX_DISTANCE] blocks. Players, plants and pets are never hit.
 *
 * Nothing about the run is saved: a mower unloaded mid-run is back to waiting after a reload.
 */
class LawnMowerDrive(private val mower: LawnMower) {
    companion object {
        const val SPEED = 0.4
        const val MAX_DISTANCE = 40.0
        const val MAX_TICKS = 200
        const val SCAN_PERIOD = 5
        const val DAMAGE = 1000f
    }

    private var ticks = 0
    private var start: Vec3 = Vec3.ZERO
    private val hit = mutableSetOf<Int>()

    /** Server side only. */
    fun tick() {
        val level = mower.level() as? ServerLevel ?: return
        if (!mower.isRunning) {
            if (mower.tickCount % SCAN_PERIOD == 0 && victimsAhead(level).isNotEmpty()) startRun()
            return
        }

        ticks++
        val forward = Vec3.directionFromRotation(0f, mower.yRot).scale(SPEED)
        mower.deltaMovement = Vec3(forward.x, mower.deltaMovement.y, forward.z)

        for (victim in victims(level, 0.2)) {
            if (!hit.add(victim.id)) continue
            victim.hurtServer(level, mower.damageSources().mobAttack(mower), if (PlayerMachines.isBoss(victim)) PlayerMachines.BOSS_DAMAGE else DAMAGE)
            mower.playSound(SoundEvents.SHEEP_SHEAR, 1.0f, 0.7f)
        }

        val blocked = ticks > 2 && mower.horizontalCollision
        if (blocked || ticks >= MAX_TICKS || mower.position().distanceTo(start) >= MAX_DISTANCE) finish(level)
    }

    private fun startRun() {
        mower.isRunning = true
        ticks = 0
        start = mower.position()
        hit.clear()
        mower.playSound(SoundEvents.MINECART_RIDING, 1.0f, 1.4f)
    }

    private fun finish(level: ServerLevel) {
        mower.isRunning = false
        level.broadcastEntityEvent(mower, EntityEvent.POOF)
        mower.playSound(SoundEvents.COPPER_BULB_TURN_OFF, 1.0f, 0.6f)
        mower.discard()
    }

    /** A monster right in front of the mower starts the run: one coming from behind would not be hit. */
    private fun victimsAhead(level: ServerLevel): List<LivingEntity> {
        val ahead = Vec3.directionFromRotation(0f, mower.yRot).scale(0.7)
        return enemiesIn(level, mower.boundingBox.move(ahead).inflate(0.1, 0.25, 0.1))
    }

    private fun victims(level: ServerLevel, reach: Double): List<LivingEntity> =
        enemiesIn(level, mower.boundingBox.inflate(reach, 0.25, reach))

    private fun enemiesIn(level: ServerLevel, area: AABB): List<LivingEntity> =
        level.getEntitiesOfClass(LivingEntity::class.java, area) { it !== mower && PlayerMachines.isEnemyOfPlayers(it) }
}
