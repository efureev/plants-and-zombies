package joshxviii.plantz.entity.plant

import dev.efureev.sproutsquad.fixes.SquashAttackGoal
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

class Squash(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    /** True while [SquashAttackGoal] carries the squash through the air (no grid clamping). */
    var leaping = false

    override fun registerGoals() {
        super.registerGoals()
    }
    override fun attackGoals() {
        super.attackGoals()
        this.goalSelector.addGoal(1, SquashAttackGoal(this))
        this.targetSelector.addGoal(4, NearestAttackableTargetGoal(this, LivingEntity::class.java, 5, true, false) { target, _ ->
            target !is Plant && (target is Zombie || (target is Enemy && isTame))
        })
    }
    override fun canLookAround(): Boolean = false
    override fun canSurviveOn(block: BlockState): Boolean = true
    override fun clampToGrid(): Boolean = !leaping && !getBlockBelow().getCollisionShape(level(), blockPosition().below()).isEmpty
}
