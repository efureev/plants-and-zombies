package dev.efureev.sproutsquad.client

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource

/** A puff of popcorn from a Corn Cannon's cob: pops up, falls, bounces on the ground and is gone in about 1.5 s. */
class PopcornParticle private constructor(
    level: ClientLevel, x: Double, y: Double, z: Double, xa: Double, ya: Double, za: Double, sprite: TextureAtlasSprite,
) : SingleQuadParticle(level, x, y, z, xa, ya, za, sprite) {
    init {
        xd = xa
        yd = ya + 0.15 + random.nextDouble() * 0.15
        zd = za
        gravity = 0.9f
        friction = 0.96f
        hasPhysics = true
        quadSize = 0.08f + random.nextFloat() * 0.04f
        lifetime = 24 + random.nextInt(10)
        roll = random.nextFloat() * Mth.TWO_PI
        oRoll = roll
    }

    override fun tick() {
        super.tick()
        if (onGround) {
            xd *= 0.7
            zd *= 0.7
        }
    }

    public override fun getLayer(): Layer = Layer.OPAQUE

    class Provider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            options: SimpleParticleType, level: ClientLevel, x: Double, y: Double, z: Double,
            xAux: Double, yAux: Double, zAux: Double, random: RandomSource,
        ): Particle = PopcornParticle(level, x, y, z, xAux, yAux, zAux, sprites.get(random))
    }
}
