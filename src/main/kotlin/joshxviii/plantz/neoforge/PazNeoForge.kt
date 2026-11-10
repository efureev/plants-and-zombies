package joshxviii.plantz.neoforge

import dev.efureev.sproutsquad.SproutSquad
import joshxviii.plantz.PazItems
import joshxviii.plantz.PazMain
import joshxviii.plantz.PazNetwork
import joshxviii.plantz.PazRegistries
import joshxviii.plantz.PazSpawnPlacements
import joshxviii.plantz.PazTerraBlender
import joshxviii.plantz.pazResource
import net.minecraft.resources.Identifier
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent
import net.neoforged.neoforge.registries.GameData
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent

/**
 * NeoForge entry point. The original content objects register themselves directly with
 * `Registry.register`; NeoForge keeps every registry unfrozen for the whole RegisterEvent
 * phase, so they are initialized during the *last* RegisterEvent, when NeoForge and every
 * other mod have already registered the entries our content may reference
 * (e.g. NeoForge's swim_speed attribute used by every LivingEntity).
 */
@Mod(PazMain.MODID)
class PazNeoForge(modBus: IEventBus, container: ModContainer) {
    private var lastRegistry: Identifier? = null

    init {
        SproutSquad.init(modBus)

        modBus.addListener(RegisterEvent::class.java) { event ->
            val last = lastRegistry ?: GameData.getRegistrationOrder().last().also { lastRegistry = it }
            if (event.registryKey.identifier() == last) {
                PazMain.onInitialize()
            }
            event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, pazResource("spawns")) { PazSpawnsBiomeModifier.CODEC }
        }

        modBus.addListener(EntityAttributeCreationEvent::class.java) { PazPlatform.onEntityAttributeCreation(it) }
        modBus.addListener(RegisterSpawnPlacementsEvent::class.java) { PazSpawnPlacements.registerSpawnPlacements(it) }
        modBus.addListener(NewDatapackRegistryEvent::class.java) { PazRegistries.registerDataPackRegistries(it) }
        modBus.addListener(RegisterPayloadHandlersEvent::class.java) { PazNetwork.registerPayloads(it.registrar("1")) }
        modBus.addListener(RegisterTooltipAppendersEvent::class.java) { PazItems.registerTooltipAppenders(it) }
        modBus.addListener(ModifyDefaultComponentsEvent::class.java) { PazItems.modifyDefaultComponents(it) }

        modBus.addListener(FMLCommonSetupEvent::class.java) { event ->
            if (ModList.get().isLoaded("terrablender")) {
                event.enqueueWork { PazTerraBlender.register() }
            }
        }
    }
}
