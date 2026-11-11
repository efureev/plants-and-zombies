package dev.efureev.sproutsquad.debug

import dev.efureev.sproutsquad.fixes.F2GameTests
import dev.efureev.sproutsquad.fixes.F2WorldGameTests
import dev.efureev.sproutsquad.fixes.OriginalGapsGameTests
import dev.efureev.sproutsquad.machines.MachinesGameTests
import dev.efureev.sproutsquad.structures.StructuresGameTests
import joshxviii.plantz.PazMain
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.FunctionGameTestInstance
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.GameTestServer
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.AABB
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
    /** [structure] is the test's arena; the framework loads its whole area before the test starts. */
    private class Test(val name: String, val run: (GameTestHelper) -> Unit, val maxTicks: Int = 100,
                       val structure: Identifier = Identifier.withDefaultNamespace("empty"))

    private val tests = listOf(
        Test("gallery", GalleryGameTest::run, structure = id("gametest/gallery_area")),
        Test("squad_plants", SquadPlantsGameTest::run),
        Test("f2_data", F2GameTests::data),
        Test("f2_squash", F2GameTests::squash, maxTicks = 600),
        Test("f2_zombie_goat", F2WorldGameTests::zombieGoat),
        Test("f2_balloon_flight", F2WorldGameTests::balloonFlight),
        Test("gaps_seed_timer", OriginalGapsGameTests::seedTimer, maxTicks = 400),
        Test("gaps_baby_zombies", OriginalGapsGameTests::babyZombies),
        Test("gaps_flint_and_steel", OriginalGapsGameTests::flintAndSteel),
        Test("gaps_plant_cooldown", OriginalGapsGameTests::plantCooldown),
        Test("gaps_digger_mines", OriginalGapsGameTests::diggerMines, maxTicks = 600),
        Test("gaps_digger_walks_around", OriginalGapsGameTests::diggerWalksAround, maxTicks = 400),
        Test("gaps_marigold_dye", OriginalGapsGameTests::marigoldDye),
        Test("gaps_citron_ball", OriginalGapsGameTests::citronBall),
        Test("machines_build", MachinesGameTests::build),
        Test("machines_allies", MachinesGameTests::allies, maxTicks = 400),
        Test("machines_turret", MachinesGameTests::turret, maxTicks = 1200),
        Test("machines_electro_turret", MachinesGameTests::electroTurret, maxTicks = 1200),
        Test("machines_mower", MachinesGameTests::mower, maxTicks = 400),
        Test("structures_templates", StructuresGameTests::templates),
        Test("structures_garden_shack", StructuresGameTests::gardenShack, structure = id("garden_shack/oak")),
        Test("structures_robo_junkyard", StructuresGameTests::roboJunkyard, maxTicks = 400, structure = id("robo_junkyard/yard")),
    )

    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterEvent::class.java) { event ->
            tests.forEach { test ->
                event.register(Registries.TEST_FUNCTION, id(test.name)) { Consumer<GameTestHelper> { helper -> clearStrays(helper); test.run(helper) } }
            }
        }
        modBus.addListener(RegisterGameTestsEvent::class.java) { event ->
            tests.forEach { test ->
                val environment = event.registerEnvironment(id(test.name))
                val function = ResourceKey.create(Registries.TEST_FUNCTION, id(test.name))
                event.registerTest(id(test.name), FunctionGameTestInstance(function, TestData(environment, test.structure, test.maxTicks, 0, true)))
            }
        }
    }

    /** How far from a test's position the mobs that earlier tests left are removed. */
    private const val STRAY_RADIUS = 48.0

    /**
     * All batches run at the same spot, and the runner clears only what is inside a test's
     * structure: mobs that earlier tests left around it (diggers tunnelling off, zombies knocked
     * aside) walked into later tests and were targeted instead of the test's own mobs. On the
     * GameTest server (`runGameTestServer`) each test starts with them removed; the tests with a
     * template as their arena place it again themselves. A test started with `/test` in a normal
     * world leaves the mobs around it alone: they may be someone's villagers or pets.
     */
    private fun clearStrays(helper: GameTestHelper) {
        if (helper.level.server !is GameTestServer) return
        val area = AABB(helper.absolutePos(BlockPos.ZERO)).inflate(STRAY_RADIUS)
        helper.level.getEntitiesOfClass(Mob::class.java, area).forEach { it.discard() }
    }

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, path)
}
