package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazTags
import joshxviii.plantz.entity.zombie.DiggerZombie
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.EventHooks
import java.util.EnumSet

/**
 * The Digger Zombie digs through blocks in its way to the target (blocks in
 * `#sproutsquad:digger_breakable`: dirt, sand, stone bricks, glass, plant pots…). The original
 * had the tag, the mining animation and a path evaluator for it, but nothing that mined.
 * It digs when it walks into a wall or cannot find a path to a target at about its own height; it
 * does not dig a 1-block step it can jump. Respects `mobGriefing` and NeoForge's block-break event
 * (claim protection); the block drops as if mined with the Digger's pickaxe.
 */
class DiggerMineGoal(private val digger: DiggerZombie) : Goal() {
    companion object {
        const val MINE_TICKS = 30
        const val CHECK_PERIOD = 10
        /** Does not dig towards targets further than this (blocks). */
        const val MAX_TARGET_DISTANCE = 16.0
    }

    private var pos: BlockPos? = null
    private var ticks = 0
    private var checkIn = 0

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        // canUse runs only every other tick, so count calls instead of matching tickCount
        if (--checkIn > 0 || digger.tunnelTime > 0) return false
        checkIn = CHECK_PERIOD / 2
        val target = digger.target?.takeIf { it.isAlive } ?: return false
        // blocked: walking into a wall, or no path at all (then the zombie does not move)
        val blocked = digger.horizontalCollision || digger.navigation.isDone || digger.navigation.isStuck
        val distanceSqr = digger.distanceToSqr(target)
        if (!blocked || distanceSqr < 2.25 || distanceSqr > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) return false
        // a target far above or below is not reached by digging sideways (tunnelling handles that)
        if (kotlin.math.abs(target.y - digger.y) > 2.0 || horizontalDistanceSqr(target) < 1.0) return false
        val level = digger.level() as? ServerLevel ?: return false
        if (!EventHooks.canEntityGrief(level, digger)) return false
        pos = blockInTheWay(level)
        return pos != null
    }

    override fun canContinueToUse(): Boolean {
        val level = digger.level() as? ServerLevel ?: return false
        val current = pos ?: return false
        return digger.isAlive && ticks <= MINE_TICKS && breakable(level, current)
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        ticks = 0
        digger.navigation.stop()
    }

    override fun tick() {
        val level = digger.level() as? ServerLevel ?: return
        val current = pos ?: return
        val center = Vec3.atCenterOf(current)
        digger.lookControl.setLookAt(center.x, center.y, center.z)
        ticks++
        if (ticks % 20 == 1) digger.mineBlockTime = 1 // plays the Digger's own mining animation
        level.destroyBlockProgress(digger.id, current, ticks * 10 / MINE_TICKS)
        if (ticks >= MINE_TICKS) {
            level.destroyBlockProgress(digger.id, current, -1)
            val state = level.getBlockState(current)
            Block.dropResources(state, level, current, level.getBlockEntity(current), digger, digger.mainHandItem)
            level.destroyBlock(current, false, digger)
            pos = null
        }
    }

    override fun stop() {
        pos?.let { (digger.level() as? ServerLevel)?.destroyBlockProgress(digger.id, it, -1) }
        pos = null
    }

    private fun horizontalDistanceSqr(target: net.minecraft.world.entity.Entity): Double {
        val dx = target.x - digger.x
        val dz = target.z - digger.z
        return dx * dx + dz * dz
    }

    /** The block in front of the digger at foot or head height, towards the target; not a jumpable step. */
    private fun blockInTheWay(level: ServerLevel): BlockPos? {
        val target = digger.target ?: return null
        val facing = Direction.getApproximateNearest(target.x - digger.x, 0.0, target.z - digger.z)
        val front = digger.blockPosition().relative(facing)
        val head = front.above()
        val headFree = level.getBlockState(head).getCollisionShape(level, head).isEmpty
        val roomToJump = level.getBlockState(head.above()).getCollisionShape(level, head.above()).isEmpty &&
            level.getBlockState(digger.blockPosition().above(2)).getCollisionShape(level, digger.blockPosition().above(2)).isEmpty
        if (headFree && roomToJump) return null // a 1-block step: jump it, do not flatten the terrain
        return listOf(front, head).firstOrNull { breakable(level, it) }
    }

    private fun breakable(level: ServerLevel, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        return !state.isAir && state.`is`(PazTags.BlockTags.DIGGER_BREAKABLE) && state.getDestroySpeed(level, pos) >= 0 &&
            EventHooks.onEntityDestroyBlock(digger, pos, state)
    }
}
