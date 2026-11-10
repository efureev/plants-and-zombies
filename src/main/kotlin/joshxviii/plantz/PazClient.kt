package joshxviii.plantz

import joshxviii.plantz.PazItems.DUCKY_TUBE
import joshxviii.plantz.PazItems.DYE_BLASTER
import joshxviii.plantz.PazItems.FOOTBALL_HELMET
import joshxviii.plantz.PazItems.GARDENING_GLOVE
import joshxviii.plantz.PazItems.PLANT_POT_HELMET
import joshxviii.plantz.item.DuckTubeItem
import joshxviii.plantz.item.DyeBlasterItem
import joshxviii.plantz.item.FootballHelmetItem
import joshxviii.plantz.item.GardeningGloveItem
import joshxviii.plantz.item.PlantPotHelmetItem
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.AddServerReloadListenersEvent
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.server.packs.resources.ResourceManagerReloadListener

object PazClient {
	fun onInitializeClient() {
		PazConfig.load()
		PazClientNetwork.initialize()
		PazRenderPipelines.initialize()

		NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent::class.java) { event ->
			event.addListener(
				pazResource("server_data_reload"),
				ResourceManagerReloadListener({ PazNetwork.ZombieRaidClientCache.clear() })
			)
		}

		// custom tooltips
		NeoForge.EVENT_BUS.addListener(ItemTooltipEvent::class.java) { event ->
			val stack = event.itemStack
			val lines = event.toolTip
			val minecraft = Minecraft.getInstance()
			val sneakKey = keybindFormat(minecraft.options.keyShift)
			val sprintKey = keybindFormat(minecraft.options.keySprint)
			val rightClick = keybindFormat(minecraft.options.keyAttack)
			val leftClick = keybindFormat(minecraft.options.keyUse)

			if (stack.`is`(GARDENING_GLOVE)) GardeningGloveItem.addToTooltip(lines, stack)
			if (stack.`is`(PLANT_POT_HELMET)) PlantPotHelmetItem.addToTooltip(lines)
			if (stack.`is`(DYE_BLASTER)) DyeBlasterItem.addToTooltip(lines)
			if (stack.`is`(DUCKY_TUBE)) DuckTubeItem.addToTooltip(lines)
		}
	}
	fun registerClientReloadListeners(event: AddClientReloadListenersEvent) {
		event.addListener(
			pazResource("client_reload"),
			ResourceManagerReloadListener({ if (PazNetwork.ZombieRaidClientCache.isActive()) { RaidMusicManager.restart() } })
		)
	}

	fun keybindFormat(keybind: KeyMapping): Component {
		return Component.translatable("chat.square_brackets", keybind.translatedKeyMessage).withColor(0x51ff53)
	}
}