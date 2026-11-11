package dev.efureev.sproutsquad.structures

import joshxviii.plantz.PazEntities
import joshxviii.plantz.entity.zombie.PazZombie
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent

/**
 * Spawn rules for zombies that the mod's structures bring in at night through `spawn_overrides`
 * (docs/en/STRUCTURES.md). In the original the Engineer, the Soldier and the Robo-Zombie come only
 * with raids, which place them without spawn rules, so they have none: a natural spawn of theirs
 * would ignore the light, and could happen in water or in the air.
 */
object StructureSpawns {
    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { event ->
            // read the Paz* objects only here: they are filled from the last RegisterEvent (neoforge/PazNeoForge.kt)
            onGround(event, PazEntities.ENGINEER_ZOMBIE)
            onGround(event, PazEntities.SOLDIER_ZOMBIE)
            onGround(event, PazEntities.ROBO_ZOMBIE)
        }
    }

    /** The same placement the original gives its zombies (PazSpawnPlacements.registerSpawnPlacement defaults). */
    private fun <T : PazZombie> onGround(event: RegisterSpawnPlacementsEvent, type: EntityType<T>) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            PazZombie::checkPazZombieSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)
    }
}
