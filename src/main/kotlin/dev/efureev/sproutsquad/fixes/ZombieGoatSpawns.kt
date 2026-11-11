package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazMain
import joshxviii.plantz.PazSpawnPlacements
import joshxviii.plantz.entity.zombie.PazZombie
import joshxviii.plantz.entity.zombie.ZombieGoat
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.EventPriority
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent

/**
 * Zombie Goat in the world (phase F2): in the original it exists only as a spawn egg.
 * It now spawns at night in mountain biomes (`#sproutsquad:zombie/has_zombie_goat`) and often
 * carries an Imp rider, like a chicken jockey.
 */
object ZombieGoatSpawns {
    val HAS_ZOMBIE_GOAT: TagKey<Biome> = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(PazMain.MODID, "zombie/has_zombie_goat"))

    const val WEIGHT = 12
    const val IMP_RIDER_CHANCE = 0.4f

    /** Spawns that get a rider; not spawn eggs or commands, so `/pz gallery` shows the goat alone. */
    private val RIDER_REASONS = setOf(
        EntitySpawnReason.NATURAL, EntitySpawnReason.CHUNK_GENERATION, EntitySpawnReason.SPAWNER,
        EntitySpawnReason.TRIAL_SPAWNER, EntitySpawnReason.STRUCTURE,
    )

    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { event ->
            // the same placement the original gives its zombies (PazSpawnPlacements.registerSpawnPlacement defaults)
            event.register(PazEntities.ZOMBIE_GOAT, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                PazZombie::checkPazZombieSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)
            // biome spawn list, read later by PazSpawnsBiomeModifier when a server starts
            PazSpawnPlacements.addBiomeSpawn(HAS_ZOMBIE_GOAT, PazEntities.ZOMBIE_GOAT, category = MobCategory.MONSTER,
                weight = WEIGHT, minGroupSize = 1, maxGroupSize = 2)
        }
        // last, so a spawn another mod cancels gets no rider
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FinalizeSpawnEvent::class.java, ::addImpRider)
    }

    /**
     * The spawner adds the goat with its passengers (`addFreshEntityWithPassengers`), so the Imp only
     * has to be mounted here, not added to the level.
     */
    private fun addImpRider(event: FinalizeSpawnEvent) {
        val goat = event.entity as? ZombieGoat ?: return
        if (event.isCanceled || event.isSpawnCancelled) return
        if (event.spawnType !in RIDER_REASONS || goat.isVehicle || goat.random.nextFloat() >= IMP_RIDER_CHANCE) return
        val imp = PazEntities.IMP.create(goat.level(), EntitySpawnReason.JOCKEY) ?: return
        // where it will sit: skip the rider if it would be stuck in a low ceiling
        imp.snapTo(goat.x, goat.y + goat.bbHeight, goat.z, goat.yRot, 0f)
        if (!event.level.noCollision(imp, imp.boundingBox)) return
        // the Imp's own spawn goes through FinalizeSpawnEvent too, like EventHooks.finalizeMobSpawn does it
        val impEvent = FinalizeSpawnEvent(imp, event.level, imp.x, imp.y, imp.z, event.difficulty, EntitySpawnReason.JOCKEY, null, null)
        NeoForge.EVENT_BUS.post(impEvent)
        if (impEvent.isCanceled || impEvent.isSpawnCancelled) return
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        imp.finalizeSpawn(event.level, impEvent.difficulty, impEvent.spawnType, impEvent.spawnData)
        imp.startRiding(goat, true, false)
    }
}
