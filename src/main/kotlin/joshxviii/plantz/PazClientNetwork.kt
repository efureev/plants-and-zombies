package joshxviii.plantz

import joshxviii.plantz.PazNetwork.ZombieRaidClientCache
import joshxviii.plantz.inventory.AbstractMailboxMenu
import joshxviii.plantz.inventory.MailCollectionBoxMenu
import joshxviii.plantz.gui.PazOptionsScreen
import joshxviii.plantz.networking.MailboxListResponsePayload
import joshxviii.plantz.networking.OpenOptionsScreenPayload
import joshxviii.plantz.networking.SendMailResponsePayload
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.networking.ZombieRaidResponsePayload
import joshxviii.plantz.raid.ZombieRaid
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent
import net.neoforged.neoforge.common.NeoForge

object PazClientNetwork {
    fun initialize() {
        ZombieRaidClientCache.clear()

        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre::class.java) {
            RaidMusicManager.tick()
        }
        // Sprout Squad: a raid left behind kept muting the menu music and showed up on the next server
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut::class.java) {
            ZombieRaidClientCache.clear()
        }
    }

    fun registerHandlers(event: RegisterClientPayloadHandlersEvent) {
        event.register(ZombieRaidResponsePayload.ID) { payload, context ->
            if (!ZombieRaidClientCache.isActive()) RaidMusicManager.restart()
            ZombieRaidClientCache.put(payload.data)
            if (payload.data.status == ZombieRaid.ZombieRaidStatus.TERMINATE) ZombieRaidClientCache.remove(payload.data.id)
        }

        event.register(OpenOptionsScreenPayload.ID) { _, context ->
            context.enqueueWork { Minecraft.getInstance().gui.setScreen(PazOptionsScreen()) }
        }

        event.register(SendMailResponsePayload.ID) { payload, context ->
            context.enqueueWork {
                val mc = Minecraft.getInstance()
                val player = mc.player ?: return@enqueueWork
                val menu = player.containerMenu as? AbstractMailboxMenu ?: return@enqueueWork
                menu.responseMessage = payload.message
                menu.responseTimeout = 30
            }
        }

        event.register(MailboxListResponsePayload.ID) { payload, context ->
            context.enqueueWork {
                val player = context.player()
                val menu = player.containerMenu as? AbstractMailboxMenu ?: return@enqueueWork

                if (menu is MailCollectionBoxMenu) {
                    menu.selectedMailboxPos = menu.data.selectedMailbox
                }
                // Rebuild list from positions
                menu.availableMailboxes = payload.mailboxes
                menu.updateFilteredMailboxes()
            }

        }

        event.register(ServerConfigResponsePayload.ID) { payload, context ->
            context.enqueueWork {
                // Sprout Squad: singleplayer and LAN host share PazConfig with their own server; keep its object
                if (Minecraft.getInstance().isLocalServer) return@enqueueWork
                try {
                    PazConfig.server = PazConfig.GSON.fromJson(payload.json, ServerConfig::class.java)
                    PazMain.LOGGER.info("Server config synced!")
                } catch (e: Exception) {
                    PazMain.LOGGER.error("Failed to parse server config", e)
                }
            }
        }

    }
}