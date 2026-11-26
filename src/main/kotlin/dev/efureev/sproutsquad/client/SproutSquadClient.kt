package dev.efureev.sproutsquad.client

import dev.efureev.sproutsquad.client.model.CornCannonModel
import dev.efureev.sproutsquad.client.model.CornCobModel
import dev.efureev.sproutsquad.client.model.DoubleSunflowerModel
import dev.efureev.sproutsquad.client.model.NeedleSpruceModel
import dev.efureev.sproutsquad.client.model.PoppyRattleModel
import dev.efureev.sproutsquad.client.model.PotlingModel
import dev.efureev.sproutsquad.debug.client.Autopilot
import dev.efureev.sproutsquad.registry.SquadEntities
import dev.efureev.sproutsquad.registry.SquadParticles
import joshxviii.plantz.renderer.entity.ProjectileRenderer
import joshxviii.plantz.renderer.entity.plant.PlantRenderer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent

/**
 * Client side of Sprout Squad's own content: model layers, renderers and particle providers.
 * Called from the client @Mod constructor (`PazNeoForgeClient`).
 */
object SproutSquadClient {
    fun init(modBus: IEventBus) {
        Autopilot.init()
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions::class.java) { event ->
            event.registerLayerDefinition(PotlingModel.LAYER_LOCATION) { PotlingModel.createBodyLayer() }
            event.registerLayerDefinition(NeedleSpruceModel.LAYER_LOCATION) { NeedleSpruceModel.createBodyLayer() }
            event.registerLayerDefinition(PoppyRattleModel.LAYER_LOCATION) { PoppyRattleModel.createBodyLayer() }
            event.registerLayerDefinition(DoubleSunflowerModel.LAYER_LOCATION) { DoubleSunflowerModel.createBodyLayer() }
            event.registerLayerDefinition(CornCannonModel.LAYER_LOCATION) { CornCannonModel.createBodyLayer() }
            event.registerLayerDefinition(CornCobModel.LAYER_LOCATION) { CornCobModel.createBodyLayer() }
        }
        modBus.addListener(EntityRenderersEvent.RegisterRenderers::class.java) { event ->
            event.registerEntityRenderer(SquadEntities.POTLING.get()) { PlantRenderer(PotlingModel(it.bakeLayer(PotlingModel.LAYER_LOCATION)), it) }
            event.registerEntityRenderer(SquadEntities.NEEDLE_SPRUCE.get()) { PlantRenderer(NeedleSpruceModel(it.bakeLayer(NeedleSpruceModel.LAYER_LOCATION)), it) }
            event.registerEntityRenderer(SquadEntities.POPPY_RATTLE.get()) { PlantRenderer(PoppyRattleModel(it.bakeLayer(PoppyRattleModel.LAYER_LOCATION)), it) }
            event.registerEntityRenderer(SquadEntities.DOUBLE_SUNFLOWER.get()) { PlantRenderer(DoubleSunflowerModel(it.bakeLayer(DoubleSunflowerModel.LAYER_LOCATION)), it) }
            event.registerEntityRenderer(SquadEntities.CORN_CANNON.get()) { PlantRenderer(CornCannonModel(it.bakeLayer(CornCannonModel.LAYER_LOCATION)), it) }
            event.registerEntityRenderer(SquadEntities.CORN_COB.get()) { ProjectileRenderer(CornCobModel(it.bakeLayer(CornCobModel.LAYER_LOCATION)), it) }
        }
        modBus.addListener(RegisterParticleProvidersEvent::class.java) { event ->
            event.registerSpriteSet(SquadParticles.POPCORN.get()) { sprites -> PopcornParticle.Provider(sprites) }
        }
    }
}
