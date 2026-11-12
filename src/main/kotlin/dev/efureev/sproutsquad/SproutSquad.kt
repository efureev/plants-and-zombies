package dev.efureev.sproutsquad

import dev.efureev.sproutsquad.debug.GalleryCommand
import dev.efureev.sproutsquad.debug.SquadGameTests
import dev.efureev.sproutsquad.fixes.BalloonFlight
import dev.efureev.sproutsquad.fixes.MarigoldColors
import dev.efureev.sproutsquad.fixes.WornItemModifiers
import dev.efureev.sproutsquad.fixes.ZombieGoatSpawns
import dev.efureev.sproutsquad.machines.MachineReinforcements
import dev.efureev.sproutsquad.machines.PlayerMachines
import dev.efureev.sproutsquad.registry.SquadAttachments
import dev.efureev.sproutsquad.registry.SquadEntities
import dev.efureev.sproutsquad.structures.StructureSpawns
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent

/**
 * Entry point for Sprout Squad's own content (everything outside the original mod's code).
 * Called from the NeoForge @Mod constructor; must not touch the original Paz* content objects here.
 */
object SproutSquad {
    fun init(modBus: IEventBus) {
        SquadEntities.register(modBus)
        SquadAttachments.register(modBus)
        PlayerMachines.register()
        MachineReinforcements.register(modBus)
        ZombieGoatSpawns.register(modBus)
        StructureSpawns.register(modBus)
        BalloonFlight.register()
        MarigoldColors.register()
        WornItemModifiers.register()

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent::class.java) { GalleryCommand.register(it.dispatcher) }

        SquadGameTests.register(modBus)
    }
}
