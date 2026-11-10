package dev.efureev.sproutsquad

import dev.efureev.sproutsquad.debug.GalleryCommand
import dev.efureev.sproutsquad.debug.GalleryGameTest
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent

/**
 * Entry point for Sprout Squad's own content (everything outside the original mod's code).
 * Called from the NeoForge @Mod constructor; must not touch the original Paz* content objects here.
 */
object SproutSquad {
    fun init(modBus: IEventBus) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent::class.java) { GalleryCommand.register(it.dispatcher) }

        modBus.addListener(RegisterEvent::class.java) { GalleryGameTest.registerFunction(it) }
        modBus.addListener(RegisterGameTestsEvent::class.java) { GalleryGameTest.registerTest(it) }
    }
}
