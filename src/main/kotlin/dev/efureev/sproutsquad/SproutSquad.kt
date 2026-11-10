package dev.efureev.sproutsquad

import dev.efureev.sproutsquad.debug.GalleryCommand
import dev.efureev.sproutsquad.debug.SquadGameTests
import dev.efureev.sproutsquad.registry.SquadEntities
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

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent::class.java) { GalleryCommand.register(it.dispatcher) }

        SquadGameTests.register(modBus)
    }
}
