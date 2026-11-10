package joshxviii.plantz.particles

import joshxviii.plantz.NukeSmokeParticleOptions
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.util.RandomSource
import kotlin.math.floor

class NukeSmokeParticle(
    level: ClientLevel, x: Double, y: Double, z: Double, xa: Double, ya: Double, za: Double,
    private val scale: Float,
    private val sprites: SpriteSet
) : SingleQuadParticle(level, x, y, z, 0.0, 0.0, 0.0, sprites.first()) {
    var rotSpeed = Math.toRadians(if (this.random.nextBoolean()) -30.0 else 30.0)
    init {
        this.quadSize = (random.nextFloat() * scale) + scale / 2.0f
        this.lifetime = (25 + (floor(this.quadSize / 5))).toInt()
    }

    override fun tick() {
        super.tick()
        this.setSpriteFromAge(sprites);
        this.alpha = 1.0f - (this.age) / this.lifetime.toFloat()
        this.oRoll = this.roll
        this.roll += this.rotSpeed.toFloat() / 10.0f
    }

    public override fun getLayer(): Layer = Layer.TRANSLUCENT

    class Provider(private val sprite: SpriteSet) : ParticleProvider<NukeSmokeParticleOptions> {
        override fun createParticle(
            options: NukeSmokeParticleOptions,
            level: ClientLevel,
            x: Double,
            y: Double,
            z: Double,
            xAux: Double,
            yAux: Double,
            zAux: Double,
            random: RandomSource
        ): Particle {
            val particle = NukeSmokeParticle(level, x, y, z, xAux, yAux, zAux, options.scale, sprite)
            val color = options.getColor()
            particle.setColor(color.x, color.y, color.z)
            return particle
        }
    }
}