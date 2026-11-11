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
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
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

    /** How far from a test's position what earlier tests left behind is removed. */
    private const val STRAY_RADIUS = 48.0

    /**
     * The GameTest server lays the tests out side by side, eight in a row, without clearing between
     * batches, and the runner clears only what is inside a test's structure. Many tests work on a strip
     * of ground beside their 1x1x1 "empty" structure, so what they left there (diggers tunnelling off,
     * zombies knocked aside, a lawn mower) walked or drove into the next tests and was targeted
     * instead of their own mobs. On the GameTest server (`runGameTestServer`) each test starts with
     * every entity but players within [STRAY_RADIUS] removed, except inside its own structure: the
     * runner has just cleared that area and placed the structure's own entities there. A test started
     * with `/test` in a normal world leaves everything around it alone: it may be someone's base.
     */
    private fun clearStrays(helper: GameTestHelper) {
        if (helper.level.server !is GameTestServer) return
        val arena = helper.bounds
        val area = AABB(helper.absolutePos(BlockPos.ZERO)).inflate(STRAY_RADIUS)
        helper.level.getEntities(null as Entity?, area) { it !is Player && !arena.intersects(it.boundingBox) }.forEach { it.discard() }
    }

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, path)
}
