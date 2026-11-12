package dev.efureev.sproutsquad.registry

import dev.efureev.sproutsquad.content.plant.Potling
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazMain
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Sprout Squad's own entities, registered through DeferredRegister (unlike the original mod's
 * direct Registry.register calls).
 *
 * Adding a plant (full checklist in docs/en/WORKSHOP.md):
 * 1. a class in `content/plant/` (copy [Potling]);
 * 2. one `plant(...)` line below;
 * 3. model + renderer in `client/SproutSquadClient.kt`;
 * 4. textures, seed packet icon, translations, recipe, the `squad_plants` tag.
 */
object SquadEntities {
    val ENTITY_TYPES: DeferredRegister<EntityType<*>> = DeferredRegister.create(Registries.ENTITY_TYPE, PazMain.MODID)

    class PlantEntry(
        val name: String,
        val type: DeferredHolder<EntityType<*>, out EntityType<out Plant>>,
        val sunCost: Int,
        val attributes: Plant.Companion.PlantAttributes,
        val spawnRules: SpawnPlacements.SpawnPredicate<Plant>,
    )

    private val plantEntries = mutableListOf<PlantEntry>()
    val plants: List<PlantEntry> get() = plantEntries.toList()

    // region Plants
    /** Potling, the mascot: a cheap sun maker and the template for new plants. */
    val POTLING = plant("potling", ::Potling, width = 0.65f, height = 1.0f, sunCost = 3,
        attributes = Plant.Companion.PlantAttributes(maxHealth = 12.0))
    // endregion

    fun register(modBus: IEventBus) {
        ENTITY_TYPES.register(modBus)
        modBus.addListener(EntityAttributeCreationEvent::class.java) { event ->
            plantEntries.forEach { event.put(it.type.get(), it.attributes.apply(Mob.createMobAttributes()).build()) }
        }
        // Common setup runs in parallel across mods; the config map is not thread-safe.
        modBus.addListener(FMLCommonSetupEvent::class.java) { event ->
            event.enqueueWork { plantEntries.forEach { PazConfig.registerDefaultSunCost("${PazMain.MODID}:${it.name}", it.sunCost) } }
        }
        // On the ground like the original's plants: without a rule, a structure's spawn_overrides
        // (the Garden Shack) place the plant anywhere in its piece, on the roof or in the water.
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { event ->
            plantEntries.forEach { entry ->
                @Suppress("UNCHECKED_CAST")
                event.register(entry.type.get() as EntityType<Plant>, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    entry.spawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)
            }
        }
    }

    /** Seed packets of our plants, appended to the original plant list in the creative tabs. */
    fun addSeedPackets(output: CreativeModeTab.Output) {
        plantEntries.forEach { output.accept(SeedPacketItem.stackFor(it.type.get())) }
    }

    private fun <T : Plant> plant(
        name: String,
        factory: EntityType.EntityFactory<T>,
        width: Float = 0.6f,
        height: Float = 1.0f,
        sunCost: Int,
        attributes: Plant.Companion.PlantAttributes = Plant.Companion.PlantAttributes(),
        spawnRules: SpawnPlacements.SpawnPredicate<Plant> = SpawnPlacements.SpawnPredicate(Plant::checkPlantSpawnRules),
    ): DeferredHolder<EntityType<*>, EntityType<T>> {
        val holder = ENTITY_TYPES.register(name) { id ->
            EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(width, height)
                .eyeHeight(height * 0.85f)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        }
        plantEntries += PlantEntry(name, holder, sunCost, attributes, spawnRules)
        return holder
    }
}
