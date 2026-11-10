package joshxviii.plantz

import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.SendMailRequestPayload
import joshxviii.plantz.networking.SendMailRequestPayload.Companion.handleSendMailPacket
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.OpenOptionsScreenPayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.UpdateCollectionBoxPayload
import joshxviii.plantz.networking.UpdateCollectionBoxPayload.Companion.handleUpdateCollectionBoxPacket
import joshxviii.plantz.networking.UpdateLetterPayload
import joshxviii.plantz.networking.UpdateLetterPayload.Companion.handleUpdateLetterPacket
import joshxviii.plantz.networking.ZombieRaidClientData
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import net.neoforged.neoforge.network.registration.PayloadRegistrar
import java.util.UUID

object PazNetwork {

    object ZombieRaidClientCache {
        private val active = mutableMapOf<UUID, ZombieRaidClientData>()

        fun get() = active.values.firstOrNull()
        fun get(id: UUID) = active[id]
        fun put(data: ZombieRaidClientData) { active[data.id] = data }
        fun remove(id: UUID) { active.remove(id) }
        fun clear() { active.clear() }

        fun isActive() = active.isNotEmpty()
    }

    fun registerPayloads(registrar: PayloadRegistrar) {
        // Serverbound payloads
        registrar.playToServer(SendMailRequestPayload.ID, SendMailRequestPayload.STREAM_CODEC, ::handleSendMailPacket)
        registrar.playToServer(UpdateCollectionBoxPayload.ID, UpdateCollectionBoxPayload.STREAM_CODEC, ::handleUpdateCollectionBoxPacket)
        registrar.playToServer(UpdateLetterPayload.ID, UpdateLetterPayload.STREAM_CODEC, ::handleUpdateLetterPacket)

        // Clientbound payloads (handlers are registered on the client, see PazClientNetwork)
        registrar.playToClient(SendMailResponsePayload.ID, SendMailResponsePayload.STREAM_CODEC)
        registrar.playToClient(MailboxListResponsePayload.ID, MailboxListResponsePayload.STREAM_CODEC)
        registrar.playToClient(ServerConfigResponsePayload.ID, ServerConfigResponsePayload.STREAM_CODEC)
        registrar.playToClient(ZombieRaidResponsePayload.ID, ZombieRaidResponsePayload.STREAM_CODEC)
        registrar.playToClient(OpenOptionsScreenPayload.ID, OpenOptionsScreenPayload.STREAM_CODEC)
    }
}