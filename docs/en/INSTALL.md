# Installing Sprout Squad

How to put the mod into the game, play together on a server, update and remove it. Русская версия: [docs/ru/INSTALL.md](../ru/INSTALL.md).

- [What you need](#what-you-need)
- [Official Minecraft Launcher](#official-minecraft-launcher)
- [Prism Launcher](#prism-launcher)
- [A server to play together](#a-server-to-play-together)
- [Updating and removing](#updating-and-removing)
- [If something goes wrong](#if-something-goes-wrong)

## What you need

| What | Version | Where to get it |
|---|---|---|
| Minecraft: Java Edition | 26.3 | a licensed copy and the [Minecraft Launcher](https://www.minecraft.net/download) |
| NeoForge | 26.3.0.102 or newer (26.3.x) | [neoforged.net](https://neoforged.net/) |
| Kotlin for Forge | 6.5.0 or newer, the NeoForge build for 26.3 | [Modrinth](https://modrinth.com/mod/kotlin-for-forge) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) |
| Sprout Squad | the latest release | [GitHub Releases](https://github.com/efureev/plants-and-zombies/releases): `sprout-squad-<version>.jar` |
| TerraBlender | optional | [Modrinth](https://modrinth.com/mod/terrablender) |

- **Kotlin for Forge is required**: the mod is written in Kotlin, and without this library the game stops at a "missing dependency" screen.
- **TerraBlender is optional.** The Graveyard biome appears without it too; with it, the Graveyard shares the Overworld with other biome mods that use TerraBlender.
- The mod is needed **on the client and on the server**: a player without it cannot join a server with it, and the other way round.
- Sprout Squad already contains Plants & Zombies: do not add the original mod next to it (it is built for Fabric, and the two would clash).

## Official Minecraft Launcher

1. In the launcher, start Minecraft **26.3** once without mods and close it: the launcher downloads the game.
2. Download the **NeoForge installer** for 26.3 from [neoforged.net](https://neoforged.net/) and run it (double-click, or `java -jar neoforge-<version>-installer.jar`). Choose **Install client** and press OK.
3. In the launcher, a **NeoForge** installation appears. Start it once and close the game: this creates the `mods` folder.
4. Open the game folder and its `mods` folder:
   - Windows: `%APPDATA%\.minecraft\mods` (type it into the Explorer address bar);
   - macOS: `~/Library/Application Support/minecraft/mods` (in Finder: Go → Go to Folder);
   - Linux: `~/.minecraft/mods`.
5. Put into `mods`: `sprout-squad-<version>.jar`, the Kotlin for Forge jar and, if you want it, TerraBlender.
6. Start the **NeoForge** installation. In the main menu, **Mods** lists Sprout Squad and Kotlin for Forge.

## Prism Launcher

[Prism Launcher](https://prismlauncher.org/) keeps every modded setup in its own instance, apart from the normal game.

1. **Add Instance** → Minecraft **26.3**, mod loader **NeoForge** (26.3.0.102 or newer) → OK.
2. **Edit** the instance → **Mods** → **Add file** → choose `sprout-squad-<version>.jar`.
3. In the same window, **Download mods** → find **Kotlin for Forge** (and TerraBlender, if you want it) → Review and confirm.
4. **Launch**.

## A server to play together

On one computer at home it is easiest to **Open to LAN** from the pause menu of a single-player world: the others on the same network join from **Multiplayer**. Everybody needs the same mods.

A dedicated server:

1. Install **Java 25** (Minecraft 26.3 needs it), for example [Eclipse Temurin](https://adoptium.net/).
2. Run the same NeoForge installer and choose **Install server** into an empty folder (or `java -jar neoforge-<version>-installer.jar --installServer`).
3. Start it once with `run.bat` (Windows) or `./run.sh` (macOS, Linux). It stops and asks you to accept the EULA: set `eula=true` in `eula.txt`.
4. Put the same `sprout-squad-<version>.jar` and Kotlin for Forge (and TerraBlender, if the clients have it) into the server's `mods` folder.
5. Start the server again. Players connect with **Multiplayer → Add Server**; their mods must be the same versions as the server's.

The server's options are in `config/sproutsquad/server.json` (described in [FEATURES](FEATURES.md#server-options-configsproutsquadserverjson)); the Time Machine and Gnome Space work only with `enableExperimentalFeatures` turned on.

## Updating and removing

- **Updating:** make a backup of your worlds (the `saves` folder), delete the old `sprout-squad-*.jar` from `mods` and put the new one there. Keep only one Sprout Squad jar. Update the server and every player together.
- **Removing:** delete the jar. A world that was played with the mod loses its plants, zombies, blocks and items; the game warns about it when you open the world. Keep a backup if you may want to return.

## If something goes wrong

| What you see | What to do |
|---|---|
| A screen about a missing dependency: `kotlinforforge` | Install Kotlin for Forge 6.5.0 or newer, the NeoForge build for 26.3 |
| A screen that the mod needs another Minecraft or NeoForge version | The mod is for Minecraft 26.3 and NeoForge 26.3.0.102 or newer: check the installation's version in the launcher |
| "Duplicate mods" or two Sprout Squads | Leave one `sprout-squad-*.jar` in `mods` and delete the others |
| Cannot join a server: mods do not match | Install the same versions of Sprout Squad and Kotlin for Forge as on the server |
| The game crashes | The details are in `logs/latest.log` and `crash-reports/` in the game folder. Open an [issue](https://github.com/efureev/plants-and-zombies/issues) and attach the log |

What the mod lets you do once it is in the game: [FEATURES](FEATURES.md).
