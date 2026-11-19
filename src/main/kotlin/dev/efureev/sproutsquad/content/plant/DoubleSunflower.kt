package dev.efureev.sproutsquad.content.plant

import joshxviii.plantz.PazEntities
import joshxviii.plantz.ai.goal.ActionGoal
import joshxviii.plantz.entity.Sun
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Double Sunflower: a Sunflower with two stems, a flower on each. Every Sunflower cycle (about 45 s)
 * each head drops a sun, so it makes twice the sun of a Sunflower; sleeps at night like it
 * (docs/en/characters/double_sunflower.md).
 */
class DoubleSunflower(type: EntityType<out Plant>, level: Level) : Plant(type, level), SquadPlantLook {
    companion object {
        const val MAX_HEALTH = 30.0
        const val SUN_COST = 8
        /** The Sunflower's cycle (Sunflower.registerGoals). */
        const val COOLDOWN = 900
        const val SUN_PER_HEAD = 1

        /**
         * Where the heads' centres are, in blocks from the plant's feet, facing +z: x to the plant's left, y up.
         * The tall head stands behind on the left, the short one in front on the right (DoubleSunflowerModel's pose).
         */
        val TALL_HEAD = Vec3(0.26, 1.35, 0.06)
        val SHORT_HEAD = Vec3(-0.45, 0.88, 0.39)
    }

    override fun attackGoals() {}

    override fun sleepsDuringNight(): Boolean = true

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(1, DoubleSunGoal(this))
    }

    /**
     * The heads' positions in the world: [TALL_HEAD] and [SHORT_HEAD] turned the way the plant looks
     * (DoubleSunflowerModel turns the whole stem, both heads with it, by the head's yaw).
     */
    fun headPositions(): List<Vec3> = listOf(TALL_HEAD, SHORT_HEAD).map {
        it.yRot(-yHeadRot * Mth.DEG_TO_RAD).scale(scale.toDouble()).add(position())
    }

    /** One sun from each head; a baby (half the sun, like GenerateSunGoal) only from the tall one. Returns the sun made. */
    fun dropSun(level: ServerLevel): Int {
        val heads = headPositions().let { if (isBaby) it.take(1) else it }
        heads.forEach { Sun.award(level, clearSpot(level, it), SUN_PER_HEAD) }
        playSound(SoundEvents.CHICKEN_EGG, 1.0f, 0.5f) // the Sunflower's sound
        return heads.size * SUN_PER_HEAD
    }

    /**
     * Where a head's sun can appear: the short head reaches 0.6 blocks out, past the plant's box and into the next
     * block when it looks diagonally at a wall; then the sun comes out above the plant's centre instead (a sun inside
     * a block gets pushed out to wherever there is room, even behind the wall).
     */
    private fun clearSpot(level: ServerLevel, spot: Vec3): Vec3 {
        val half = PazEntities.SUN.width / 2.0
        return if (level.noCollision(AABB.ofSize(spot, half * 2, half * 2, half * 2))) spot else Vec3(x, spot.y, z)
    }

    /**
     * Glows up before the sun comes, like the Sunflower (PlantRenderer's glow for it). Not while dying: the
     * renderer then draws the `_sleep` texture, which has no glow sheet, and would light up the whole plant.
     */
    override fun lookGlow(cooldown: Int): Float =
        if (isAsleep || isGrowingSeeds || deathTime > 0) 0f else 1f - (cooldown.coerceIn(0, 100) / 100f)

    /** GenerateSunGoal of the Sunflower (awake at night with coffee), dropping the sun from both heads. */
    private class DoubleSunGoal(val plant: DoubleSunflower) :
        ActionGoal(plant, cooldownTime = COOLDOWN, actionDelay = 10, cooldownVariationRange = -10..20) {
        override fun canUse(): Boolean = plant.isAlive && !plant.isAsleep && !plant.isGrowingSeeds
        override fun canDoAction(): Boolean = true
        override fun doAction(): Boolean {
            val level = plant.level() as? ServerLevel ?: return false
            plant.dropSun(level)
            return true
        }
    }
}
