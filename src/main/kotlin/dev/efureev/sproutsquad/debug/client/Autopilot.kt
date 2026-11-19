package dev.efureev.sproutsquad.debug.client

import joshxviii.plantz.PazMain
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.PauseScreen
import net.neoforged.neoforge.client.gui.LoadingErrorScreen
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import java.io.File

/**
 * Development aid for checking the game by eye without anyone at the keyboard: plays a script of
 * commands, waits and screenshots in a singleplayer world, then closes the game.
 *
 * Off unless the JVM is started with `-Dsproutsquad.autopilot=<script file>` (`./gradlew runClient -Pautopilot=<file>`,
 * see build.gradle). One step per line, `#` starts a comment:
 * - `wait <ticks>` — do nothing for that many client ticks (20 a second);
 * - `cmd <command>` — run a command as the player, with full permissions, without the leading slash;
 * - `hud on|off` — show or hide the HUD (F1);
 * - `shot <name>` — save `screenshots/<name>.png` in the game directory;
 * - `quit` — close the game.
 * The script starts once the player is in the world.
 */
object Autopilot {
    private const val PROPERTY = "sproutsquad.autopilot"

    private var steps: ArrayDeque<String> = ArrayDeque()
    private var waitTicks = 0
    private var lastScreen: String? = null

    fun init() {
        val path = System.getProperty(PROPERTY) ?: return
        val file = File(path)
        if (!file.isFile) {
            PazMain.LOGGER.error("Autopilot: no script at {}", file.absolutePath)
            return
        }
        steps = ArrayDeque(file.readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() })
        PazMain.LOGGER.info("Autopilot: {} steps from {}", steps.size, file.absolutePath)
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post::class.java) { tick(Minecraft.getInstance()) }
    }

    /**
     * NeoForge stops on a screen when a mod (Kotlin for Forge's deprecated `logoFile`) only warns; press its
     * "continue" for the script. With real errors close the game instead: the log has them.
     */
    private fun continuePastWarnings(minecraft: Minecraft, screen: LoadingErrorScreen) {
        fun field(name: String) = LoadingErrorScreen::class.java.getDeclaredField(name).apply { isAccessible = true }.get(screen)
        if ((field("modLoadErrors") as List<*>).isNotEmpty()) {
            PazMain.LOGGER.error("Autopilot: the mods failed to load, quitting")
            minecraft.stop()
            return
        }
        PazMain.LOGGER.info("Autopilot: continuing past mod loading warnings")
        (field("nextScreenTask") as Runnable).run()
    }

    private fun tick(minecraft: Minecraft) {
        // a screen in the way (a warning, an error) stops the script: say which, so the log shows it
        val screen = minecraft.gui.screen()?.javaClass?.simpleName
        if (screen != lastScreen) {
            lastScreen = screen
            PazMain.LOGGER.info("Autopilot: screen {}", screen ?: "none")
            (minecraft.gui.screen() as? LoadingErrorScreen)?.let { continuePastWarnings(minecraft, it) }
        }
        val player = minecraft.player ?: return
        val server = minecraft.singleplayerServer ?: return
        // nobody is at the keyboard: losing focus (another window, another desktop) must not pause the script
        minecraft.options.pauseOnLostFocus = false
        if (minecraft.gui.screen() is PauseScreen) minecraft.gui.setScreen(null)
        if (minecraft.level == null || minecraft.gui.screen() != null) return
        if (waitTicks > 0) {
            waitTicks--
            return
        }
        val step = steps.removeFirstOrNull() ?: return
        val (verb, arg) = step.split(' ', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        PazMain.LOGGER.info("Autopilot: {}", step)
        when (verb) {
            "wait" -> waitTicks = arg.toIntOrNull() ?: 0.also { PazMain.LOGGER.error("Autopilot: '{}' needs a number of ticks", step) }
            "cmd" -> server.execute {
                val serverPlayer = server.playerList.getPlayer(player.uuid) ?: return@execute
                val source = server.createCommandSourceStack().withEntity(serverPlayer)
                    .withPosition(serverPlayer.position()).withRotation(serverPlayer.rotationVector).withLevel(serverPlayer.level())
                server.commands.performPrefixedCommand(source, arg)
            }
            "hud" -> if (minecraft.gui.hud.isHidden != (arg == "off")) minecraft.gui.hud.toggle()
            "shot" -> Screenshot.grab(minecraft.gameDirectory, "$arg.png", minecraft.gameRenderer.mainRenderTarget(), 1) {
                PazMain.LOGGER.info("Autopilot: {}", it.string)
            }
            "quit" -> minecraft.stop()
            else -> PazMain.LOGGER.error("Autopilot: unknown step '{}'", step)
        }
    }
}
