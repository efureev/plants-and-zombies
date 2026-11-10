package joshxviii.plantz.neoforge

import dev.efureev.sproutsquad.client.SproutSquadClient
import joshxviii.plantz.PazClient
import joshxviii.plantz.PazClientNetwork
import joshxviii.plantz.PazMain
import joshxviii.plantz.PazModels
import joshxviii.plantz.PazParticles
import joshxviii.plantz.PazRenderPipelines
import joshxviii.plantz.PazScreens
import joshxviii.plantz.gui.PazOptionsScreen
import joshxviii.plantz.item.model.BatteryFullness
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent

/** Client-only NeoForge entry point (Fabric: ClientModInitializer + ModMenu). */
@Mod(value = PazMain.MODID, dist = [Dist.CLIENT])
class PazNeoForgeClient(modBus: IEventBus, container: ModContainer) {
    init {
        container.registerExtensionPoint(IConfigScreenFactory::class.java, IConfigScreenFactory { _, parent -> PazOptionsScreen(parent) })

        modBus.addListener(FMLClientSetupEvent::class.java) { event -> event.enqueueWork { PazClient.onInitializeClient() } }
        modBus.addListener(RegisterClientPayloadHandlersEvent::class.java) { PazClientNetwork.registerHandlers(it) }

        modBus.addListener(RegisterItemModelsEvent::class.java) { PazModels.registerItemModels(it) }
        modBus.addListener(RegisterRangeSelectItemModelPropertyEvent::class.java) { BatteryFullness.register(it) }
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions::class.java) { PazModels.registerLayerDefinitions(it) }
        modBus.addListener(EntityRenderersEvent.RegisterRenderers::class.java) { PazModels.registerRenderers(it) }
        SproutSquadClient.init(modBus)
        modBus.addListener(RegisterParticleGroupsEvent::class.java) { PazParticles.registerGroups(it) }
        modBus.addListener(RegisterParticleProvidersEvent::class.java) { PazParticles.registerProviders(it) }
        modBus.addListener(RegisterMenuScreensEvent::class.java) { PazScreens.registerMenuScreens(it) }
        modBus.addListener(RegisterGuiLayersEvent::class.java) { PazScreens.registerGuiLayers(it) }
        modBus.addListener(RegisterRenderPipelinesEvent::class.java) { PazRenderPipelines.registerPipelines(it) }
        modBus.addListener(AddClientReloadListenersEvent::class.java) { PazClient.registerClientReloadListeners(it) }
    }
}
