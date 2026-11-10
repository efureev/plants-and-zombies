package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.plant.Squash
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.sin

/**
 * Squash attack, as in the original game: when a target comes close, the squash winds up, leaps over it
 * and slams down, killing whatever is under it, then disappears (single use).
 *
 * The timing follows `SquashAnimation.action` (4 s): wind-up until ~2 s, stretch at 3.52 s, flatten at 3.64 s.
 * Players are never squashed, even by a wild squash.
 */
class SquashAttackGoal(private val squash: Squash) : Goal() {

    companion object {
        const val LEAP_TICK = 40
        const val HOVER_TICK = 62
        const val SLAM_TICK = 70
        const val IMPACT_TICK = 73
        const val END_TICK = 80

        const val TRIGGER_RANGE = 3.0
        const val HOVER_HEIGHT = 2.0
        const val DAMAGE = 1000f
    }

    private var ticks = 0
    private var target: LivingEntity? = null
    private var start: Vec3 = Vec3.ZERO
    private var aim: Vec3 = Vec3.ZERO

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (squash.isAsleep || squash.isGrowingSeeds || squash.cooldown != 0) return false
        val target = squash.target ?: return false
        return canSquash(target) && inRange(target)
    }

    override fun canContinueToUse(): Boolean = ticks < END_TICK && squash.isAlive
    override fun isInterruptable(): Boolean = false
    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        ticks = 0
        target = squash.target
        aim = target?.position() ?: squash.position()
        squash.navigation.stop()
        squash.cooldown = -1 // plant ACTION state: plays SquashAnimation.action
    }

    override fun stop() {
        squash.leaping = false
        squash.isNoGravity = false
        if (squash.isAlive && squash.cooldown < 0) squash.cooldown = 20
        target = null
    }

    override fun tick() {
        ticks++
        val current = target
        if (current != null && current.isAlive && ticks <= HOVER_TICK) aim = current.position()
        if (current != null && current.isAlive) squash.lookControl.setLookAt(current, 30f, 30f)

        when {
            ticks == LEAP_TICK -> {
                start = squash.position()
                squash.leaping = true
                squash.isNoGravity = true
                squash.playSound(SoundEvents.SLIME_JUMP, 1.0f, 0.6f)
            }
            ticks in LEAP_TICK + 1..HOVER_TICK -> {
                val progress = (ticks - LEAP_TICK).toDouble() / (HOVER_TICK - LEAP_TICK)
                val arc = sin(progress * Math.PI) * 0.75
                moveTo(Vec3(
                    Mth.lerp(progress, start.x, aim.x),
                    Mth.lerp(progress, start.y, aim.y + HOVER_HEIGHT) + arc,
                    Mth.lerp(progress, start.z, aim.z),
                ))
            }
            ticks in HOVER_TICK + 1..SLAM_TICK -> moveTo(aim.add(0.0, HOVER_HEIGHT, 0.0))
            ticks in SLAM_TICK + 1..IMPACT_TICK -> {
                val progress = (ticks - SLAM_TICK).toDouble() / (IMPACT_TICK - SLAM_TICK)
                moveTo(aim.add(0.0, HOVER_HEIGHT * (1 - progress), 0.0))
            }
        }
        if (ticks == IMPACT_TICK) impact()
        if (ticks == END_TICK) vanish()
    }

    private fun moveTo(position: Vec3) {
        squash.deltaMovement = position.subtract(squash.position())
    }

    private fun impact() {
        val level = squash.level() as? ServerLevel ?: return
        squash.deltaMovement = Vec3.ZERO
        squash.isNoGravity = false

        val source = squash.damageSources().source(
            PazDamageTypes.PLANT, squash,
            if (PazConfig.PLAYER_CREDIT_FOR_PLANT_KILLS) squash.rootOwner else squash
        )
        val area = AABB.ofSize(Vec3(aim.x, aim.y + 0.75, aim.z), 1.6, 2.0, 1.6)
        for (victim in level.getEntitiesOfClass(LivingEntity::class.java, area) { it !== squash && it.isAlive && canSquash(it) }) {
            victim.hurtServer(level, source, DAMAGE)
        }

        squash.playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.0f, 0.8f)
        val ground = level.getBlockState(BlockPos.containing(aim).below())
        if (!ground.isAir) level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, ground),
            aim.x, aim.y + 0.1, aim.z, 30, 0.6, 0.1, 0.6, 0.15
        )
    }

    private fun vanish() {
        val level = squash.level() as? ServerLevel ?: return
        level.broadcastEntityEvent(squash, EntityEvent.POOF)
        squash.restorePumpkinFromArmor()
        squash.discard()
    }

    private fun canSquash(entity: LivingEntity): Boolean =
        entity !is Plant && entity !is Player && squash.canAttack(entity)

    private fun inRange(entity: LivingEntity): Boolean =
        abs(entity.y - squash.y) <= 1.5 && entity.position().subtract(squash.position()).horizontalDistance() <= TRIGGER_RANGE
}
