package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazEffects
import joshxviii.plantz.PazEntities
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.entity.gnome.GnomeVariant
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.FunctionGameTestInstance
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestData
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.attribute.EnvironmentAttributes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.BrewingInput
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.Consumer

/**
 * GameTests for phase F2: the new loot tables, recipes and brewing load and roll what they should.
 * Runs with `./gradlew runGameTestServer`.
 */
object F2GameTests {
    private val ENVIRONMENT = id("f2")
    private val DATA = id("f2_data")

    /** Rolls per randomized check; every checked drop has a chance of at least 3%, so a miss is ~1e-4 or less. */
    private const val ROLLS = 300

    private fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(PazMain.MODID, path)
    private fun function(id: Identifier): ResourceKey<Consumer<GameTestHelper>> = ResourceKey.create(Registries.TEST_FUNCTION, id)

    fun registerFunctions(event: RegisterEvent) {
        event.register(Registries.TEST_FUNCTION, DATA) { Consumer<GameTestHelper>(::data) }
    }

    fun registerTests(event: RegisterGameTestsEvent) {
        val environment = event.registerEnvironment(ENVIRONMENT)
        event.registerTest(DATA, FunctionGameTestInstance(function(DATA), TestData(environment, Identifier.withDefaultNamespace("empty"), 100, 0, true)))
    }

    private fun data(helper: GameTestHelper) {
        val level = helper.level
        val server = level.server
        val problems = mutableListOf<String>()

        // Every new or changed loot table must load (a broken file silently turns into LootTable.EMPTY).
        val tables = listOf(
            "raid/mail_reward_bonus", "raid/mail_reward_disco", "raid/mail_reward_zomboween", "raid/mail_reward_zomboss",
            "raid/army/treasure", "raid/league/treasure", "raid/disco/treasure", "raid/winter/treasure",
            "raid/zomboween/treasure", "raid/zomboween/seeds", "raid/zomboss/treasure",
            "entities/backup_dancer", "entities/imp", "entities/pirate_captain",
            "entities/pirate_captain_ghost", "entities/zombat", "entities/zombie_goat", "entities/gnome", "entities/grave_digger",
            "blocks/smooth_brainz_alloy_block", "blocks/lawn_flamingo",
        )
        for (path in tables) if (lootTable(server, path) === LootTable.EMPTY) problems += "loot table $path is missing or broken"

        // Chest-style rewards (mailbox rolls them with the CHEST parameter set).
        val chest = LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
            .create(LootContextParamSets.CHEST)
        fun rollChest(path: String) = (1..ROLLS).flatMap { lootTable(server, path).getRandomItems(chest) }
        expect(problems, "raid/mail_reward_bonus", rollChest("raid/mail_reward_bonus"), "brainzium") { it.isOf(PazItems.BRAINZIUM) }
        expect(problems, "raid/disco/treasure", rollChest("raid/disco/treasure"), "party hat") { it.isOf(PazItems.PARTY_HAT) }
        for (path in listOf("raid/winter/treasure", "raid/zomboween/treasure", "raid/zomboss/treasure")) {
            if (rollChest(path).isEmpty()) problems += "$path rolled nothing"
        }

        // Mob drops: only for kills by a player or a plant (#sproutsquad:drop_loot_when_killed_by).
        val plant = helper.spawnWithNoFreeWill(PazEntities.PEA_SHOOTER, BlockPos(2, 1, 0))
        val gnome = helper.spawnWithNoFreeWill(PazEntities.GNOME, BlockPos(0, 1, 2))
        gnome.variant = GnomeVariant.RED
        fun rollMob(path: String, attacker: LivingEntity?): List<ItemStack> {
            val source = if (attacker != null) level.damageSources().mobAttack(attacker) else level.damageSources().generic()
            val params = LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, gnome as Entity)
                .withParameter(LootContextParams.ORIGIN, gnome.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.entity)
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.directEntity)
                .create(LootContextParamSets.ENTITY)
            return (1..ROLLS).flatMap { lootTable(server, path).getRandomItems(params) }
        }
        val gnomeDrops = rollMob("entities/gnome", plant)
        expect(problems, "entities/gnome", gnomeDrops, "red garden gnome") { it.isId("red_garden_gnome") }
        if (gnomeDrops.any { !it.isId("red_garden_gnome") }) problems += "a red gnome dropped a statue of another colour"
        if (rollMob("entities/backup_dancer", null).isNotEmpty()) problems += "backup dancer drops loot without a player or plant kill"
        expect(problems, "entities/backup_dancer", rollMob("entities/backup_dancer", plant), "party hat") { it.isOf(PazItems.PARTY_HAT) }
        expect(problems, "entities/grave_digger", rollMob("entities/grave_digger", plant), "brainz flag") { it.isId("brainz_flag") }
        expect(problems, "entities/imp", rollMob("entities/imp", plant), "seed packet") { it.isOf(PazItems.SEED_PACKET) }
        expect(problems, "entities/pirate_captain_ghost", rollMob("entities/pirate_captain_ghost", plant), "phantom membrane") { it.isOf(Items.PHANTOM_MEMBRANE) }
        expect(problems, "entities/zombat", rollMob("entities/zombat", plant), "leather") { it.isOf(Items.LEATHER) }
        expect(problems, "entities/zombie_goat", rollMob("entities/zombie_goat", plant), "goat horn") {
            it.isOf(Items.GOAT_HORN) && it.has(DataComponents.INSTRUMENT)
        }
        plant.discard()
        gnome.discard()

        // Recipes.
        val recipes = server.recipeManager
        for (path in listOf("smooth_brainz_alloy_block", "smooth_brainz_alloy_block_stonecutting", "taco_from_vanilla")) {
            if (recipes.byKey(ResourceKey.create(Registries.RECIPE, id(path))).isEmpty) problems += "recipe $path is missing"
        }
        val brews: Map<Item, Holder<Potion>> = mapOf(
            Items.RED_MUSHROOM to PazEffects.HYPNOTIZE_POTION,
            Items.HONEYCOMB to PazEffects.BUTTERED_POTION,
            BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lightning_rod")) to PazEffects.ELECTRIFIED_POTION,
        )
        for ((reagent, potion) in brews) {
            for (bottle in listOf(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                val input = BrewingInput(PotionContents.createItemStack(bottle, Potions.AWKWARD), ItemStack(reagent))
                val result = recipes.getRecipeFor(RecipeType.BREWING, input, level).map { it.value().assemble(input) }.orElse(ItemStack.EMPTY)
                if (!result.isOf(bottle) || result.get(DataComponents.POTION_CONTENTS)?.`is`(potion) != true) {
                    problems += "brewing ${bottle.id()} + ${reagent.id()} gave ${result.item.id()} instead of ${potion.registeredName}"
                }
            }
            val splash = BrewingInput(PotionContents.createItemStack(Items.POTION, potion), ItemStack(Items.GUNPOWDER))
            val splashResult = recipes.getRecipeFor(RecipeType.BREWING, splash, level).map { it.value().assemble(splash) }.orElse(ItemStack.EMPTY)
            if (!splashResult.isOf(Items.SPLASH_POTION)) problems += "gunpowder does not turn ${potion.registeredName} into a splash potion"
        }

        // Jurassic Period spawns use the 26.x attribute format (legacy "spawners" is ignored by the codec).
        val jurassic = server.registryAccess().lookupOrThrow(Registries.BIOME).getValue(id("jurassic_period"))
        val spawns = jurassic?.attributes?.applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)
        if (spawns == null || spawns.getMobsToSpawn(MobCategory.CREATURE).isEmpty) {
            problems += "jurassic_period has no creature spawns"
        } else if (spawns.getMobsToSpawn(MobCategory.MONSTER).unwrap().none { it.value().type() == EntityTypes.OCELOT }) {
            problems += "jurassic_period lost its ocelot spawn"
        }

        if (problems.isNotEmpty()) {
            helper.fail(problems.joinToString("; "))
            return
        }
        helper.succeed()
    }

    private fun lootTable(server: MinecraftServer, path: String): LootTable =
        server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id(path)))

    private fun expect(problems: MutableList<String>, table: String, drops: List<ItemStack>, what: String, test: (ItemStack) -> Boolean) {
        if (drops.none(test)) problems += "$table never dropped $what in $ROLLS rolls"
    }

    private fun ItemStack.isOf(item: Item): Boolean = this.item == item
    private fun ItemStack.isId(path: String): Boolean = BuiltInRegistries.ITEM.getKey(item) == id(path)
    private fun Item.id(): String = BuiltInRegistries.ITEM.getKey(this).toString()
}
