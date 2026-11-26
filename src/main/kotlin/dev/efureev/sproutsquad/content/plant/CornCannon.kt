package dev.efureev.sproutsquad.content.plant

import dev.efureev.sproutsquad.content.projectile.CornCob
import dev.efureev.sproutsquad.machines.PlayerMachines
import joshxviii.plantz.PazSounds
import joshxviii.plantz.isHypnotized
import joshxviii.plantz.ai.goal.ProjectileAttackGoal
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.Vec2
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Corn Cannon: heavy artillery. A long husk lying on a rosette of leaves fires its corn cob as a rocket
 * in a high arc; the cob blows up and hurts only what the cannon would shoot at. A new cob grows in 3 s.
 * It does not see zombies within [MIN_RANGE] blocks. Does not sleep. See docs/en/characters/corn_cannon.md.
 */
class CornCannon(type: EntityType<out Plant>, level: Level) : Plant(type, level), SquadPlantLook {
    companion object {
        /** Allies of the players a cannon and its cob leave alone: hypnotized mobs and a player's machines. */
        fun isAlly(entity: Entity): Boolean = entity.isHypnotized() || PlayerMachines.isPlayerMachine(entity)

        /** What a cob whose cannon is gone may hit: a zombie that is nobody's ally. */
        fun isStrayCobTarget(entity: Entity): Boolean = entity is Zombie && !isAlly(entity)

        private val RELOAD_TOTAL: EntityDataAccessor<Int> = SynchedEntityData.defineId(CornCannon::class.java, EntityDataSerializers.INT)

        const val MAX_HEALTH = 90.0
        const val SUN_COST = 14
        const val RANGE = 40.0

        /** Zombies closer than this are not seen: no shot, and a target that comes this close is dropped. */
        const val MIN_RANGE = 5.0

        /** Ticks from the start of the action to the launch: the nose is up about 0.5 s in. */
        const val ACTION_DELAY = 11

        /** Cooldown after a shot, while the new cob grows: a shot every COOLDOWN + ACTION_DELAY - 1 = 70 ticks. */
        const val COOLDOWN = 60

        /** How long the eyes squint from the start of a shot: the aim and the launch, until the kick back settles. */
        const val AIM_MILLIS = 700L

        /**
         * A called-off shot: it goes through the rest of the shot's motion without firing before it may aim again
         * (ACTION_DELAY + SETTLE_TICKS covers SHOT_ANIMATION_TICKS, so the pose does not snap back).
         */
        const val SETTLE_TICKS = 11

        /** Length of the shot's animation (CornCannonModel): aim, launch, kick back and settle. */
        const val SHOT_ANIMATION_TICKS = 22

        /** Blasts in a row that miss the target before the cannon leaves it alone for [MISS_IGNORE_TICKS]. */
        const val MISSES_TO_DROP = 3
        const val MISS_IGNORE_TICKS = 100

        const val ENTITY_LIMIT = 10
        const val EYE_HEIGHT = 0.6f

        /**
         * Where the cob leaves the husk, as a [CornCob] spawn offset from the eyes: forward and up, about where
         * the raised mouth is and still inside the cannon's own block (a cob that starts in a wall right in
         * front vanishes on its first tick).
         */
        val COB_SPAWN = Vec2(0.3f, 0.15f)
    }

    override fun sleepsDuringNight(): Boolean = false

    /**
     * What it shoots at, and so what its cob may hurt: zombies; when tamed, other monsters too. Never creepers,
     * plants, hypnotized mobs or a player's machines (allies of the players).
     */
    fun isValidTarget(target: LivingEntity): Boolean =
        target !is Plant && target !is Creeper && !isAlly(target) && (target is Zombie || (target is Enemy && isTame))

    fun inDeadZone(target: Entity): Boolean = distanceToSqr(target) < MIN_RANGE * MIN_RANGE

    /**
     * Nothing the cob would hit over [entity]'s head, by the cob's own rules: blocks with collision count (leaves
     * too), water does not (the cob flies through it). The cob goes up and comes down from the sky, so a cannon
     * under a roof or a target in a cave or under a tree would only get the roof hit, forever. A ray straight up
     * from the eyes to just above the column's highest block of any kind (scaffolding is not in the
     * motion-blocking heightmap).
     */
    fun underOpenSky(entity: Entity): Boolean {
        val level = level()
        val from = entity.eyePosition
        val top = level.getHeight(Heightmap.Types.WORLD_SURFACE, entity.blockX, entity.blockZ) + 1.0
        if (from.y >= top) return true
        // collision as for something coming down from above (scaffolding then has its top plate), not as for the entity
        val up = ClipContext(from, Vec3(from.x, top, from.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
            CollisionContext.positionContext(top))
        return level.clip(up).type == HitResult.Type.MISS
    }

    /** A target worth aiming at, sky over the cannon aside: valid, out of the dead zone, not left alone after misses, under open sky. */
    fun canAimAt(target: LivingEntity): Boolean =
        isValidTarget(target) && !inDeadZone(target) && !ignores(target) && underOpenSky(target)

    /** A target it can shoot at right now: [canAimAt], and open sky over the cannon too. */
    fun canShootAt(target: LivingEntity): Boolean = canAimAt(target) && underOpenSky(this)

    /** The cooldown the last shot set (shorter when powered up), synced for the cob's growth; 0 after a called-off shot. */
    var reloadTotal: Int
        get() = entityData.get(RELOAD_TOTAL)
        private set(value) = entityData.set(RELOAD_TOTAL, value)

    private var missedTarget: UUID? = null
    private var misses = 0
    private val ignoredUntil = HashMap<UUID, Long>()

    /**
     * A blast of its cob that did not hurt [target] (it burst on an overhang or a wall next to it). After
     * [MISSES_TO_DROP] in a row the cannon drops the target and leaves it alone for [MISS_IGNORE_TICKS].
     */
    fun blastMissed(target: LivingEntity) {
        if (missedTarget != target.uuid) {
            missedTarget = target.uuid
            misses = 0
        }
        if (++misses < MISSES_TO_DROP) return
        misses = 0
        ignoredUntil[target.uuid] = level().gameTime + MISS_IGNORE_TICKS
        if (this.target === target) this.target = null
    }

    /** A blast that hurt [target]: the count of misses starts again. */
    fun blastHit(target: LivingEntity) {
        if (missedTarget == target.uuid) misses = 0
    }

    private fun ignores(target: Entity): Boolean {
        val until = ignoredUntil[target.uuid] ?: return false
        if (until > level().gameTime) return true
        ignoredUntil.remove(target.uuid)
        return false
    }

    /** Called when a shot's action ends: [total] is the cooldown it set, 0 if no cob left. */
    fun reloadStarted(total: Int) {
        reloadTotal = total
    }

    /**
     * How grown the cob is, 0 right after a shot to 1 for a full one: by the cooldown left out of the cooldown
     * that shot set, whatever a power-up does meanwhile. -1 while aiming, 0 when idle, or a called-off shot: full.
     */
    fun cobGrowth(): Float {
        val total = reloadTotal
        if (cooldown <= 0 || total <= 0) return 1f
        return (1f - cooldown / total.toFloat()).coerceIn(0f, 1f)
    }

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(RELOAD_TOTAL, COOLDOWN) // a planted cannon starts cooling down: its first cob grows
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putInt("sproutsquad:ReloadTotal", reloadTotal)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        reloadTotal = input.getIntOr("sproutsquad:ReloadTotal", COOLDOWN)
    }

    /** The model's pose: the cob's growth in thousandths (CornCannonModel). */
    override fun lookPose(): Int = (cobGrowth() * 1000).roundToInt()

    /** `aim` (squinting) for the first [AIM_MILLIS] of a shot, plain otherwise and while dying (the renderer adds `sleep` then). */
    fun faceSuffix(actionMillis: Long?): String = when {
        deathTime > 0 -> ""
        actionMillis != null && actionMillis < AIM_MILLIS -> "aim"
        else -> ""
    }

    override fun lookSuffix(actionMillis: Long?): String? = faceSuffix(actionMillis).ifEmpty { null }

    /**
     * No retaliation or owner goals: they would pick players, animals or villagers, which the cob passes
     * through. Whatever hurts it and is a valid target is found by the nearest-target goal anyway.
     */
    override fun attackGoals() {}

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(2, CornCannonAttackGoal(this))
        targetSelector.addGoal(4, CornCannonTargetGoal(this))
    }
}

/**
 * The nearest target it can shoot at ([CornCannon.canShootAt]), seen or not: the cob flies in a high arc, and
 * the eyes sit only 0.6 blocks up, so a one-block wall in front would otherwise hide every zombie behind it.
 */
class CornCannonTargetGoal(private val cannon: CornCannon) : NearestAttackableTargetGoal<LivingEntity>(
    cannon, LivingEntity::class.java, 5, false, false,
    { target, _ -> cannon.canAimAt(target) },
) {
    init {
        targetConditions.ignoreLineOfSight()
    }

    /** The cannon's own sky once per search, before the per-target checks. */
    override fun canUse(): Boolean = cannon.underOpenSky(cannon) && super.canUse()
}

/**
 * Launches one [CornCob] in a high arc; drops a target it can no longer shoot at ([CornCannon.canShootAt]).
 * A shot called off during the aim uses up no cob: the reload ActionGoal sets anyway becomes a short settle
 * while the nose comes down, with the cob still full. [fired] is shared with the end-of-action callback.
 */
class CornCannonAttackGoal private constructor(private val cannon: CornCannon, private val fired: BooleanArray) : ProjectileAttackGoal(
    usingEntity = cannon,
    cooldownTime = CornCannon.COOLDOWN,
    actionDelay = CornCannon.ACTION_DELAY,
    actionStartEffect = { fired[0] = false },
    actionEndEffect = {
        if (fired[0]) cannon.reloadStarted(cannon.cooldown)
        else {
            cannon.reloadStarted(0)
            cannon.cooldown = CornCannon.SETTLE_TICKS
        }
    },
    projectileFactory = { CornCob(cannon.level(), cannon) },
    velocity = 1.1,
    useHighArc = true,
    soundEvent = null,
) {
    constructor(cannon: CornCannon) : this(cannon, BooleanArray(1))

    private fun dropUnseenTarget(): Boolean {
        val target = cannon.target ?: return true
        if (cannon.canShootAt(target)) return false
        cannon.target = null
        return true
    }

    override fun canDoAction(): Boolean = !dropUnseenTarget() && super.canDoAction()

    override fun doAction(): Boolean {
        if (dropUnseenTarget()) return false
        fired[0] = super.doAction()
        if (fired[0]) cannon.playSound(PazSounds.PROJECTILE_SHOOT, 1.0f, 0.55f)
        return fired[0]
    }
}
