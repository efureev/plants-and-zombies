package dev.efureev.sproutsquad.content.plant

import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.pazResource
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

/**
 * Poppy Rattle: a poppy seed head with a face and a crown-shaped lid. Once by day and once by night it
 * grows a seed packet of a random plant and keeps up to two under its lid until its owner takes them.
 * Never sleeps. See docs/en/characters/poppy_rattle.md.
 */
class PoppyRattle(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    companion object {
        const val MAX_HEALTH = 16.0
        const val SUN_COST = 6

        /** How many may stand within 32 blocks per owner (PazConfig entity limit). */
        const val ENTITY_LIMIT = 4

        /** Seed packets it can hold at once: one from the day, one from the night. */
        const val CAPACITY = 2

        /** Length of a day or a night in clock ticks: a phase. */
        const val PHASE_TICKS = 12000L

        /** Plants it never hands out (`data/sproutsquad/tags/entity_type/poppy_rattle_excluded.json`). */
        val EXCLUDED: TagKey<EntityType<*>> = TagKey.create(Registries.ENTITY_TYPE, pazResource("poppy_rattle_excluded"))
    }

    override fun attackGoals() {}

    override fun sleepsDuringNight(): Boolean = false
}
