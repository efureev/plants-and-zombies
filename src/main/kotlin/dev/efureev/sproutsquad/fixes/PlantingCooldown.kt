package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.PazConfig
import joshxviii.plantz.item.SeedPacketItem
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.component.UseCooldown
import java.util.Optional

/**
 * `plantCooldownEnabled`: after planting, every seed packet of that plant type waits. The original
 * puts the cooldown group (the plant id) only on the stack that was used, so another stack of the
 * same plant could be planted at once; this checks the group itself.
 */
object PlantingCooldown {
    fun isOnCooldown(player: Player, type: EntityType<*>?): Boolean {
        if (!PazConfig.PLANT_COOLDOWN_ENABLED || type == null || player.hasInfiniteMaterials()) return false
        val probe = SeedPacketItem.stackFor(type)
        probe.set(DataComponents.USE_COOLDOWN, UseCooldown(1f, Optional.of(BuiltInRegistries.ENTITY_TYPE.getKey(type))))
        return player.cooldowns.isOnCooldown(probe)
    }
}
