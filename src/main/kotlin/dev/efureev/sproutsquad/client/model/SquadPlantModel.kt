package dev.efureev.sproutsquad.client.model

import joshxviii.plantz.ai.PlantState
import joshxviii.plantz.model.plants.PlantModel
import joshxviii.plantz.renderer.entity.plant.PlantRenderState
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.KeyframeAnimation
import net.minecraft.client.model.geom.ModelPart

/**
 * Base model for Sprout Squad plants.
 *
 * The original [PlantModel] keeps its animations in package-private fields, so a subclass in our
 * package cannot set them. Instead the animations are passed here: [setupAnim] lets the original
 * apply its pose reset and the shared "bounce", then plays ours on top.
 */
abstract class SquadPlantModel(
    root: ModelPart,
    init: AnimationDefinition? = null,
    idle: AnimationDefinition? = null,
    action: AnimationDefinition? = null,
    sleep: AnimationDefinition? = null,
) : PlantModel(root) {
    private val initAnim: KeyframeAnimation? = init?.bake(root)
    private val idleAnim: KeyframeAnimation? = idle?.bake(root)
    private val actionAnim: KeyframeAnimation? = action?.bake(root)
    private val sleepAnim: KeyframeAnimation? = sleep?.bake(root)

    override fun setupAnim(state: PlantRenderState) {
        super.setupAnim(state)
        initAnim?.apply(state.initAnimationState, state.ageInTicks)
        idleAnim?.apply(state.idleAnimationState, state.ageInTicks)
        if (state.plantState != PlantState.IDLE) actionAnim?.apply(state.actionAnimationState, state.ageInTicks)
        sleepAnim?.apply(state.sleepAnimationState, state.ageInTicks)
    }
}
