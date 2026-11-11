package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazConfig
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.UseCooldown
import java.util.Optional

/**
 * `plantCooldownEnabled`: after planting from a seed packet, every packet of that plant type waits
 * (`plantCooldownTime` + `plantCooldownTimePerSun` per sun of its cost). The cooldown group is the
 * plant id. Unlike the original's `SeedPacketItem.applyCooldown`, the stacks themselves are not
 * changed: a component patch on them would stop packets of the same plant from stacking.
 */
object PlantingCooldown {
    private fun active(player: Player, type: EntityType<*>?): Boolean =
        PazConfig.PLANT_COOLDOWN_ENABLED && type != null && !player.hasInfiniteMaterials()

    private fun group(type: EntityType<*>): Identifier = BuiltInRegistries.ENTITY_TYPE.getKey(type)

    fun isOnCooldown(player: Player, type: EntityType<*>?): Boolean {
        if (!active(player, type)) return false
        val probe = SeedPacketItem.stackFor(type!!)
        probe.set(DataComponents.USE_COOLDOWN, UseCooldown(1f, Optional.of(group(type))))
        return player.cooldowns.isOnCooldown(probe)
    }

    /** True (and tells the player) if planting this type must wait. */
    fun refuse(player: Player, type: EntityType<*>?): Boolean {
        if (!isOnCooldown(player, type)) return false
        player.sendOverlayMessage(Component.translatable("message.sproutsquad.plant_recharging", type!!.description.copy().withStyle(ChatFormatting.BOLD)).withStyle(ChatFormatting.RED))
        return true
    }

    fun start(player: Player, type: EntityType<*>?) {
        if (!active(player, type)) return
        val seconds = PazConfig.getCooldownTime(PazConfig.getSunCost(type))
        player.cooldowns.addCooldown(group(type!!), (seconds * 20).toInt())
    }
}
