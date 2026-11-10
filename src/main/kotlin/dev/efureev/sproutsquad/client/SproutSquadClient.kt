package dev.efureev.sproutsquad.client

import dev.efureev.sproutsquad.client.model.PotlingModel
import dev.efureev.sproutsquad.registry.SquadEntities
import joshxviii.plantz.renderer.entity.plant.PlantRenderer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent

/**
 * Client side of Sprout Squad's own content: model layers and renderers.
 * Called from the client @Mod constructor (`PazNeoForgeClient`).
 */
object SproutSquadClient {
    fun init(modBus: IEventBus) {
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions::class.java) { event ->
            event.registerLayerDefinition(PotlingModel.LAYER_LOCATION) { PotlingModel.createBodyLayer() }
        }
        modBus.addListener(EntityRenderersEvent.RegisterRenderers::class.java) { event ->
            event.registerEntityRenderer(SquadEntities.POTLING.get()) { PlantRenderer(PotlingModel(it.bakeLayer(PotlingModel.LAYER_LOCATION)), it) }
        }
    }
}
