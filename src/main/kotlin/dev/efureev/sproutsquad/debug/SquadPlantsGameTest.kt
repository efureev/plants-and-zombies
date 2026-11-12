package dev.efureev.sproutsquad.debug

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazMain
import joshxviii.plantz.PazTags
import joshxviii.plantz.entity.plant.Plant
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.SpawnPlacements

/**
 * Checklist for Sprout Squad's own plants (docs/en/WORKSHOP.md), as a GameTest: every plant in
 * [SquadEntities] must spawn with its declared health, have a sun cost and spawn rules, be in the `plant` tag, have a seed packet
 * recipe, textures (main and `_sleep`), a seed packet icon and en/ru names.
 * Registered in [SquadGameTests].
 */
object SquadPlantsGameTest {
    private val LANGUAGES = listOf("en_us", "ru_ru")

    fun run(helper: GameTestHelper) {
        val problems = mutableListOf<String>()
        val languages = LANGUAGES.associateWith { readJson("/assets/${PazMain.MODID}/lang/$it.json") }

        SquadEntities.plants.forEach { entry ->
            val type = entry.type.get()
            val name = entry.name
            fun problem(text: String) = problems.add("$name: $text")

            val plant = type.create(helper.level, EntitySpawnReason.COMMAND)
            if (plant !is Plant) problem("does not spawn as a Plant")
            else if (plant.maxHealth != entry.attributes.maxHealth.toFloat()) problem("max health ${plant.maxHealth}, expected ${entry.attributes.maxHealth}")
            // the value itself may be tuned in config/sproutsquad/server.json, only check that it is known
            if (PazConfig.getSunCost(type) < 0) problem("no sun cost (not registered in PazConfig)")
            if (!type.builtInRegistryHolder().`is`(PazTags.EntityTypes.PLANT)) problem("not in #${PazMain.MODID}:plant (add it to squad_plants.json)")
            // without spawn rules a structure's spawn_overrides place it anywhere in the piece: on roofs, in water
            if (SpawnPlacements.getPlacementType(type) != SpawnPlacementTypes.ON_GROUND) problem("no spawn rules (SquadEntities registers them)")

            val recipe = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(PazMain.MODID, "${name}_seed_packet"))
            if (helper.level.server.recipeManager.byKey(recipe).isEmpty) problem("no recipe ${recipe.identifier()}")

            val textures = "/assets/${PazMain.MODID}/textures"
            listOf(
                "$textures/entity/plant/$name/$name.png",
                "$textures/entity/plant/$name/${name}_sleep.png",
                "$textures/item/plant_icon/$name.png",
                "/assets/${PazMain.MODID}/models/item/plant_icon/$name.json",
            ).filter { SquadPlantsGameTest::class.java.getResource(it) == null }.forEach { problem("missing $it") }

            languages.forEach { (lang, json) ->
                if (json?.has("entity.${PazMain.MODID}.$name") != true) problem("no name in $lang.json")
            }
        }

        if (problems.isNotEmpty()) {
            helper.fail("Squad plant checklist:\n" + problems.joinToString("\n"))
            return
        }
        PazMain.LOGGER.info("Squad plants GameTest: {} plants checked", SquadEntities.plants.size)
        helper.succeed()
    }

    private fun readJson(path: String): JsonObject? =
        SquadPlantsGameTest::class.java.getResourceAsStream(path)?.reader(Charsets.UTF_8)?.use { JsonParser.parseReader(it).asJsonObject }
}
