package dev.efureev.sproutsquad.content.plant

import com.mojang.serialization.Codec
import joshxviii.plantz.ai.goal.ActionGoal
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.item.SeedPacketItem
import joshxviii.plantz.pazResource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Poppy Rattle: a poppy seed head with a face and a crown-shaped lid. Once by day and once by night it
 * grows a seed packet of a random plant and keeps up to two under its lid until its owner takes them.
 * Never sleeps. See docs/en/characters/poppy_rattle.md.
 *
 * Days and nights are counted by the dimension's clock as phase numbers ([phaseOf]), so a night slept
 * through still gives its seed, a thunderstorm is not a night, and time set back gives nothing.
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

        private val HELD_COUNT: EntityDataAccessor<Int> = SynchedEntityData.defineId(PoppyRattle::class.java, EntityDataSerializers.INT)
        private val NIGHT: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(PoppyRattle::class.java, EntityDataSerializers.BOOLEAN)

        /** Phase number of a clock time: even phases are days, odd ones nights. */
        fun phaseOf(clockTicks: Long): Long = Math.floorDiv(clockTicks, PHASE_TICKS)

        fun isNightPhase(phase: Long): Boolean = Math.floorMod(phase, 2L) == 1L

        /** How often a plant comes up, by its sun cost: cheap plants four times as often as the dearest. */
        fun weightFor(sunCost: Int): Int = when {
            sunCost <= 4 -> 4
            sunCost <= 7 -> 3
            sunCost <= 10 -> 2
            else -> 1
        }
    }

    private val held = mutableListOf<EntityType<*>>()
    private val owed = mutableListOf<Boolean>()

    /** The phase last counted; -1 for a rattle that has not seen a clock yet (a new one). */
    private var lastPhase = -1L

    /** Clock time when this phase's seed becomes owed; -1 when it is owed already or the phase is used. */
    private var dueAt = -1L
    private var lastGiven: EntityType<*>? = null

    val heldCount: Int get() = entityData.get(HELD_COUNT)
    val isNightPose: Boolean get() = entityData.get(NIGHT)

    fun held(): List<EntityType<*>> = held.toList()

    /** Seeds owed and not yet delivered, `true` for a night seed. */
    fun owedSeeds(): List<Boolean> = owed.toList()

    override fun attackGoals() {}

    override fun sleepsDuringNight(): Boolean = false

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(1, PoppySeedGoal(this))
    }

    override fun defineSynchedData(entityData: SynchedEntityData.Builder) {
        super.defineSynchedData(entityData)
        entityData.define(HELD_COUNT, 0)
        entityData.define(NIGHT, false)
    }

    override fun tick() {
        super.tick()
        if (!level().isClientSide && tickCount % 20 == 0) advanceClock(clockTime())
    }

    /** The dimension's clock time, or null where days do not change (the Nether, the End). */
    private fun clockTime(): Long? {
        val dimension = level().dimensionType()
        if (dimension.hasFixedTime() || dimension.defaultClock().isEmpty) return null
        return level().defaultClockTime
    }

    /**
     * Counts phases up to [clockTicks]: a new phase schedules its seed at a random moment in its first half;
     * a phase that ended before its seed came gives it at once. Worn on a head, new, or with time set back,
     * the current phase counts as used.
     */
    fun advanceClock(clockTicks: Long?) {
        if (clockTicks == null) return
        val phase = phaseOf(clockTicks)
        entityData.set(NIGHT, isNightPhase(phase))
        if (isAttached() || lastPhase < 0 || phase < lastPhase) {
            lastPhase = phase
            dueAt = -1
            return
        }
        if (phase > lastPhase) {
            if (dueAt >= 0) owe(isNightPhase(lastPhase))
            lastPhase = phase
            dueAt = phase * PHASE_TICKS + random.nextInt((PHASE_TICKS / 2).toInt())
        }
        if (dueAt >= 0 && clockTicks >= dueAt) {
            owe(isNightPhase(phase))
            dueAt = -1
        }
    }

    private fun owe(night: Boolean) {
        if (owed.size < CAPACITY) owed += night
    }

    /** Turns the first owed seed into a packet under the lid; false if it is full (the seed is lost) or no plant fits. */
    fun deliverSeed(): Boolean {
        if (owed.isEmpty()) return false
        val night = owed.removeAt(0)
        if (held.size >= CAPACITY) return false
        val level = level() as? ServerLevel ?: return false
        val type = PoppyRattlePool.pick(level, night, random, lastGiven) ?: return false
        held += type
        lastGiven = type
        syncHeld()
        playSound(SoundEvents.DECORATED_POT_INSERT)
        return true
    }

    /** Gives every held packet to [player] (what does not fit falls at their feet). */
    fun takeAll(player: Player): Int {
        val count = held.size
        held.forEach { type ->
            val stack = SeedPacketItem.stackFor(type)
            if (!player.inventory.add(stack)) (level() as? ServerLevel)?.let { player.spawnAtLocation(it, stack) }
        }
        held.clear()
        syncHeld()
        if (count > 0) playSound(SoundEvents.BUNDLE_REMOVE_ONE)
        return count
    }

    private fun dropHeld() {
        val level = level() as? ServerLevel ?: return
        held.forEach { level.addFreshEntity(ItemEntity(level, x, y + 0.5, z, SeedPacketItem.stackFor(it))) }
        held.clear()
        syncHeld()
    }

    private fun syncHeld() = entityData.set(HELD_COUNT, held.size)

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty && !player.isSecondaryUseActive
            && !isAttached() && heldCount > 0) {
            if (level().isClientSide) return InteractionResult.SUCCESS
            if (!verifyOwner(player)) return InteractionResult.FAIL
            takeAll(player)
            return InteractionResult.SUCCESS_SERVER
        }
        return super.mobInteract(player, hand)
    }

    override fun dropAsSeedPacketItem(force: Boolean, dropPumpkin: Boolean): Boolean {
        dropHeld()
        return super.dropAsSeedPacketItem(force, dropPumpkin)
    }

    override fun die(source: DamageSource) {
        val wasDead = dead
        super.die(source)
        if (!wasDead && dead) dropHeld()
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON

    override fun getAmbientSoundInterval(): Int = 300

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.store("sproutsquad:PoppyHeld", Identifier.CODEC.listOf(), held.map { EntityType.getKey(it) })
        output.store("sproutsquad:PoppyOwed", Codec.BOOL.listOf(), owed.toList())
        output.putLong("sproutsquad:PoppyPhase", lastPhase)
        output.putLong("sproutsquad:PoppyDueAt", dueAt)
        lastGiven?.let { output.putString("sproutsquad:PoppyLast", EntityType.getKey(it).toString()) }
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        held.clear()
        input.read("sproutsquad:PoppyHeld", Identifier.CODEC.listOf()).orElse(emptyList())
            .mapNotNull { BuiltInRegistries.ENTITY_TYPE.getOptional(it).orElse(null) }
            .take(CAPACITY).forEach { held += it }
        owed.clear()
        owed += input.read("sproutsquad:PoppyOwed", Codec.BOOL.listOf()).orElse(emptyList()).take(CAPACITY)
        lastPhase = input.getLongOr("sproutsquad:PoppyPhase", -1L)
        dueAt = input.getLongOr("sproutsquad:PoppyDueAt", -1L)
        lastGiven = input.getString("sproutsquad:PoppyLast").flatMap { id ->
            Identifier.tryParse(id)?.let { BuiltInRegistries.ENTITY_TYPE.getOptional(it) } ?: java.util.Optional.empty()
        }.orElse(null)
        syncHeld()
    }

    /** Delivers an owed seed: the head jolts and the lid hops (the `action` animation), then the packet is there. */
    class PoppySeedGoal(private val rattle: PoppyRattle) : ActionGoal(rattle, cooldownTime = 20, actionDelay = 10) {
        override fun startOnCooldown(): Boolean = false
        override fun canUse(): Boolean = rattle.isAlive && !rattle.isAttached()
        override fun canDoAction(): Boolean = rattle.owedSeeds().isNotEmpty()
        override fun doAction(): Boolean = rattle.deliverSeed()
    }
}
