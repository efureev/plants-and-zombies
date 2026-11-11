package dev.efureev.sproutsquad.machines

import joshxviii.plantz.PazEntities
import joshxviii.plantz.entity.zombie.ZombieRobot
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent

/**
 * Battle machines never come as reinforcements.
 *
 * Machines are zombies to the game: when one is hit on Hard it may call a reinforcement of its own type
 * at a random spot 7–40 blocks away. The chance comes from the vanilla zombie setup (a random base and,
 * for some, a leader bonus), and machines had no spawn rules, so that turret or drone appeared in the air
 * or underground, and a player's machine called a hostile one (the reinforcement is not the player's).
 *
 * A reinforcement is placed only where the type's spawn rule allows it, so the machines' rule refuses
 * [EntitySpawnReason.REINFORCEMENT]. This stops it where it happens, whatever the chance, for every spawn
 * reason and for machines saved in existing worlds. Nothing else spawns machines through spawn rules:
 * blueprints, Engineers, raids and structures place them directly.
 */
object MachineReinforcements {
    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { event ->
            // read the Paz* objects only here: they are filled from the last RegisterEvent (neoforge/PazNeoForge.kt)
            neverAsReinforcement(event, PazEntities.ZOMBIE_TURRET)
            neverAsReinforcement(event, PazEntities.ELECTRO_TURRET)
            neverAsReinforcement(event, PazEntities.ZOMBIE_DRONE)
        }
    }

    private fun <T : ZombieRobot> neverAsReinforcement(event: RegisterSpawnPlacementsEvent, type: EntityType<T>) {
        event.register(type, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            { _, _, reason, _, _ -> reason != EntitySpawnReason.REINFORCEMENT }, RegisterSpawnPlacementsEvent.Operation.REPLACE)
    }
}
