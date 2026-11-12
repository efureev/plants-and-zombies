package dev.efureev.sproutsquad.debug

import dev.efureev.sproutsquad.fixes.F2GameTests
import dev.efureev.sproutsquad.fixes.F2WorldGameTests
import dev.efureev.sproutsquad.fixes.OriginalBugsGameTests
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
        Test("gaps_head_plant_ticks", OriginalGapsGameTests::headPlantTicks, maxTicks = 200),
        Test("gaps_queued_mail_leaves", OriginalGapsGameTests::queuedMailLeaves),
        Test("gaps_pumpkin_armor_cost", OriginalGapsGameTests::pumpkinArmorCost),
        Test("gaps_config_off_server_thread", OriginalGapsGameTests::configOffServerThread),
        Test("bugs_chomper_spares_unchompable", OriginalBugsGameTests::chomperSparesUnchompable),
        Test("bugs_pumpkin_keeps_plant_attributes", OriginalBugsGameTests::pumpkinKeepsPlantAttributes, maxTicks = 200),
        Test("bugs_minecart_planting_costs_sun", OriginalBugsGameTests::minecartPlantingCostsSun),
        Test("bugs_skates_modifiers_cleared", OriginalBugsGameTests::skatesModifiersCleared, maxTicks = 200),
        Test("bugs_newspaper_anger", OriginalBugsGameTests::newspaperAnger, maxTicks = 200),
        Test("bugs_sun_battery_saved", OriginalBugsGameTests::sunBatterySaved),
        Test("bugs_gravestone_forgets_vanished", OriginalBugsGameTests::gravestoneForgetsVanished),
        Test("bugs_panic_teleport_window", OriginalBugsGameTests::panicTeleportWindow),
        Test("bugs_zombie_unknown_state", OriginalBugsGameTests::zombieUnknownState),
        Test("bugs_block_names", OriginalBugsGameTests::blockNames),
        Test("bugs_shears_follow_ownership", OriginalBugsGameTests::shearsFollowOwnership),
        Test("bugs_grave_buster_needs_grave", OriginalBugsGameTests::graveBusterNeedsGrave),
        Test("bugs_coffee_bean_needs_sleeper", OriginalBugsGameTests::coffeeBeanNeedsSleeper),
        Test("bugs_citron_chases_when_new", OriginalBugsGameTests::citronChasesWhenNew),
        Test("bugs_letter_menu_keeps_its_letter", OriginalBugsGameTests::letterMenuKeepsItsLetter),
        Test("bugs_letter_data_equality", OriginalBugsGameTests::letterDataEquality),
        Test("bugs_conversion_keeps_pumpkin", OriginalBugsGameTests::conversionKeepsPumpkin),
        Test("bugs_seeds_after_required_sun", OriginalBugsGameTests::seedsAfterRequiredSun),
        Test("bugs_flag_healing_saved", OriginalBugsGameTests::flagHealingSaved),
        Test("bugs_mailbox_list_loads_no_chunks", OriginalBugsGameTests::mailboxListLoadsNoChunks),
        Test("bugs_plant_spawn_roll", OriginalBugsGameTests::plantSpawnRoll),
        Test("bugs_plants_are_ambient", OriginalBugsGameTests::plantsAreAmbient),
        Test("bugs_mob_helmet_wears", OriginalBugsGameTests::mobHelmetWears),
        Test("bugs_hero_wave_list_mutable", OriginalBugsGameTests::heroWaveListMutable),
        Test("bugs_worn_plant_snapshot_fresh", OriginalBugsGameTests::wornPlantSnapshotFresh, maxTicks = 200),
        Test("bugs_conversion_keeps_offline_owner", OriginalBugsGameTests::conversionKeepsOfflineOwner),
        Test("bugs_letter_author_limit", OriginalBugsGameTests::letterAuthorLimit),
        Test("bugs_removed_mailbox_unlisted", OriginalBugsGameTests::removedMailboxUnlisted),
        Test("bugs_pumpkin_planting_limit", OriginalBugsGameTests::pumpkinPlantingLimit),
        Test("bugs_glove_break_keeps_plant", OriginalBugsGameTests::gloveBreakKeepsPlant),
        Test("bugs_mailbox_menu_reach", OriginalBugsGameTests::mailboxMenuReach),
        Test("bugs_glove_gets_no_cooldown", OriginalBugsGameTests::gloveGetsNoCooldown),
        Test("bugs_blocker_lets_piercing_through", OriginalBugsGameTests::blockerLetsPiercingThrough),
        Test("bugs_blocker_sends_back_only_shots", OriginalBugsGameTests::blockerSendsBackOnlyShots),
        Test("bugs_watering_can_last_drop", OriginalBugsGameTests::wateringCanLastDrop),
        Test("bugs_dispenser_needs_owner", OriginalBugsGameTests::dispenserNeedsOwner),
        Test("bugs_battery_respects_slot", OriginalBugsGameTests::batteryRespectsSlot),
        Test("bugs_flag_crack_ids", OriginalBugsGameTests::flagCrackIds),
        Test("bugs_villagers_trust_allies", OriginalBugsGameTests::villagersTrustAllies),
        Test("bugs_leader_bonus_only_off_mod_zombies", OriginalBugsGameTests::leaderBonusOnlyOffModZombies),
        Test("bugs_block_knockback_only_for_mod_blockers", OriginalBugsGameTests::blockKnockbackOnlyForModBlockers),
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
