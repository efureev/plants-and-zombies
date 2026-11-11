package dev.efureev.sproutsquad.registry

import joshxviii.plantz.entity.zombie.PazZombie
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent

/** Spawn placements of Sprout Squad's own code, registered from [RegisterSpawnPlacementsEvent] listeners. */
object SquadSpawnPlacements {
    /**
     * The placement the original gives its zombies (the defaults of PazSpawnPlacements.registerSpawnPlacement):
     * on the ground, in the dark, never on Peaceful.
     */
    fun <T : PazZombie> likeOriginalZombies(event: RegisterSpawnPlacementsEvent, type: EntityType<T>) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            PazZombie::checkPazZombieSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)
    }
}
