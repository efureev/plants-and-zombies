package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.model.plants.PlantModel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.model.geom.ModelPart

/**
 * Base model for Sprout Squad plants: bakes the given animations into the original [PlantModel],
 * which plays them (init, idle, action while not idle, sleep, walk, plus the shared bounce).
 */
abstract class SquadPlantModel(
    root: ModelPart,
    init: AnimationDefinition? = null,
    idle: AnimationDefinition? = null,
    action: AnimationDefinition? = null,
    sleep: AnimationDefinition? = null,
    walk: AnimationDefinition? = null,
) : PlantModel(root) {
    init {
        initAnimation = init?.bake(root)
        idleAnimation = idle?.bake(root)
        actionAnimation = action?.bake(root)
        sleepAnimation = sleep?.bake(root)
        walkAnimation = walk?.bake(root)
    }
}
