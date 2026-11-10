package joshxviii.plantz.particles

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.RisingParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.RandomSource

class PoweredUpParticle private constructor(
    level: ClientLevel, x: Double, y: Double, z: Double, xd: Double, yd: Double, zd: Double,
    private val sprites: SpriteSet
) : RisingParticle(level, x, y, z, xd, yd, zd, sprites.first()) {

    init {
        quadSize = 0.2f
        alpha = 0.5f
        lifetime = random.nextInt(4, 9)
        setSpriteFromAge(sprites)
    }

    override fun tick() {
        xo = x
        yo = y
        zo = z
        xd = 0.0
        zd = 0.0
        yd += 0.001
        move(xd, yd, zd)
        setSpriteFromAge(sprites)
        if (age++ >= lifetime) remove()
    }

    public override fun getLayer(): Layer {
        return Layer.TRANSLUCENT
    }

    override fun move(xa: Double, ya: Double, za: Double) {
        this.boundingBox = this.boundingBox.move(xa, ya, za)
        this.setLocationFromBoundingbox()
    }

    override fun getQuadSize(a: Float): Float {
        return this.quadSize
    }

    public override fun getLightCoords(a: Float): Int {
        return LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), 1.0f - (this.age + a) / this.lifetime)
    }

    class Provider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            options: SimpleParticleType,
            level: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xAux: Double,
            yAux: Double,
            zAux: Double,
            random: RandomSource
        ): Particle {
            return PoweredUpParticle(level, x, y, z, xAux, yAux, zAux, sprites)
        }
    }
}