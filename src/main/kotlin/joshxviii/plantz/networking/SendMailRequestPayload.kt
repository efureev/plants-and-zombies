package joshxviii.plantz.networking

import joshxviii.plantz.PazCriteria
import joshxviii.plantz.block.MailboxState
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.block.entity.MailboxMailQueue
import joshxviii.plantz.block.entity.MailboxManager
import joshxviii.plantz.block.entity.getMailboxMailQueue
import joshxviii.plantz.inventory.MailboxMenu
import joshxviii.plantz.pazResource
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity

data class SendMailRequestPayload(val targetPos: BlockPos) : CustomPacketPayload {

    companion object {
        val ID: CustomPacketPayload.Type<SendMailRequestPayload> = CustomPacketPayload.Type(pazResource("send_mail_request"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SendMailRequestPayload> =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                SendMailRequestPayload::targetPos,
                ::SendMailRequestPayload
            )

        fun handleSendMailPacket(payload: SendMailRequestPayload, context: IPayloadContext) {
            val player = context.player() as ServerPlayer
            val level = player.level()
            val targetPos = payload.targetPos

            val menu = player.containerMenu as? MailboxMenu ?: return

            val senderBE = level.getBlockEntity(menu.data.blockPos) as? MailboxBlockEntity ?: return
            if (menu.availableMailboxes.none { it.blockPos == targetPos }) return

            val targetBE = loadedMailbox(level, targetPos)
            val result = trySendStack(level, menu.mailSlot.item.copy(), targetPos)

            // Sprout Squad: queued mail has left too; left in the slot it would be sent again and again
            if (result.isSent()) {
                menu.mailSlot.set(ItemStack.EMPTY)
                menu.broadcastChanges()
                senderBE.setChanged()
                targetBE?.setChanged()
                targetBE?.updateMailboxState(MailboxState.HAS_MAIL)
                level.playSound(null, menu.data.blockPos, SoundEvents.UI_LOOM_SELECT_PATTERN, SoundSource.BLOCKS, 0.3f, 1.2f)
                PacketDistributor.sendToPlayer(player, SendMailResponsePayload(
                    Component.translatable("container.sproutsquad.mailbox_success").withColor(0x00FF00)
                ))
            }
            else {
                level.playSound(null, menu.data.blockPos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.3f, 1.2f)
                PacketDistributor.sendToPlayer(player, SendMailResponsePayload(
                    Component.translatable("container.sproutsquad.mailbox_full", targetBE?.name ?: Component.translatable("item.sproutsquad.mailbox")).withColor(0xFF0000)
                ))
            }
            PazCriteria.SEND_MAIL.trigger(player, result.isSent())
        }

        fun trySendStack(level: ServerLevel, stack: ItemStack, targetPos: BlockPos): MailboxSendResult {
            if (stack.isEmpty) return MailboxSendResult.FAIL
            // Sprout Squad: checked first. getBlockEntity loads the chunk on the spot, so every letter loaded the
            // target's chunk on the server thread and the queue below was never used.
            if (!level.isLoaded(targetPos)) {// the target's chunk is not loaded: queue the mail
                level.getMailboxMailQueue().queue(targetPos, stack)
                return MailboxSendResult.QUEUED
            }
            val targetBE = level.getBlockEntity(targetPos) as? MailboxBlockEntity

            return when {
                targetBE != null -> {// the mailbox is present, try to insert mail
                    if (MailboxMailQueue.tryInsertIntoMailbox(targetBE, stack)) MailboxSendResult.SUCCESS else MailboxSendResult.FULL
                }
                else -> {// the chunk is loaded, but the mailbox is missing. discard
                    level.getMailboxMailQueue().discardFor(targetPos)
                    MailboxManager.unregisterMailbox(level, targetPos)
                    MailboxSendResult.DISCARDED
                }
            }
        }

        /** The target mailbox, without loading its chunk. */
        fun loadedMailbox(level: ServerLevel, pos: BlockPos): MailboxBlockEntity? =
            if (level.isLoaded(pos)) level.getBlockEntity(pos) as? MailboxBlockEntity else null
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = ID
}

enum class MailboxSendResult {
    SUCCESS,
    FAIL,
    FULL,
    QUEUED,
    DISCARDED;

    /** The mail left the sender: delivered now or queued until the target mailbox loads. */
    fun isSent(): Boolean = this == SUCCESS || this == QUEUED
}