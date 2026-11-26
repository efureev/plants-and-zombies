package dev.efureev.sproutsquad.content.projectile

import dev.efureev.sproutsquad.content.plant.CornCannon
import dev.efureev.sproutsquad.registry.SquadEntities
import dev.efureev.sproutsquad.registry.SquadParticles
import joshxviii.plantz.PazDamageTypes
import joshxviii.plantz.PazSounds
import joshxviii.plantz.entity.projectile.PazProjectile
import joshxviii.plantz.knockback
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

/**
 * The Corn Cannon's rocket: a corn cob with the same cone at both ends, no flame or smoke. On hitting an
 * entity or a block it blows up: [DAMAGE_CENTER] at the centre down to [DAMAGE_EDGE] at [RADIUS] blocks,
 * only to what the cannon would shoot at ([CornCannon.isValidTarget]); it flies through everything else.
 * Breaks no blocks and sets no fire. See docs/en/characters/corn_cannon.md.
 */
class CornCob(type: EntityType<out CornCob>, level: Level, owner: CornCannon? = null) : PazProjectile(
    type, level, owner, CornCannon.COB_SPAWN,
    PazDamageTypes.PLANT,
    damage = DAMAGE_CENTER,
    knockback = 0.4,
) {
    constructor(level: Level, owner: CornCannon? = null) : this(SquadEntities.CORN_COB.get(), level, owner)

    companion object {
        const val DAMAGE_CENTER = 8f
        const val DAMAGE_EDGE = 4f
        const val RADIUS = 2.5

        /** Popcorn puffs scattered by the blast. */
        private const val POPCORN = 24

        /** Splash damage at [distance] blocks from the centre: linear from 8 to 4, nothing past the edge. */
        fun splashDamage(distance: Double): Float =
            if (distance > RADIUS) 0f
            else (DAMAGE_CENTER - (DAMAGE_CENTER - DAMAGE_EDGE) * (distance / RADIUS)).toFloat()
    }

    override fun getDefaultGravity(): Double = 0.03

    /**
     * The cannon that fired it. [entityOwner] exists only on the server and only for a cob it has just made; a
     * cob on the client or loaded from a save knows its cannon from the projectile's owner reference (synced
     * in the spawn packet, kept in the save). Not `getOwner()`: that gives the planting player for player credit.
     */
    private fun cannon(): CornCannon? =
        entityOwner as? CornCannon ?: owner?.getEntity(level(), Entity::class.java) as? CornCannon

    /**
     * Only what its cannon would shoot at, never the cannon itself. A cob whose cannon is gone (unloaded,
     * dead, or a summoned cob) hits only zombies, never a player, a pet or a plant.
     */
    override fun canHitEntity(entity: Entity): Boolean {
        if (!super.canHitEntity(entity) || entity !is LivingEntity) return false
        val cannon = cannon() ?: return entity is Zombie
        return entity !== cannon && cannon.isValidTarget(entity)
    }

    /** [canHitEntity] for callers outside the class (tests). */
    fun hits(entity: Entity): Boolean = canHitEntity(entity)

    /** The server decides where the cob blows up; the client only draws it and waits for the removal. */
    override fun onHitEntity(hitResult: EntityHitResult) {
        if (level() !is ServerLevel) return
        explodeAt(position())
        discard()
    }

    override fun onHitBlock(hitResult: BlockHitResult) {
        if (level() !is ServerLevel) return
        explodeAt(hitResult.location)
        discard()
    }

    /** The blast at [centre]: splash damage and a push to every valid target within [RADIUS], a flash, popcorn and a bang. */
    fun explodeAt(centre: Vec3) {
        val level = level() as? ServerLevel ?: return
        val source = damageSources().source(damageType, this, getOwner())
        level.getEntitiesOfClass(LivingEntity::class.java, AABB(centre, centre).inflate(RADIUS)) { hits(it) }.forEach { target ->
            val distance = sqrt(target.boundingBox.distanceToSqr(centre))
            val amount = splashDamage(distance)
            if (amount <= 0f) return@forEach
            if (target.hurtServer(level, source, amount)) {
                val direction = calculateHorizontalHurtKnockbackDirection(target, source)
                target.knockback(this.knockback, -direction.leftDouble(), -direction.rightDouble())
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0.0, 0.0, 0.0, 0.0)
        level.sendParticles(SquadParticles.POPCORN.get(), centre.x, centre.y + 0.3, centre.z, POPCORN, 0.6, 0.3, 0.6, 0.12)
        level.playSound(null, centre.x, centre.y, centre.z, PazSounds.MISSILE_EXPLODE.value(), SoundSource.NEUTRAL, 1.0f, 1.0f)
    }
}
