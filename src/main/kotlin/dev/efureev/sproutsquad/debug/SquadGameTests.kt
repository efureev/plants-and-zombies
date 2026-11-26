package dev.efureev.sproutsquad.debug

import dev.efureev.sproutsquad.content.plant.DoubleSunflowerGameTests
import dev.efureev.sproutsquad.content.plant.CornCannonGameTests
import dev.efureev.sproutsquad.content.plant.NeedleSpruceGameTests
import dev.efureev.sproutsquad.content.plant.PoppyRattleGameTests
import dev.efureev.sproutsquad.fixes.F2GameTests
import dev.efureev.sproutsquad.fixes.F2WorldGameTests
import dev.efureev.sproutsquad.fixes.OriginalBugsGameTests
import dev.efureev.sproutsquad.fixes.OriginalGapsGameTests
import dev.efureev.sproutsquad.fixes.PortGameTests
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
        Test("bugs_wearer_gone_drops_plant", OriginalBugsGameTests::wearerGoneDropsPlant),
        Test("bugs_riding_wearer_plant_follows", OriginalBugsGameTests::ridingWearerPlantFollows),
        Test("bugs_dispenser_limit_and_spot", OriginalBugsGameTests::dispenserLimitAndSpot),
        Test("needle_spruce_stats", NeedleSpruceGameTests::stats),
        Test("needle_spruce_face", NeedleSpruceGameTests::face),
        Test("needle_spruce_volley", NeedleSpruceGameTests::volley, maxTicks = 400, structure = id("gametest/lane")),
        Test("needle_spruce_out_of_range", NeedleSpruceGameTests::outOfRange, maxTicks = 400, structure = id("gametest/lane")),
        Test("needle_spruce_lives_on_snow", NeedleSpruceGameTests::livesOnSnow, structure = id("gametest/lane")),
        Test("needle_spruce_needs_headroom", NeedleSpruceGameTests::needsHeadroom, structure = id("gametest/lane")),
        Test("needle_spruce_powder_snow", NeedleSpruceGameTests::powderSnow, structure = id("gametest/lane")),
        Test("needle_spruce_spawns", NeedleSpruceGameTests::spawns, structure = id("gametest/lane")),
        Test("needle_spruce_stump_on_death", NeedleSpruceGameTests::stumpOnDeath, structure = id("gametest/lane")),
        Test("needle_spruce_killed_by_zombie", NeedleSpruceGameTests::killedByZombie, structure = id("gametest/lane")),
        Test("needle_spruce_stump_replaces_snow_layer", NeedleSpruceGameTests::stumpReplacesSnowLayer, structure = id("gametest/lane")),
        Test("needle_spruce_no_stump_in_water_or_on_slab", NeedleSpruceGameTests::noStumpInWaterOrOnSlab, structure = id("gametest/lane")),
        Test("needle_spruce_no_stump_without_death", NeedleSpruceGameTests::noStumpWithoutDeath, structure = id("gametest/lane")),
        Test("needle_spruce_stump_on_farmland_path_and_mud", NeedleSpruceGameTests::stumpOnFarmlandPathAndMud, structure = id("gametest/lane")),
        Test("needle_spruce_stands_on_its_stump", NeedleSpruceGameTests::standsOnItsStumpOnlyWithAStump, structure = id("gametest/lane")),
        Test("needle_spruce_stump_loot", NeedleSpruceGameTests::stumpLoot, structure = id("gametest/lane")),
        Test("poppy_rattle_stats", PoppyRattleGameTests::stats),
        Test("poppy_rattle_pool", PoppyRattleGameTests::pool),
        Test("poppy_rattle_phases", PoppyRattleGameTests::phases),
        Test("poppy_rattle_skipped_night", PoppyRattleGameTests::skippedNight),
        Test("poppy_rattle_time_rewind", PoppyRattleGameTests::timeRewind),
        Test("poppy_rattle_capacity", PoppyRattleGameTests::capacity),
        Test("poppy_rattle_worn", PoppyRattleGameTests::worn),
        Test("poppy_rattle_collect", PoppyRattleGameTests::collect),
        Test("poppy_rattle_drops", PoppyRattleGameTests::drops),
        Test("poppy_rattle_saved", PoppyRattleGameTests::saved),
        Test("poppy_rattle_night_texture", PoppyRattleGameTests::nightTexture),
        Test("poppy_rattle_entity_limit_fallback", PoppyRattleGameTests::entityLimitFallback),
        Test("poppy_rattle_asleep_waits", PoppyRattleGameTests::asleepWaits),
        Test("poppy_rattle_full_owes_nothing", PoppyRattleGameTests::fullOwesNothing),
        Test("poppy_rattle_look", PoppyRattleGameTests::look),
        Test("poppy_rattle_under_low_ceiling", PoppyRattleGameTests::underLowCeiling),
        Test("double_sunflower_stats", DoubleSunflowerGameTests::stats),
        Test("double_sunflower_two_suns", DoubleSunflowerGameTests::twoSuns),
        Test("double_sunflower_sun_not_in_wall", DoubleSunflowerGameTests::sunNotInWall),
        Test("double_sunflower_sun_under_low_ceiling", DoubleSunflowerGameTests::sunUnderLowCeiling),
        Test("double_sunflower_recipe", DoubleSunflowerGameTests::recipe),
        Test("double_sunflower_mutation", DoubleSunflowerGameTests::mutation),
        Test("corn_cannon_stats", CornCannonGameTests::stats),
        Test("corn_cannon_face", CornCannonGameTests::face),
        Test("corn_cannon_fires", CornCannonGameTests::fires, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_dead_zone", CornCannonGameTests::deadZone, maxTicks = 600, structure = id("gametest/lane")),
        Test("corn_cannon_spares_friends", CornCannonGameTests::sparesFriends, structure = id("gametest/lane")),
        Test("corn_cannon_wall_in_front", CornCannonGameTests::wallInFront, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_cob_after_reload", CornCannonGameTests::cobAfterReload, structure = id("gametest/lane")),
        Test("corn_cannon_covered_target", CornCannonGameTests::coveredTarget, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_spares_allies", CornCannonGameTests::sparesAllies, structure = id("gametest/lane")),
        Test("corn_cannon_cancelled_shot", CornCannonGameTests::cancelledShot, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_cob_growth", CornCannonGameTests::cobGrowth),
        Test("corn_cannon_blast_behind_wall", CornCannonGameTests::blastBehindWall, structure = id("gametest/lane")),
        Test("corn_cannon_scaled_blast", CornCannonGameTests::scaledBlast),
        Test("corn_cannon_hits_target_block", CornCannonGameTests::hitsTargetBlock, maxTicks = 100, structure = id("gametest/lane")),
        Test("corn_cannon_leaf_roof", CornCannonGameTests::leafRoof, maxTicks = 300, structure = id("gametest/lane")),
        Test("corn_cannon_zombie_in_water", CornCannonGameTests::zombieInWater, maxTicks = 600, structure = id("gametest/lane")),
        Test("corn_cannon_under_roof", CornCannonGameTests::cannonUnderRoof, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_scale_saved", CornCannonGameTests::scaleSaved),
        Test("corn_cannon_scaffolding_roof", CornCannonGameTests::scaffoldingRoof, maxTicks = 300, structure = id("gametest/lane")),
        Test("corn_cannon_misses_drop_target", CornCannonGameTests::missesDropTarget, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_cob_keeps_speed_in_water", CornCannonGameTests::cobKeepsSpeedInWater, structure = id("gametest/lane")),
        Test("corn_cannon_blocked_blast_is_no_miss", CornCannonGameTests::blockedBlastIsNoMiss, structure = id("gametest/lane")),
        Test("corn_cannon_overhang_misses", CornCannonGameTests::overhangMisses, structure = id("gametest/lane")),
        Test("corn_cannon_ignored_targets_forgotten", CornCannonGameTests::ignoredTargetsForgotten, maxTicks = 300, structure = id("gametest/lane")),
        Test("corn_cannon_scaffolding_over_cannon", CornCannonGameTests::scaffoldingOverCannon, maxTicks = 400, structure = id("gametest/lane")),
        Test("corn_cannon_blocked_path_not_fired", CornCannonGameTests::blockedPathNotFired, structure = id("gametest/lane")),
        Test("corn_cannon_recipe", CornCannonGameTests::recipe),
        Test("corn_cannon_not_from_poppy_rattle", CornCannonGameTests::notFromPoppyRattle),
        Test("port_saved_player_loads", PortGameTests::savedPlayerLoads),
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
