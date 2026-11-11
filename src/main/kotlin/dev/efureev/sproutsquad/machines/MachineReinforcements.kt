package dev.efureev.sproutsquad.machines

import joshxviii.plantz.entity.zombie.ZombieRobot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent

/**
 * Battle machines never call reinforcements.
 *
 * Machines are zombies to the game, and the vanilla zombie setup gives every zombie a 0–10% chance to
 * call a reinforcement of its own type when it is hit on Hard, at a random spot 7–40 blocks away. Machines
 * have no spawn rules, so that turret or drone could appear in the air or underground, and a player's
 * machine called a hostile one (the reinforcement is not the player's). The base chance is set to 0 when a
 * machine joins a level: after its setup, and also for machines saved in existing worlds.
 */
object MachineReinforcements {
    fun register() {
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent::class.java) { event ->
            if (event.level.isClientSide) return@addListener
            val machine = event.entity as? ZombieRobot ?: return@addListener
            machine.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE)?.baseValue = 0.0
        }
    }
}
