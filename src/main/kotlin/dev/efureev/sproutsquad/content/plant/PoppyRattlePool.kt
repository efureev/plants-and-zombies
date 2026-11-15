package dev.efureev.sproutsquad.content.plant

import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazTags
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import java.util.concurrent.ConcurrentHashMap

/**
 * Which plant a [PoppyRattle] hands out: every plant of the `sproutsquad:plant` tag except
 * [PoppyRattle.EXCLUDED], awake at that time of day, cheaper ones more often.
 */
object PoppyRattlePool {
    /** Whether a plant type sleeps by day and at night, asked once of a sample plant. */
    private val sleeps = ConcurrentHashMap<EntityType<*>, Pair<Boolean, Boolean>>()

    /** Plants for a day seed (not asleep by day) or a night seed (not asleep at night). */
    fun candidates(level: ServerLevel, night: Boolean): List<EntityType<*>> =
        BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(PazTags.EntityTypes.PLANT)
            .filter { !it.`is`(PoppyRattle.EXCLUDED) }
            .map { it.value() }
            .filter { type ->
                val (byDay, atNight) = sleepFlags(level, type) ?: return@filter false
                if (night) !atNight else !byDay
            }

    fun pick(level: ServerLevel, night: Boolean, random: RandomSource, last: EntityType<*>?): EntityType<*>? =
        pickFrom(candidates(level, night), random, last)

    /** A weighted pick ([PoppyRattle.weightFor] the sun cost) that avoids [last] unless it is the only one. */
    fun pickFrom(candidates: List<EntityType<*>>, random: RandomSource, last: EntityType<*>?): EntityType<*>? {
        val pool = candidates.filter { it != last }.ifEmpty { candidates }
        if (pool.isEmpty()) return null
        val weights = pool.map { PoppyRattle.weightFor(PazConfig.getSunCost(it)) }
        var roll = random.nextInt(weights.sum())
        pool.forEachIndexed { i, type ->
            roll -= weights[i]
            if (roll < 0) return type
        }
        return pool.last()
    }

    private fun sleepFlags(level: ServerLevel, type: EntityType<*>): Pair<Boolean, Boolean>? =
        sleeps[type] ?: (type.create(level, EntitySpawnReason.LOAD) as? Plant)
            ?.let { Pair(it.sleepsDuringDay(), it.sleepsDuringNight()) }
            ?.also { sleeps[type] = it }
}
