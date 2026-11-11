package dev.efureev.sproutsquad.content.plant

import joshxviii.plantz.ai.goal.GenerateSunGoal
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

/**
 * Potling, the Sprout Squad mascot. A cheap support plant: every minute it shakes
 * its sprout and drops a sun; sleeps at night. Kept deliberately simple as the template for
 * new characters (see docs/en/characters/potling.md).
 */
class Potling(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    override fun attackGoals() {}

    override fun sleepsDuringNight(): Boolean = true

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(1, GenerateSunGoal(
            usingEntity = this,
            cooldownTime = 1200,
            actionDelay = 10,
        ))
    }
}
