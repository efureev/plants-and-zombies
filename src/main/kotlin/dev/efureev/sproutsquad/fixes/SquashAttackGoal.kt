package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.plant.Squash
import joshxviii.plantz.raid.isBoss
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.common.Tags
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.sin

/**
 * Squash attack, as in the original game: when a target comes close, the squash winds up, leaps over it
 * and slams down, killing whatever is under it, then disappears (single use).
 *
 * The timing follows `SquashAnimation.action` (4 s): wind-up until ~2 s, stretch at 3.52 s, flatten at 3.64 s.
 * Only zombies (and, for a tame squash, other monsters) are squashed: never players, plants,
 * villagers or pets standing next to the target. Bosses take [BOSS_DAMAGE] instead of a one-shot.
 * If the target dies or walks away during the wind-up, the squash stays and waits for the next one.
 * Nothing about the leap is saved: a squash unloaded mid-air falls back down on reload.
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
        const val BOSS_DAMAGE = 40f

        /** Who a squash targets and squashes; shared with the target goal in [Squash]. */
        fun isSquashTarget(squash: Squash, entity: LivingEntity): Boolean =
            entity !is Plant && entity !is Player && (entity is Zombie || (entity is Enemy && squash.isTame))
    }

    private var ticks = 0
    private var target: LivingEntity? = null
    private var origin: Vec3 = Vec3.ZERO
    private var start: Vec3 = Vec3.ZERO
    private var aim: Vec3 = Vec3.ZERO

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        // a worn or carried squash is held in place and must not hit anything
        if (squash.isAsleep || squash.isGrowingSeeds || squash.cooldown != 0 || squash.isPassenger || squash.isAttached()) return false
        val target = squash.target ?: return false
        return canSquash(target) && inRange(target, TRIGGER_RANGE)
    }

    /** Before the leap the attack is cancelled if the target is gone; after it, it always finishes. */
    override fun canContinueToUse(): Boolean {
        if (!squash.isAlive || ticks >= END_TICK) return false
        if (ticks >= LEAP_TICK) return true
        val current = target
        return current != null && current.isAlive && canSquash(current) && inRange(current, TRIGGER_RANGE + 1.0)
    }
    override fun isInterruptable(): Boolean = false
    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        ticks = 0
        target = squash.target
        origin = squash.position()
        aim = target?.position() ?: origin
        squash.navigation.stop()
        squash.cooldown = -1 // plant ACTION state: plays SquashAnimation.action
    }

    override fun stop() {
        if (squash.isAlive && squash.cooldown < 0) squash.cooldown = 20
        target = null
    }

    override fun tick() {
        ticks++
        val current = target
        // follow the target until the slam, but never further than the trigger range from where the squash stood
        if (current != null && current.isAlive && ticks <= HOVER_TICK &&
            current.position().subtract(origin).horizontalDistance() <= TRIGGER_RANGE + 1.5) aim = current.position()
        if (current != null && current.isAlive) squash.lookControl.setLookAt(current, 30f, 30f)

        when {
            ticks == LEAP_TICK -> {
                start = squash.position()
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

        val source = squash.damageSources().source(
            PazDamageTypes.PLANT, squash,
            if (PazConfig.PLAYER_CREDIT_FOR_PLANT_KILLS) squash.rootOwner else squash
        )
        val area = AABB.ofSize(Vec3(aim.x, aim.y + 0.75, aim.z), 1.6, 2.0, 1.6)
        for (victim in level.getEntitiesOfClass(LivingEntity::class.java, area) { it !== squash && it.isAlive && canSquash(it) }) {
            val boss = victim.type.builtInRegistryHolder().`is`(Tags.EntityTypes.BOSSES) || (victim is Zombie && victim.isBoss())
            victim.hurtServer(level, source, if (boss) BOSS_DAMAGE else DAMAGE)
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
        isSquashTarget(squash, entity) && squash.canAttack(entity)

    private fun inRange(entity: LivingEntity, range: Double): Boolean =
        abs(entity.y - squash.y) <= 1.5 && entity.position().subtract(squash.position()).horizontalDistance() <= range
}
