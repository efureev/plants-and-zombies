package dev.efureev.sproutsquad.content.plant

import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.projectile.Needle
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec2

/**
 * Needle Spruce: a three-block mountain spruce. It stands straight; when it sees an enemy within
 * 20 blocks it bends forward like a catapult and fires three needles at once, one from each tier.
 * Does not sleep at night. See docs/en/characters/needle_spruce.md.
 */
class NeedleSpruce(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    companion object {
        const val MAX_HEALTH = 40.0
        const val NEEDLE_DAMAGE = 1.0
        const val RANGE = 20.0
        const val SUN_COST = 7
        const val VOLLEY_SIZE = 3

        /** Ticks from the start of the action to the shot: the crown is at the bottom of its whip (0.55 s). */
        const val ACTION_DELAY = 11

        /**
         * Cooldown after a volley: a volley every 70 ticks (3.5 s). ActionGoal starts the next action
         * on the tick the cooldown runs out, so the cycle is COOLDOWN + ACTION_DELAY - 1.
         */
        const val COOLDOWN = 60

        /** Sun dropped when a player kills it (the drawing's number; other plants drop half their price). */
        const val PLAYER_KILL_SUN = 4

        /** How long the eyes stay squeezed shut from the start of a volley. */
        const val STRAIN_MILLIS = 850L

        const val EYE_HEIGHT = 1.4f

        /**
         * Where the needles leave the crown, as [Needle] spawn offsets from the eyes: x forward along
         * the look direction, y up. The fronts of the three tiers, as the crown leans into the shot.
         */
        val NEEDLE_OFFSETS = listOf(Vec2(0.6f, 0.0f), Vec2(0.7f, 0.5f), Vec2(0.8f, 0.9f))
    }

    override fun sleepsDuringNight(): Boolean = false

    /**
     * The texture suffix for the face: `strain` (eyes squeezed shut) for the first [STRAIN_MILLIS] of a
     * volley, `angry` while it has a target in range (the synced [isAggressive] flag that
     * ProjectileAttackGoal sets), plain otherwise. Nothing while dying: the renderer adds `sleep` then.
     * @param actionMillis how long the action animation has been running, or null if it is not.
     */
    fun faceSuffix(actionMillis: Long?): String = when {
        deathTime > 0 -> ""
        actionMillis != null && actionMillis < STRAIN_MILLIS -> "strain"
        isAggressive -> "angry"
        else -> ""
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(2, NeedleVolleyGoal(this))
        targetSelector.addGoal(4, NearestAttackableTargetGoal(this, LivingEntity::class.java, 5, true, false) { target, _ ->
            target !is Plant
                && target !is Creeper
                && (target is Zombie || (target is Enemy && isTame))
        })
    }
}

/**
 * Fires [NeedleSpruce.VOLLEY_SIZE] needles at once, each aimed like a single shot of [ProjectileAttackGoal],
 * from the tier given by [tier] (shared with the projectile factory, which the parent calls per shot).
 */
class NeedleVolleyGoal private constructor(private val spruce: NeedleSpruce, private val tier: IntArray) : ProjectileAttackGoal(
    usingEntity = spruce,
    cooldownTime = NeedleSpruce.COOLDOWN,
    actionDelay = NeedleSpruce.ACTION_DELAY,
    projectileFactory = { Needle(spruce.level(), spruce, NeedleSpruce.NEEDLE_OFFSETS[tier[0]]) },
    velocity = 1.5,
    aimAtBalloons = true,
    soundEvent = null,
) {
    constructor(spruce: NeedleSpruce) : this(spruce, IntArray(1))

    override fun doAction(): Boolean {
        var fired = false
        for (i in 0 until NeedleSpruce.VOLLEY_SIZE) {
            tier[0] = i
            fired = super.doAction() or fired
        }
        if (fired) spruce.playSound(PazSounds.PROJECTILE_SHOOT, 0.9f, 0.4f / (spruce.random.nextFloat() * 0.4f + 0.8f))
        return fired
    }
}
