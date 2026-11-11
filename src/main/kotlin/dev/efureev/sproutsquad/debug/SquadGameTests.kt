package dev.efureev.sproutsquad.debug

import dev.efureev.sproutsquad.fixes.F2GameTests
import dev.efureev.sproutsquad.machines.MachinesGameTests
import joshxviii.plantz.PazMain
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.FunctionGameTestInstance
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.Consumer

/**
 * All of Sprout Squad's GameTests, run by `./gradlew runGameTestServer`.
 * Each test gets its own environment (same id as the test), so it runs in its own batch: the
 * gallery spawns a wide area of mobs and must not overlap another test's structure.
 * An environment id can be registered only once.
 */
object SquadGameTests {
    private class Test(val name: String, val run: (GameTestHelper) -> Unit, val maxTicks: Int = 100)

    private val tests = listOf(
        Test("gallery", GalleryGameTest::run),
        Test("squad_plants", SquadPlantsGameTest::run),
        Test("f2_data", F2GameTests::data),
        Test("f2_squash", F2GameTests::squash, maxTicks = 600),
        Test("machines_build", MachinesGameTests::build),
        Test("machines_allies", MachinesGameTests::allies, maxTicks = 400),
        Test("machines_turret", MachinesGameTests::turret, maxTicks = 1200),
        Test("machines_electro_turret", MachinesGameTests::electroTurret, maxTicks = 1200),
        Test("machines_mower", MachinesGameTests::mower, maxTicks = 400),
    )

    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterEvent::class.java) { event ->
            tests.forEach { test -> event.register(Registries.TEST_FUNCTION, id(test.name)) { Consumer<GameTestHelper>(test.run) } }
        }
        modBus.addListener(RegisterGameTestsEvent::class.java) { event ->
            tests.forEach { test ->
                val environment = event.registerEnvironment(id(test.name))
                val function = ResourceKey.create(Registries.TEST_FUNCTION, id(test.name))
                event.registerTest(id(test.name), FunctionGameTestInstance(function, TestData(environment, Identifier.withDefaultNamespace("empty"), test.maxTicks, 0, true)))
            }
        }
    }

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, path)
}
