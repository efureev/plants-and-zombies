package dev.efureev.sproutsquad.content.plant

/**
 * How the original's PlantRenderer draws one of our plants. It reads these once in
 * `PlantRenderer.extractRenderState`, so a new Sprout Squad plant needs no branch of its own in that file.
 * Common code: no client classes here.
 */
interface SquadPlantLook {
    /**
     * A texture suffix after the plant's own (`<id>_<suffix>.png`), or null for none.
     * [actionMillis]: how long the action animation has been running, null when it is not.
     */
    fun lookSuffix(actionMillis: Long?): String? = null

    /** A pose value for the model (`PlantRenderState.squadPose`). */
    fun lookPose(): Int = 0

    /** `PlantRenderState.useSpecialAction` for the model. */
    fun lookSpecialAction(): Boolean = false
}
