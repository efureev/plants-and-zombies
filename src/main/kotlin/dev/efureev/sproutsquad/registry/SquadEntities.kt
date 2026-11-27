package dev.efureev.sproutsquad.registry

import dev.efureev.sproutsquad.content.plant.CornCannon
import dev.efureev.sproutsquad.content.plant.DoubleSunflower
import dev.efureev.sproutsquad.content.plant.NeedleSpruce
import dev.efureev.sproutsquad.content.plant.PoppyRattle
import dev.efureev.sproutsquad.content.plant.Potling
import dev.efureev.sproutsquad.content.projectile.CornCob
import joshxviii.plantz.PazConfig
import joshxviii.plantz.PazMain
import joshxviii.plantz.PazSpawnPlacements
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.SpawnPlacementTypes
import net.minecraft.world.entity.SpawnPlacements
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.level.biome.Biome
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
        val wild: WildSpawn?,
        val entityLimit: Int?,
    )

    /** Where a plant grows wild: a biome spawn for every biome in [biomes] (applied by PazSpawnsBiomeModifier). */
    class WildSpawn(val biomes: TagKey<Biome>, val weight: Int, val minGroupSize: Int = 1, val maxGroupSize: Int = 1)

    private val plantEntries = mutableListOf<PlantEntry>()
    val plants: List<PlantEntry> get() = plantEntries.toList()

    // region Plants
    /** Potling, the mascot: a cheap sun maker and the template for new plants. */
    val POTLING = plant("potling", ::Potling, width = 0.65f, height = 1.0f, sunCost = 3,
        attributes = Plant.Companion.PlantAttributes(maxHealth = 12.0))

    /** Needle Spruce: a three-block mountain spruce firing three needles per volley. */
    val NEEDLE_SPRUCE = plant("needle_spruce", ::NeedleSpruce, width = 0.9f, height = 2.9f, eyeHeight = NeedleSpruce.EYE_HEIGHT,
        sunCost = NeedleSpruce.SUN_COST,
        attributes = Plant.Companion.PlantAttributes(maxHealth = NeedleSpruce.MAX_HEALTH, attackDamage = NeedleSpruce.NEEDLE_DAMAGE,
            followRange = NeedleSpruce.RANGE),
        spawnRules = SpawnPlacements.SpawnPredicate(NeedleSpruce::checkNeedleSpruceSpawnRules),
        wild = WildSpawn(NeedleSpruce.HAS_NEEDLE_SPRUCE, weight = 4, minGroupSize = 1, maxGroupSize = 2))

    /** Poppy Rattle: grows a random plant's seed packet once by day and once by night. */
    val POPPY_RATTLE = plant("poppy_rattle", ::PoppyRattle, width = 0.6f, height = 1.2f, eyeHeight = PoppyRattle.EYE_HEIGHT,
        sunCost = PoppyRattle.SUN_COST,
        entityLimit = PoppyRattle.ENTITY_LIMIT,
        attributes = Plant.Companion.PlantAttributes(maxHealth = PoppyRattle.MAX_HEALTH))

    /** Double Sunflower: two stems, two flowers, twice the Sunflower's sun. */
    val DOUBLE_SUNFLOWER = plant("double_sunflower", ::DoubleSunflower, width = 0.9f, height = 1.5f,
        sunCost = DoubleSunflower.SUN_COST,
        attributes = Plant.Companion.PlantAttributes(maxHealth = DoubleSunflower.MAX_HEALTH))

    /** Corn Cannon: heavy artillery firing its own corn cob as a rocket. */
    val CORN_CANNON = plant("corn_cannon", ::CornCannon, width = CornCannon.WIDTH, height = CornCannon.HEIGHT, eyeHeight = CornCannon.EYE_HEIGHT,
        sunCost = CornCannon.SUN_COST,
        entityLimit = CornCannon.ENTITY_LIMIT,
        attributes = Plant.Companion.PlantAttributes(maxHealth = CornCannon.MAX_HEALTH, followRange = CornCannon.RANGE))
    // endregion

    // region Projectiles
    /** The Corn Cannon's corn cob rocket. */
    val CORN_COB: DeferredHolder<EntityType<*>, EntityType<CornCob>> = projectile("corn_cob", ::CornCob, 0.5f, 0.5f)
    // endregion

    fun register(modBus: IEventBus) {
        ENTITY_TYPES.register(modBus)
        modBus.addListener(EntityAttributeCreationEvent::class.java) { event ->
            plantEntries.forEach { event.put(it.type.get(), it.attributes.apply(Mob.createMobAttributes()).build()) }
        }
        // Common setup runs in parallel across mods; the config map is not thread-safe.
        modBus.addListener(FMLCommonSetupEvent::class.java) { event ->
            event.enqueueWork {
                plantEntries.forEach { entry ->
                    val id = "${PazMain.MODID}:${entry.name}"
                    PazConfig.registerDefaultSunCost(id, entry.sunCost)
                    entry.entityLimit?.let { PazConfig.registerDefaultEntityLimit(id, it) }
                }
            }
        }
        // On the ground like the original's plants: without a rule, a structure's spawn_overrides
        // (the Garden Shack) place the plant anywhere in its piece, on the roof or in the water.
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { event ->
            plantEntries.forEach { entry ->
                @Suppress("UNCHECKED_CAST")
                val type = entry.type.get() as EntityType<Plant>
                event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    entry.spawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE)
                // biome spawn lists, read later by PazSpawnsBiomeModifier when a server starts
                entry.wild?.let { PazSpawnPlacements.addBiomeSpawn(it.biomes, type, it.weight, it.minGroupSize, it.maxGroupSize) }
            }
        }
    }

    /** Seed packets of our plants, appended to the original plant list in the creative tabs. */
    fun addSeedPackets(output: CreativeModeTab.Output) {
        plantEntries.forEach { output.accept(SeedPacketItem.stackFor(it.type.get())) }
    }

    /** A projectile, sized like the original's (`PazEntities.registerProjectile`: no eye height). */
    private fun <T : net.minecraft.world.entity.Entity> projectile(
        name: String,
        factory: EntityType.EntityFactory<T>,
        width: Float,
        height: Float,
    ): DeferredHolder<EntityType<*>, EntityType<T>> = ENTITY_TYPES.register(name) { id ->
        EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(width, height)
            .eyeHeight(0f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    }

    private fun <T : Plant> plant(
        name: String,
        factory: EntityType.EntityFactory<T>,
        width: Float = 0.6f,
        height: Float = 1.0f,
        eyeHeight: Float = height * 0.85f,
        sunCost: Int,
        attributes: Plant.Companion.PlantAttributes = Plant.Companion.PlantAttributes(),
        spawnRules: SpawnPlacements.SpawnPredicate<Plant> = SpawnPlacements.SpawnPredicate(Plant::checkPlantSpawnRules),
        wild: WildSpawn? = null,
        entityLimit: Int? = null,
    ): DeferredHolder<EntityType<*>, EntityType<T>> {
        val holder = ENTITY_TYPES.register(name) { id ->
            // AMBIENT like the original's plants (PazEntities.registerPlant)
            EntityType.Builder.of(factory, MobCategory.AMBIENT)
                .canSpawnFarFromPlayer()
                .sized(width, height)
                .eyeHeight(eyeHeight)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
        }
        plantEntries += PlantEntry(name, holder, sunCost, attributes, spawnRules, wild, entityLimit)
        return holder
    }
}
