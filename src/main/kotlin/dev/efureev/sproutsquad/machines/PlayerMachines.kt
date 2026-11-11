package dev.efureev.sproutsquad.machines

import dev.efureev.sproutsquad.registry.SquadAttachments
import joshxviii.plantz.entity.blueprint_machines.LawnMower
import joshxviii.plantz.entity.plant.Plant
import joshxviii.plantz.entity.zombie.ZombieRobot
import joshxviii.plantz.isHypnotized
import joshxviii.plantz.raid.isBoss
import joshxviii.plantz.raid.setIgnoreFlag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.Tags
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent

/**
 * Battle machines built by players from blueprints (turrets, the drone, the lawn mower).
 *
 * In the original mod every machine is a hostile "zombie": built from a blueprint it had no owner,
 * attacked the player and plants, walked to the flag to break it and despawned like a monster.
 * A player-built machine is marked with [SquadAttachments.PLAYER_MACHINE] and then:
 * - targets only monsters hostile to players ([isEnemyOfPlayers]);
 * - is never targeted or hit by plants, does not hit players, plants or other players' machines;
 * - never despawns (also on Peaceful: the despawn event is checked first);
 * - ignores the raid flag.
 * Machines built by Engineer zombies stay hostile.
 */
object PlayerMachines {
    /** Damage a one-shot machine attack deals to bosses instead of killing them. */
    const val BOSS_DAMAGE = 40f

    fun register() {
        NeoForge.EVENT_BUS.addListener(LivingChangeTargetEvent::class.java, ::onChangeTarget)
        NeoForge.EVENT_BUS.addListener(MobDespawnEvent::class.java) { event ->
            if (isPlayerMachine(event.entity)) event.result = MobDespawnEvent.Result.DENY
        }
    }

    fun isPlayerMachine(entity: Entity?): Boolean =
        entity is ZombieRobot && entity.getData(SquadAttachments.PLAYER_MACHINE)

    /** Monsters a player's machine fights: hostile mobs that are not allies of the players. */
    fun isEnemyOfPlayers(entity: LivingEntity): Boolean =
        entity.isAlive
            && entity is Enemy
            && entity !is Creeper       // blowing up next to the machine or the player
            && entity !is NeutralMob    // zombified piglins, endermen: leave them alone
            && entity !is Plant
            && !isPlayerMachine(entity)
            && !entity.isHypnotized()

    /**
     * Projectile hits that never happen: a player's machine does not shoot players, plants or other
     * players' machines, and plants do not shoot players' machines (stray shots in a line of fire).
     */
    fun blocksHit(owner: Entity, target: Entity): Boolean =
        (isPlayerMachine(owner) && (isPlayerMachine(target) || target is Plant || target is Player))
            || (owner is Plant && isPlayerMachine(target))

    /** `canAttack` of a player's machine: replaces PazZombie's "zombies never attack zombies". */
    fun canBeAttackedByMachine(target: LivingEntity): Boolean = isEnemyOfPlayers(target) && target.canBeSeenAsEnemy()

    fun isBoss(entity: LivingEntity): Boolean =
        entity.type.builtInRegistryHolder().`is`(Tags.EntityTypes.BOSSES) || (entity is Zombie && entity.isBoss())

    /** Called by `BlueprintItem` right before the built machine is added to the level. */
    fun onBuilt(entity: Entity, player: Player) {
        when (entity) {
            is ZombieRobot -> {
                entity.setData(SquadAttachments.PLAYER_MACHINE, true)
                entity.owner = player
                entity.setIgnoreFlag(true)
            }
            // The blueprint turns every machine to face the player; the mower must face away
            // from the player (handle towards them), the way it will drive.
            is LawnMower -> {
                val yaw = entity.yRot + 180f
                entity.yRot = yaw
                entity.yBodyRot = yaw
                entity.yHeadRot = yaw
            }
        }
    }

    private fun onChangeTarget(event: LivingChangeTargetEvent) {
        val attacker = event.entity
        val target = event.newAboutToBeSetTarget ?: return
        val blocked = when {
            isPlayerMachine(attacker) -> !isEnemyOfPlayers(target)  // also stops the original "attack plants" goals
            attacker is Plant -> isPlayerMachine(target)
            else -> false
        }
        if (blocked) event.isCanceled = true
    }
}
