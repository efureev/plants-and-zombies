package dev.efureev.sproutsquad.debug.client

import joshxviii.plantz.PazMain
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.screens.PauseScreen
import net.neoforged.neoforge.client.gui.LoadingErrorScreen
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.common.NeoForge
import java.io.File

/**
 * Development aid for checking the game by eye without anyone at the keyboard: plays a script of
 * commands, waits and screenshots in a singleplayer world, then closes the game.
 *
 * Off unless the JVM is started with `-Dsproutsquad.autopilot=<script file>` (`./gradlew runClient -Pautopilot=<file>`,
 * see build.gradle). One step per line; a line starting with `#` is a comment (only a whole line: commands use `#` for tags):
 * - `wait <ticks>` — do nothing for that many client ticks (20 a second, 0 or more);
 * - `cmd <command>` — run a command as the player, with full permissions, without the leading slash; the next step
 *   waits until the server has run it, but what it changes reaches the screen a few ticks later (`wait 5` before a `shot`);
 * - `hud on|off` — show or hide the HUD (F1);
 * - `shot <name>` — save `screenshots/<name>.png` in the game directory, from a frame drawn after the steps before it;
 * - `quit` — close the game.
 * The script starts once the player is in the world. While it runs, losing focus does not pause the game; the
 * setting is restored when the script ends.
 */
object Autopilot {
    private const val PROPERTY = "sproutsquad.autopilot"
    private const val COMMAND_TIMEOUT = 200

    private var steps: ArrayDeque<String> = ArrayDeque()
    private var waitTicks = 0
    private var lastScreen: String? = null
    /** A `cmd` the server has not run yet: the script waits for it, so steps keep their order. */
    @Volatile private var commandPending = false
    /** Client ticks spent waiting for [commandPending]; past [COMMAND_TIMEOUT] the script gives up on it. */
    private var commandWaitTicks = 0
    /** Frames drawn so far, and when the last step ran: a `shot` waits for a frame drawn after the steps before it. */
    @Volatile private var frames = 0L
    private var lastStepFrame = -1L
    /** The player's "pause on lost focus" while the script overrides it; null when it does not. */
    private var savedPauseOnLostFocus: Boolean? = null

    fun init() {
        val path = System.getProperty(PROPERTY) ?: return
        val file = File(path)
        if (!file.isFile) {
            PazMain.LOGGER.error("Autopilot: no script at {}", file.absolutePath)
            return
        }
        steps = ArrayDeque(file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") })
        PazMain.LOGGER.info("Autopilot: {} steps from {}", steps.size, file.absolutePath)
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post::class.java) { tick(Minecraft.getInstance()) }
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post::class.java) { frames++ }
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

    private fun restoreOptions(minecraft: Minecraft) {
        savedPauseOnLostFocus?.let { minecraft.options.pauseOnLostFocus = it }
        savedPauseOnLostFocus = null
    }

    private fun tick(minecraft: Minecraft) {
        // a screen in the way (a warning, an error) stops the script: say which, so the log shows it
        val screen = minecraft.gui.screen()?.javaClass?.simpleName
        if (screen != lastScreen) {
            lastScreen = screen
            PazMain.LOGGER.info("Autopilot: screen {}", screen ?: "none")
            (minecraft.gui.screen() as? LoadingErrorScreen)?.let { continuePastWarnings(minecraft, it) }
        }
        val player = minecraft.player
        val server = minecraft.singleplayerServer
        if (player == null || server == null) {
            restoreOptions(minecraft) // out of the world (kicked, the world failed): the player's own setting again
            return
        }
        if (steps.isEmpty() && waitTicks == 0 && !commandPending) {
            restoreOptions(minecraft) // the script is over: Esc and focus work as usual again
            return
        }
        // nobody is at the keyboard: losing focus (another window, another desktop) must not pause the script
        if (savedPauseOnLostFocus == null) savedPauseOnLostFocus = minecraft.options.pauseOnLostFocus
        minecraft.options.pauseOnLostFocus = false
        if (minecraft.gui.screen() is PauseScreen) minecraft.gui.setScreen(null)
        if (minecraft.level == null || minecraft.gui.screen() != null) return
        if (commandPending) {
            if (++commandWaitTicks > COMMAND_TIMEOUT) {
                PazMain.LOGGER.error("Autopilot: the server did not run the last command in {} ticks, going on", COMMAND_TIMEOUT)
                commandPending = false
            }
            return
        }
        if (waitTicks > 0) {
            waitTicks--
            return
        }
        val step = steps.firstOrNull() ?: return
        val (verb, arg) = step.split(Regex("\\s+"), limit = 2).let { it[0] to it.getOrElse(1) { "" }.trim() }
        // a screenshot shows the frame drawn last: wait for one drawn after the previous step (a hidden HUD, a change)
        if (verb == "shot" && frames <= lastStepFrame) return
        steps.removeFirst()
        lastStepFrame = frames
        PazMain.LOGGER.info("Autopilot: {}", step)
        when (verb) {
            "wait" -> waitTicks = arg.toIntOrNull()?.takeIf { it >= 0 }
                ?: 0.also { PazMain.LOGGER.error("Autopilot: '{}' needs a number of ticks, 0 or more", step) }
            "cmd" -> {
                commandPending = true
                commandWaitTicks = 0
                server.execute {
                    try {
                        val serverPlayer = server.playerList.getPlayer(player.uuid)
                            ?: return@execute PazMain.LOGGER.error("Autopilot: no player on the server, skipped '{}'", step)
                        val source = server.createCommandSourceStack().withEntity(serverPlayer)
                            .withPosition(serverPlayer.position()).withRotation(serverPlayer.rotationVector).withLevel(serverPlayer.level())
                        server.commands.performPrefixedCommand(source, arg)
                    } finally {
                        commandPending = false
                    }
                }
            }
            "hud" -> if (minecraft.gui.hud.isHidden != (arg == "off")) minecraft.gui.hud.toggle()
            "shot" -> Screenshot.grab(minecraft.gameDirectory, "$arg.png", minecraft.gameRenderer.mainRenderTarget(), 1) {
                PazMain.LOGGER.info("Autopilot: {}", it.string)
            }
            "quit" -> {
                restoreOptions(minecraft)
                minecraft.stop()
            }
            else -> PazMain.LOGGER.error("Autopilot: unknown step '{}'", step)
        }
    }
}
