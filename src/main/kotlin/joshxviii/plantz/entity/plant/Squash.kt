package joshxviii.plantz.entity.plant

import dev.efureev.sproutsquad.fixes.SquashAttackGoal
import joshxviii.plantz.ai.PlantState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class Squash(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    override fun registerGoals() {
        super.registerGoals()
    }
    // No HurtByTargetGoal from Plant: a squash never attacks players, so a player target would block it.
    override fun attackGoals() {
        this.goalSelector.addGoal(1, SquashAttackGoal(this))
        this.targetSelector.addGoal(4, NearestAttackableTargetGoal(this, LivingEntity::class.java, 5, true, false) { target, _ ->
            SquashAttackGoal.isSquashTarget(this, target)
        })
    }
    override fun canLookAround(): Boolean = false
    override fun canSurviveOn(block: BlockState): Boolean = true
    // The plant state is synced, so client and server agree: no grid snapping during the attack (leap).
    override fun clampToGrid(): Boolean = state != PlantState.ACTION && !getBlockBelow().getCollisionShape(level(), blockPosition().below()).isEmpty
}
