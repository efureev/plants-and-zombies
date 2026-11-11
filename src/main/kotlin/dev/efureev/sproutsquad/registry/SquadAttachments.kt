package dev.efureev.sproutsquad.registry

import com.mojang.serialization.Codec
import joshxviii.plantz.PazMain
import net.minecraft.network.codec.ByteBufCodecs
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

/** Sprout Squad's own data attachments (saved with the entity). */
object SquadAttachments {
    val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PazMain.MODID)

    /**
     * Set on a machine a player built from a blueprint. Kept separately from the machine's owner:
     * the owner reference only resolves while that player is online and nearby. Synced to clients.
     */
    val PLAYER_MACHINE: DeferredHolder<AttachmentType<*>, AttachmentType<Boolean>> = ATTACHMENT_TYPES.register("player_machine") { ->
        AttachmentType.builder { -> false }.serialize(Codec.BOOL.fieldOf("value")) { it }.sync(ByteBufCodecs.BOOL).build()
    }

    fun register(modBus: IEventBus) = ATTACHMENT_TYPES.register(modBus)
}
