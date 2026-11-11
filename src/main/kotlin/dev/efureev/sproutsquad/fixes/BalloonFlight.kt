package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.getHeldBalloons
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.tick.PlayerTickEvent

/**
 * Balloons lift a player, but the server only knows that the player hangs in the air without
 * support: after 80 ticks (`ServerGamePacketListenerImpl.aboveGroundTickCount`, or
 * `aboveGroundVehicleTickCount` for a ridden mount) it kicks them for flying. Original TODO in
 * `entity/Balloon.kt`. The counters are reset only while a balloon actually pulls the player (or the
 * mount) up, so the vanilla anti-fly check still works for everyone else. The player tick runs right
 * before the server checks the counters.
 */
object BalloonFlight {
    /** Well below the 80-tick limit. */
    const val CHECK_AFTER_TICKS = 40

    fun register() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post::class.java) { event ->
            val player = event.entity as? ServerPlayer ?: return@addListener
            val connection = player.connection
            // look for balloons only once the player has been floating for a while (it scans entities)
            if (connection.aboveGroundTickCount >= CHECK_AFTER_TICKS && liftedByBalloon(player)) connection.aboveGroundTickCount = 0
            val vehicle = player.rootVehicle
            if (vehicle !== player && connection.aboveGroundVehicleTickCount >= CHECK_AFTER_TICKS && liftedByBalloon(vehicle)) {
                connection.aboveGroundVehicleTickCount = 0
            }
        }
    }

    /** Same condition as the lift in `Balloon.kt`: the balloon is more than 2 blocks above and pulls. */
    fun liftedByBalloon(entity: Entity): Boolean = entity.getHeldBalloons().any { it.y - entity.y > 2.0 }
}
