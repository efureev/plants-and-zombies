package dev.efureev.sproutsquad.structures

import dev.efureev.sproutsquad.registry.SquadSpawnPlacements
import joshxviii.plantz.PazEntities
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
            SquadSpawnPlacements.likeOriginalZombies(event, PazEntities.ENGINEER_ZOMBIE)
            SquadSpawnPlacements.likeOriginalZombies(event, PazEntities.SOLDIER_ZOMBIE)
            SquadSpawnPlacements.likeOriginalZombies(event, PazEntities.ROBO_ZOMBIE)
        }
    }
}
