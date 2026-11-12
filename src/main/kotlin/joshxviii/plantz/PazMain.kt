package joshxviii.plantz

import joshxviii.plantz.command.PazCommands
import joshxviii.plantz.block.entity.MailboxBlockEntity
import joshxviii.plantz.block.entity.MailboxManager
import joshxviii.plantz.block.entity.getMailboxMailQueue
import joshxviii.plantz.block.entity.getTimeMachineManager
import joshxviii.plantz.block.entity.TimeMachineBlockEntity
import joshxviii.plantz.networking.ServerConfigResponsePayload
import joshxviii.plantz.recipe.DoorShieldRecipe
import joshxviii.plantz.raid.ZombieRaids
import joshxviii.plantz.raid.getZombieRaids
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger


object PazMain {
	const val MODID = "sproutsquad"
	@JvmField
	val LOGGER: Logger = LogManager.getLogger("Sprout Squad")

	/** Called once, during the last NeoForge RegisterEvent (all registries are unfrozen then). */
	fun onInitialize() {
		PazRegistries.initialize()
		DoorShieldRecipe.register()
		PazEnchantments.initialize()
		PazConfig.load()
		// clear client raid cache + load config
		NeoForge.EVENT_BUS.addListener(ServerStartingEvent::class.java) {
			ZombieRaids.clearActiveRaidCenters()
			MailboxManager.clearMailboxes() // Sprout Squad: the previous world's mailboxes stayed listed
			PazConfig.load()
		}
		// config sync
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent::class.java) { event ->
			val player = event.entity as? ServerPlayer ?: return@addListener
			val json = PazConfig.let { it.GSON.toJson(it.server) }
			val payload = ServerConfigResponsePayload(json)
			PacketDistributor.sendToPlayer(player, payload)

			LOGGER.info("Sent server config to ${player.name.string}")
		}
		// raid ticking
		NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post::class.java) { event ->
			(event.level as? ServerLevel)?.let { it.getZombieRaids().tick(it) }
		}
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post::class.java) { event ->
			event.server.overworld().getTimeMachineManager().tick(event.server)
		}

		PazCommands.initialize()
		PazServerParticles.initialize()
		PazBlocks.initialize()
		PazItems.initialize()
		PazLootTables.initialize()
		PazEffects.initialize()
		joshxviii.plantz.neoforge.PazSyncedData.initialize()
		PazCreativeTabs.initialize()
		PazEntities.initialize()
		PazDamageTypes.initialize()
		PazCriteria.initialize()
		PazDataSerializers.initialize()
		PazAttributes.initialize()
		PazSounds.initialize()
		PazSpawnPlacements.initialize()
		PazMenus.initialize()
		PazJukeboxSongs.initialize()
		PazPoiTypes.initialize()
		PazWorldGen.initialize()
		PlantHeadAttachment.register()
	}

	/** Mailbox / time machine bookkeeping; called from [BlockEntity.onLoad] on the server. */
	fun onBlockEntityLoad(blockEntity: BlockEntity, level: ServerLevel) {
		(blockEntity as? TimeMachineBlockEntity)?.let { level.getTimeMachineManager().register(it) }
		(blockEntity as? MailboxBlockEntity)?.let {
			MailboxManager.registerMailbox(level, it)
			level.getMailboxMailQueue().deliverTo(it)
		}
	}
}
