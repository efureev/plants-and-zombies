package joshxviii.plantz.neoforge

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent

/**
 * Data collected while the original content objects initialize, and handed to
 * NeoForge mod-bus events that fire later (see [PazNeoForge]).
 */
object PazPlatform {
    private val defaultAttributes = LinkedHashMap<EntityType<out LivingEntity>, AttributeSupplier>()

    /** Replacement for Fabric's `FabricDefaultAttributeRegistry.register`. */
    fun registerDefaultAttributes(type: EntityType<out LivingEntity>, attributes: AttributeSupplier.Builder) {
        defaultAttributes[type] = attributes.build()
    }

    internal fun onEntityAttributeCreation(event: EntityAttributeCreationEvent) {
        defaultAttributes.forEach { (type, attributes) -> event.put(type, attributes) }
    }
}
