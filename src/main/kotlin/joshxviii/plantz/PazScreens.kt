package joshxviii.plantz

import joshxviii.plantz.gui.LetterScreen
import joshxviii.plantz.gui.MailCollectionBoxScreen
import joshxviii.plantz.gui.MailboxScreen
import joshxviii.plantz.gui.TimeMachineScreen
import joshxviii.plantz.gui.ZombieRaidOverlay
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.gui.VanillaGuiLayers

object PazScreens {

    fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(PazMenus.LETTER_MENU, ::LetterScreen)
        event.register(PazMenus.MAILBOX_MENU, ::MailboxScreen)
        event.register(PazMenus.MAIL_COLLECTION_BOX_MENU, ::MailCollectionBoxScreen)
        event.register(PazMenus.TIME_MACHINE_MENU, ::TimeMachineScreen)
    }

    fun registerGuiLayers(event: RegisterGuiLayersEvent) {
        event.registerAbove(
            VanillaGuiLayers.BOSS_OVERLAY,
            pazResource("zombie_raid"),
            ZombieRaidOverlay::extractRenderState
        )
    }

}