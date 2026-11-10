package dev.efureev.sproutsquad.fixes

import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent

/**
 * Event wiring for phase F2 ("fixes and accessibility", see docs/ROADMAP.md).
 * Most F2 fixes are data-only; this object only registers their GameTests.
 */
object F2Hooks {
    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterEvent::class.java) { event ->
            if (event.registryKey == Registries.TEST_FUNCTION) F2GameTests.registerFunctions(event)
        }
        modBus.addListener(RegisterGameTestsEvent::class.java) { F2GameTests.registerTests(it) }
    }
}
