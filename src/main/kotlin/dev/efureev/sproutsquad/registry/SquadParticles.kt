package dev.efureev.sproutsquad.registry

import joshxviii.plantz.PazMain
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.Registries
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/** Sprout Squad's own particle types. Their client providers are in `client/SproutSquadClient.kt`. */
object SquadParticles {
    val PARTICLE_TYPES: DeferredRegister<ParticleType<*>> = DeferredRegister.create(Registries.PARTICLE_TYPE, PazMain.MODID)

    /** Popcorn puffs scattered by a Corn Cannon's exploding cob. */
    val POPCORN: DeferredHolder<ParticleType<*>, SimpleParticleType> = PARTICLE_TYPES.register("popcorn") { -> SimpleParticleType(false) }

    fun register(modBus: IEventBus) {
        PARTICLE_TYPES.register(modBus)
    }
}
