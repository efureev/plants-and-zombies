package dev.efureev.sproutsquad.debug

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
    private val tests: List<Pair<String, (GameTestHelper) -> Unit>> = listOf(
        "gallery" to GalleryGameTest::run,
        "squad_plants" to SquadPlantsGameTest::run,
    )

    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterEvent::class.java) { event ->
            tests.forEach { (name, test) -> event.register(Registries.TEST_FUNCTION, id(name)) { Consumer<GameTestHelper>(test) } }
        }
        modBus.addListener(RegisterGameTestsEvent::class.java) { event ->
            tests.forEach { (name, _) ->
                val environment = event.registerEnvironment(id(name))
                val function = ResourceKey.create(Registries.TEST_FUNCTION, id(name))
                event.registerTest(id(name), FunctionGameTestInstance(function, TestData(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)))
            }
        }
    }

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, path)
}
