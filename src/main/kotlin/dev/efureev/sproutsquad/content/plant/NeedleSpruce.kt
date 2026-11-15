package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.block.NeedleSpruceStumpBlock
import dev.efureev.sproutsquad.registry.SquadBlocks
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazSounds
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.Sun
import joshxviii.plantz.pazResource
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.projectile.Needle
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.registries.Registries
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2

/**
 * Needle Spruce: a three-block mountain spruce. It stands straight; when it sees an enemy within
 * 20 blocks it bends forward like a catapult and fires three needles at once, one from each tier.
 * Does not sleep at night. See docs/en/characters/needle_spruce.md.
 */
class NeedleSpruce(type: EntityType<out Plant>, level: Level) : Plant(type, level) {
    companion object {
        const val MAX_HEALTH = 40.0
        const val NEEDLE_DAMAGE = 1.0
        const val RANGE = 20.0
        const val SUN_COST = 7
        const val VOLLEY_SIZE = 3

        /** Ticks from the start of the action to the shot: the crown is at the bottom of its whip (0.55 s). */
        const val ACTION_DELAY = 11

        /**
         * Cooldown after a volley: a volley every 70 ticks (3.5 s). ActionGoal starts the next action
         * on the tick the cooldown runs out, so the cycle is COOLDOWN + ACTION_DELAY - 1.
         */
        const val COOLDOWN = 60

        /** Sun dropped when a player kills it (the drawing's number; other plants drop half their price). */
        const val PLAYER_KILL_SUN = 4

        /** How long the eyes stay squeezed shut from the start of a volley. */
        const val STRAIN_MILLIS = 850L

        const val EYE_HEIGHT = 1.4f

        /** Where it grows wild: snowy slopes and groves (`data/sproutsquad/tags/worldgen/biome/plant/has_needle_spruce.json`). */
        val HAS_NEEDLE_SPRUCE: TagKey<Biome> = TagKey.create(Registries.BIOME, pazResource("plant/has_needle_spruce"))

        /**
         * Where the needles leave the crown, as [Needle] spawn offsets from the eyes: x forward along
         * the look direction, y up. The fronts of the three tiers, as the crown leans into the shot.
         */
        val NEEDLE_OFFSETS = listOf(Vec2(0.6f, 0.0f), Vec2(0.7f, 0.5f), Vec2(0.8f, 0.9f))
    }

    override fun sleepsDuringNight(): Boolean = false

    /**
     * A mountain spruce: lives and is planted on snow too, where it grows wild (like the Ice Peashooter).
     * Not on powder snow: it would sink in (it is freeze immune, but buried).
     */
    override fun canSurviveOn(block: BlockState): Boolean =
        super.canSurviveOn(block) || (block.`is`(BlockTags.SNOW) && !block.`is`(Blocks.POWDER_SNOW))

    /** Three blocks tall: a lower ceiling would leave it standing in the ceiling, suffocating. */
    override fun hasRoomAt(level: LevelReader, pos: BlockPos): Boolean {
        val half = type.width / 2.0
        val x = pos.x + 0.5
        val z = pos.z + 0.5
        return level.noBlockCollision(null, AABB(x - half, pos.y + 1.0, z - half, x + half, pos.y + type.height.toDouble(), z + half))
    }

    /**
     * The texture suffix for the face: `strain` (eyes squeezed shut) for the first [STRAIN_MILLIS] of a
     * volley, `angry` while it has a target in range (the synced [isAggressive] flag that
     * ProjectileAttackGoal sets), plain otherwise. Nothing while dying: the renderer adds `sleep` then.
     * @param actionMillis how long the action animation has been running, or null if it is not.
     */
    fun faceSuffix(actionMillis: Long?): String = when {
        deathTime > 0 -> ""
        actionMillis != null && actionMillis < STRAIN_MILLIS -> "strain"
        isAggressive -> "angry"
        else -> ""
    }

    /**
     * The trunk snaps at the bend: the lower part stays as a stump block (NeedleSpruceStumpBlock), the
     * crown falls (NeedleSpruceModel) and sheds its needles. A spruce worn on a head or riding something
     * drops what the stump would. A player's kill drops [PLAYER_KILL_SUN] sun in all: Plant.die gives
     * half the price, this adds the rest.
     */
    override fun die(source: DamageSource) {
        val wasDead = dead
        super.die(source)
        val level = level() as? ServerLevel ?: return
        if (wasDead) return
        val stump = SquadBlocks.NEEDLE_SPRUCE_STUMP.get().defaultBlockState()
        if (isPassenger || isAttached()) Block.dropResources(stump, level, blockPosition())
        else NeedleSpruceStumpBlock.placeOrDrop(level, blockPosition())
        level.sendParticles(BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LEAVES.defaultBlockState()),
            x, y + 2.0, z, 40, 0.5, 0.6, 0.5, 0.1)
        if (source.entity is Player) {
            val extra = PLAYER_KILL_SUN - PazConfig.getSunCost(type) / 2
            if (extra > 0) Sun.award(level, position(), extra)
        }
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(2, NeedleVolleyGoal(this))
        targetSelector.addGoal(4, NearestAttackableTargetGoal(this, LivingEntity::class.java, 5, true, false) { target, _ ->
            target !is Plant
                && target !is Creeper
                && (target is Zombie || (target is Enemy && isTame))
        })
    }
}

/**
 * Fires [NeedleSpruce.VOLLEY_SIZE] needles at once, each aimed like a single shot of [ProjectileAttackGoal],
 * from the tier given by [tier] (shared with the projectile factory, which the parent calls per shot).
 */
class NeedleVolleyGoal private constructor(private val spruce: NeedleSpruce, private val tier: IntArray) : ProjectileAttackGoal(
    usingEntity = spruce,
    cooldownTime = NeedleSpruce.COOLDOWN,
    actionDelay = NeedleSpruce.ACTION_DELAY,
    projectileFactory = { Needle(spruce.level(), spruce, NeedleSpruce.NEEDLE_OFFSETS[tier[0]]) },
    velocity = 1.5,
    aimAtBalloons = true,
    soundEvent = null,
) {
    constructor(spruce: NeedleSpruce) : this(spruce, IntArray(1))

    override fun doAction(): Boolean {
        var fired = false
        for (i in 0 until NeedleSpruce.VOLLEY_SIZE) {
            tier[0] = i
            fired = super.doAction() or fired
        }
        if (fired) spruce.playSound(PazSounds.PROJECTILE_SHOOT, 0.9f, 0.4f / (spruce.random.nextFloat() * 0.4f + 0.8f))
        return fired
    }
}
