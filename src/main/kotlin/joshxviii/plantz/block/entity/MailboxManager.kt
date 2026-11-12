package joshxviii.plantz.block.entity

import joshxviii.plantz.MailboxData
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import java.util.Locale

object MailboxManager {

    // Sprout Squad: each mailbox's data is kept with its position. The list was built with getBlockEntity, which
    // loads the chunk on the spot: opening any mailbox loaded the chunks of all the known mailboxes on the server
    // thread. Mailboxes whose chunk is not loaded are listed with the data they had when last seen.
    private val mailboxes: MutableMap<ResourceKey<Level>, MutableMap<BlockPos, MailboxData>> = mutableMapOf()

    /** Called when a server starts: the registry is in memory and used to keep the mailboxes of the last world. */
    fun clearMailboxes() {
        mailboxes.clear()
    }

    fun registerMailbox(level: Level, blockEntity: MailboxBlockEntity) {
        val levelKey = level.dimension()
        mailboxes.getOrPut(levelKey) { mutableMapOf() }[blockEntity.blockPos.immutable()] = blockEntity.asMailBoxData()
    }

    /** Updates the stored data of a registered mailbox (name, colour). */
    fun refreshMailbox(level: Level, blockEntity: MailboxBlockEntity) {
        mailboxes[level.dimension()]?.computeIfPresent(blockEntity.blockPos) { _, _ -> blockEntity.asMailBoxData() }
    }

    fun isRegistered(level: Level, pos: BlockPos): Boolean = mailboxes[level.dimension()]?.containsKey(pos) == true

    fun unregisterMailbox(level: Level, pos: BlockPos) {
        val levelKey = level.dimension()
        mailboxes[levelKey]?.remove(pos)
    }

    fun getMailboxesInLevel(level: Level): List<MailboxData> {
        val known = mailboxes[level.dimension()] ?: return emptyList()
        val result = mutableListOf<MailboxData>()
        val iterator = known.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (level.isLoaded(entry.key)) {
                val mailbox = level.getBlockEntity(entry.key) as? MailboxBlockEntity
                if (mailbox == null) {// loaded, but the mailbox is gone
                    iterator.remove()
                    continue
                }
                entry.setValue(mailbox.asMailBoxData())
            }
            result += entry.value
        }
        return result
    }

    fun searchMailboxes(mailboxes: List<MailboxData>, search: String): List<MailboxData> {
        if (search.isEmpty()) return mailboxes

        return mailboxes.filter { mailbox ->
            mailbox.name.string.lowercase(Locale.getDefault())
                .contains(search.lowercase(Locale.getDefault()))
        }
    }
}