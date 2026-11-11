package dev.efureev.sproutsquad.fixes

import dev.efureev.sproutsquad.registry.SquadAttachments
import joshxviii.plantz.entity.plant.Marigold
import net.minecraft.core.component.DataComponents
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.DyeColor
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

/**
 * Marigolds can be dyed: right-click one with any dye (owner only, if tamed). The original has a
 * texture for every dye colour (`marigold_<colour>.png`, `marigold_sleep_<colour>.png`) that no
 * code ever picked. A Marigold named "Rainbow" uses the rainbow texture (a magic name, like
 * "Flowey").
 */
object MarigoldColors {
    fun register() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract::class.java, ::onInteract)
    }

    fun color(marigold: Marigold): DyeColor? =
        marigold.getData(SquadAttachments.MARIGOLD_COLOR).takeIf { it >= 0 }?.let { DyeColor.byId(it) }

    private fun onInteract(event: PlayerInteractEvent.EntityInteract) {
        val marigold = event.target as? Marigold ?: return
        val dye = event.itemStack.get(DataComponents.DYE) ?: return
        val player = event.entity
        if (marigold.isTame && !marigold.isOwnedBy(player)) return
        if (color(marigold) == dye) return
        if (!marigold.level().isClientSide) {
            marigold.setData(SquadAttachments.MARIGOLD_COLOR, dye.id)
            event.itemStack.consume(1, player)
            marigold.playSound(SoundEvents.DYE_USE, 1.0f, 1.0f)
        }
        event.isCanceled = true
        event.cancellationResult = InteractionResult.SUCCESS
    }
}
