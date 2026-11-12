package joshxviii.plantz.networking

import joshxviii.plantz.MailboxData
import joshxviii.plantz.block.entity.MailCollectionBoxEntity
import joshxviii.plantz.inventory.MailCollectionBoxMenu
import joshxviii.plantz.pazResource
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

data class UpdateCollectionBoxPayload(
    val collectionBoxPos: BlockPos,
    val selectedMailbox: BlockPos?
) : CustomPacketPayload {
    companion object {
        val ID: CustomPacketPayload.Type<UpdateCollectionBoxPayload> = CustomPacketPayload.Type(pazResource("update_collection_box"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, UpdateCollectionBoxPayload> =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, UpdateCollectionBoxPayload::collectionBoxPos,
                ByteBufCodecs.optional(BlockPos.STREAM_CODEC), { Optional.ofNullable(it.selectedMailbox) },
                { pos, targetPos -> UpdateCollectionBoxPayload(pos, targetPos.getOrNull()) }
            )

        fun handleUpdateCollectionBoxPacket(payload: UpdateCollectionBoxPayload, context: IPayloadContext) {
            val player = context.player() as ServerPlayer
            val level = player.level()

            val be = level.getBlockEntity(payload.collectionBoxPos) as? MailCollectionBoxEntity ?: return
            val menu = player.containerMenu as? MailCollectionBoxMenu ?: return
            // Sprout Squad: only the box this player has open, and only a mailbox the server listed to them
            // (any collection box in the level could be pointed anywhere, losing its mail into the queue)
            if (menu.data.blockPos != payload.collectionBoxPos || !menu.stillValid(player)) return
            val target = payload.selectedMailbox
            if (target != null && menu.availableMailboxes.none { it.blockPos == target }) return

            be.selectedMailbox = target
            be.setChanged()
        }
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}