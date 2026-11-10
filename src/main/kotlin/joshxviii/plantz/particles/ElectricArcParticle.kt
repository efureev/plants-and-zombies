package joshxviii.plantz.particles
import joshxviii.plantz.ElectricArcParticleOptions
import joshxviii.plantz.PazParticles
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3

class ElectricArcParticle private constructor(
    world: ClientLevel,
    x: Double, y: Double, z: Double,
    val targetPos: Vec3,
    var thickness: Float,
    val color: Int,
) : Particle(world, x, y, z) {
    var startPos: Vec3 = Vec3(x, y, z)
    val particleAge: Int
        get() = (age)
    var alpha: Float = 1.0f

    init {
        lifetime = 6 + world.random.nextInt(4)
        hasPhysics = false
    }

    override fun tick() {
        super.tick()
    }

    override fun getGroup(): ParticleRenderType = PazParticles.ELECTRIC_ARC

    override fun getLightCoords(a: Float): Int = LightCoordsUtil.addSmoothBlockEmission(super.getLightCoords(a), 1.0f)

    class Provider : ParticleProvider<ElectricArcParticleOptions> {
        override fun createParticle(
            options: ElectricArcParticleOptions,
            level: ClientLevel,
            x: Double, y: Double, z: Double,
            vx: Double, vy: Double, vz: Double,
            random: RandomSource
        ): Particle {
            return ElectricArcParticle(
                level, x, y, z, options.targetPos, options.width, options.color
            )
        }
    }
}