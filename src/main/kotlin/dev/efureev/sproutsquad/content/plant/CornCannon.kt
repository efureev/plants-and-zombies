package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.projectile.CornCob
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2

/**
 * Corn Cannon: heavy artillery. A long husk lying on a rosette of leaves fires its corn cob as a rocket
 * in a high arc; the cob blows up and hurts only what the cannon would shoot at. A new cob grows in 3 s.
 * It does not see zombies within [MIN_RANGE] blocks. Does not sleep. See docs/en/characters/corn_cannon.md.
 */
class CornCannon(type: EntityType<out Plant>, level: Level) : Plant(type, level), SquadPlantLook {
    companion object {
        const val MAX_HEALTH = 90.0
        const val SUN_COST = 14
        const val RANGE = 40.0

        /** Zombies closer than this are not seen: no shot, and a target that comes this close is dropped. */
        const val MIN_RANGE = 5.0

        /** Ticks from the start of the action to the launch: the nose is up about 0.5 s in. */
        const val ACTION_DELAY = 11

        /** Cooldown after a shot, while the new cob grows: a shot every COOLDOWN + ACTION_DELAY - 1 = 70 ticks. */
        const val COOLDOWN = 60

        /** How long the eyes squint from the start of a shot: the aim and the launch, until the kick back settles. */
        const val AIM_MILLIS = 700L

        const val ENTITY_LIMIT = 10
        const val EYE_HEIGHT = 0.6f

        /**
         * Where the cob leaves the husk, as a [CornCob] spawn offset from the eyes: forward and up, about where
         * the raised mouth is and still inside the cannon's own block (a cob that starts in a wall right in
         * front vanishes on its first tick).
         */
        val COB_SPAWN = Vec2(0.3f, 0.15f)
    }

    override fun sleepsDuringNight(): Boolean = false

    /** What it shoots at, and so what its cob may hurt: zombies; when tamed, other monsters too, never creepers or plants. */
    fun isValidTarget(target: LivingEntity): Boolean =
        target !is Plant && target !is Creeper && (target is Zombie || (target is Enemy && isTame))

    fun inDeadZone(target: Entity): Boolean = distanceToSqr(target) < MIN_RANGE * MIN_RANGE

    /** `aim` (squinting) for the first [AIM_MILLIS] of a shot, plain otherwise and while dying (the renderer adds `sleep` then). */
    fun faceSuffix(actionMillis: Long?): String = when {
        deathTime > 0 -> ""
        actionMillis != null && actionMillis < AIM_MILLIS -> "aim"
        else -> ""
    }

    override fun lookSuffix(actionMillis: Long?): String? = faceSuffix(actionMillis).ifEmpty { null }

    /**
     * No retaliation or owner goals: they would pick players, animals or villagers, which the cob passes
     * through. Whatever hurts it and is a valid target is found by the nearest-target goal anyway.
     */
    override fun attackGoals() {}

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(2, CornCannonAttackGoal(this))
        targetSelector.addGoal(4, CornCannonTargetGoal(this))
    }
}

/**
 * The nearest valid target outside the dead zone, seen or not: the cob flies in a high arc, and the eyes sit
 * only 0.6 blocks up, so a one-block wall in front would otherwise hide every zombie behind it.
 */
class CornCannonTargetGoal(cannon: CornCannon) : NearestAttackableTargetGoal<LivingEntity>(
    cannon, LivingEntity::class.java, 5, false, false,
    { target, _ -> cannon.isValidTarget(target) && !cannon.inDeadZone(target) },
) {
    init {
        targetConditions.ignoreLineOfSight()
    }
}

/** Launches one [CornCob] in a high arc; drops a target that is not valid or has come into the dead zone. */
class CornCannonAttackGoal(private val cannon: CornCannon) : ProjectileAttackGoal(
    usingEntity = cannon,
    cooldownTime = CornCannon.COOLDOWN,
    actionDelay = CornCannon.ACTION_DELAY,
    projectileFactory = { CornCob(cannon.level(), cannon) },
    velocity = 1.1,
    useHighArc = true,
    soundEvent = null,
) {
    private fun dropUnseenTarget(): Boolean {
        val target = cannon.target ?: return true
        if (cannon.isValidTarget(target) && !cannon.inDeadZone(target)) return false
        cannon.target = null
        return true
    }

    override fun canDoAction(): Boolean = !dropUnseenTarget() && super.canDoAction()

    override fun doAction(): Boolean {
        if (dropUnseenTarget()) return false
        val fired = super.doAction()
        if (fired) cannon.playSound(PazSounds.PROJECTILE_SHOOT, 1.0f, 0.55f)
        return fired
    }
}
