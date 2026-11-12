package dev.efureev.sproutsquad.fixes

import joshxviii.plantz.item.FootballHelmetItem
import joshxviii.plantz.item.RollerSkatesItem
import net.minecraft.world.entity.EquipmentSlot
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent

/**
 * Roller Skates and the Football Helmet add temporary attribute modifiers (frictionless skating, step
 * height, the charge's speed) and take them off only from their own `inventoryTick`. An item that
 * leaves its slot without passing through that tick (broken by wear, thrown out of the inventory
 * screen) left them on the wearer: a player whose skates broke slid on every block as on ice until
 * they rejoined. The modifiers now go with the item.
 */
object WornItemModifiers {
    fun register() {
        NeoForge.EVENT_BUS.addListener(LivingEquipmentChangeEvent::class.java, ::onEquipmentChange)
    }

    private fun onEquipmentChange(event: LivingEquipmentChangeEvent) {
        val entity = event.entity
        if (entity.level().isClientSide) return
        val old = event.from.item
        val new = event.to.item
        when (event.slot) {
            EquipmentSlot.FEET -> if (old is RollerSkatesItem && new !is RollerSkatesItem) old.removeModifiers(entity)
            EquipmentSlot.HEAD -> if (old is FootballHelmetItem && new !is FootballHelmetItem) old.removeModifiers(entity)
            else -> {}
        }
    }
}
