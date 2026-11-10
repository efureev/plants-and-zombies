package joshxviii.plantz

import joshxviii.plantz.particles.*
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.minecraft.client.particle.CritParticle
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SpellParticle
import net.minecraft.client.particle.SplashParticle

object PazParticles {
    @JvmField
    val ELECTRIC_ARC = ParticleRenderType(pazResource("electric_arc").toString(), "electric_arc")
    @JvmField
    val BEAM = ParticleRenderType(pazResource("beam").toString(), "beam")

    fun registerGroups(event: RegisterParticleGroupsEvent) {
        // particle groups
        event.register(ELECTRIC_ARC) {
            ElectricArcParticleGroup(it)
        }
        event.register(BEAM) {
            BeamParticleGroup(it)
        }

    }

    fun registerProviders(event: RegisterParticleProvidersEvent) {
        // particle providers
        event.registerSpriteSet(PazServerParticles.BUTTER_DRIP) { sprite ->
            SplashParticle.Provider(sprite)
        }

        event.registerSpriteSet(PazServerParticles.CHILLED) { sprite ->
            SplashParticle.Provider(sprite)
        }

        event.registerSpriteSet(PazServerParticles.PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.ICE_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.FIRE_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.ELECTRIC_PEA_HIT) { spriteSet ->
            PeaParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.HYPNO_SPORE) { spriteSet ->
            HypnoSporeParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.SPORE) { spriteSet ->
            SporeParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.SPORE_HIT) {
            spriteSet -> CritParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.FUME_BUBBLE) { spriteSet ->
            FumeBubbleParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.EMBER) { spriteSet ->
            EmberParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.ELECTRIFIED) { spriteSet ->
            ElectrifiedParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.ELECTRIC_ARC) { _ ->
            ElectricArcParticle.Provider()
        }

        event.registerSpriteSet(PazServerParticles.BEAM) { _ ->
            BeamParticle.Provider()
        }

        event.registerSpriteSet(PazServerParticles.ENERGIZED) { spriteSet ->
            EnergizedParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.POWERED_UP) { spriteSet ->
            PoweredUpParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.POP) { sprite ->
            PopParticle.Provider(sprite)
        }

        event.registerSpriteSet(PazServerParticles.SLEEP) { spriteSet ->
            SleepParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.NOTIFY) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        event.registerSpriteSet(PazServerParticles.NEEDS_SUN) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        event.registerSpriteSet(PazServerParticles.NEEDS_WATER) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }
        event.registerSpriteSet(PazServerParticles.NEEDS_TIME) { spriteSet ->
            NotifyParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.NUKE_WAVE) { spriteSet ->
            NukeWaveParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.NUKE_BLAST) { spriteSet ->
            NukeBlastParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.NUKE_BLAST) { spriteSet ->
            NukeBlastParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.NUKE_SMOKE) { spriteSet ->
            NukeSmokeParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.PLASMA_BURST) { spriteSet ->
            PlasmaBurstParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.PAINT_BALL) { spriteSet ->
            PaintBallParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.CONFETTI) { spriteSet ->
            ConfettiParticle.Provider(spriteSet)
        }

        event.registerSpriteSet(PazServerParticles.ZOMBIE_OMEN) { sprite ->
            SpellParticle.Provider(sprite)
        }
    }
}