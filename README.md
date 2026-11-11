<p align="center">
  <img src="docs/images/logo.png" alt="Sprout Squad" width="600">
</p>

<p align="center">A Minecraft 26.3 mod for <b>NeoForge</b>: plants defend your base from zombies, and the squad keeps growing with characters of its own.</p>

<p align="center"><b>English</b> · <a href="docs/ru/README.md">Русский</a></p>

> Unofficial NeoForge fork of [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) by **joshxviii**, ported from Fabric, licensed under MPL-2.0.

## Status

The port to NeoForge and the rename to Sprout Squad are done; in-game testing is in progress.

## What the mod lets you do

- **Grow a garden of defenders.** More than 30 plants — shooters, catapults, walls, bombs, mushrooms for the night — planted from seed packets and paid for with sun.
- **Collect sun** from Sunflowers, Sun Shrooms and other producers; keep it in Bottles of Sun and Solar Batteries.
- **Fight zombies.** 20 zombie types with variants: some roam the Overworld at night, the toughest — up to Gargantuars and bosses — come with raids.
- **Run raids.** Craft a Plantz Flag from a Brainz Flag, place it and bring a Bad Omen: waves of zombies with leaders and bosses attack the flag, and rewards arrive by mail.
- **Build battle machines.** Turrets, a drone and a lawn mower from blueprints and Brainz Alloy fight on your side.
- **Use the gear.** Hypnosis and other potions, balloons, helmets, garden gnome statues, plant pots and a watering can.
- **Explore the Graveyard.** A gloomy biome with gravestones that raise zombies and mausoleums with treasure. With experimental features on, the Time Machine opens era biomes: Ancient Egypt, Dark Ages, Far Future, Jurassic Period.

The full guide is in [FEATURES](docs/en/FEATURES.md); every plant, zombie and machine has its own card in [characters](docs/en/characters/README.md).

What the fork adds on top of the original:

- **Fixes for content you could not get or use**: battle machines built from blueprints fight for the player, the lawn mower works as the last line of defence, Squash attacks, brewable potions, loot for raids and mobs, Zombie Goat spawns, no kicks for flying on balloons ([full list](docs/en/UPSTREAM.md)).
- **Its own characters**, starting with the mascot plant [Potling](docs/en/characters/potling.md).
- **New structures**: the [Garden Shack](docs/en/STRUCTURES.md#garden-shack), an abandoned garden with wild plants to tame, and the [Robo Junkyard](docs/en/STRUCTURES.md#robo-junkyard), a scrap yard guarded by turrets, with blueprints in its workshop.

| | |
|---|---|
| Minecraft | 26.3 |
| Loader | NeoForge 26.3.x |
| Dependencies | Kotlin for Forge; optional TerraBlender, Iris |
| Java | 25 |

## Installing

Download `sprout-squad-<version>.jar` from [Releases](https://github.com/efureev/plants-and-zombies/releases) and put it into the `mods` folder of a NeoForge 26.3 installation together with [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge). Step by step, for the official launcher, Prism Launcher and a server: [INSTALL](docs/en/INSTALL.md).

## Building

```bash
./gradlew build              # the jar ends up in build/libs/ (sprout-squad-<version>.jar)
./gradlew runClient          # start a client for testing
./gradlew runServer          # start a dedicated server
./gradlew runGameTestServer  # load the mod and run its GameTests, then exit
```

JDK 25 is required.

Debug commands (server operator, level 2):

```text
/pz gallery           # line up every plant, zombie and variant in front of you (with name labels)
/pz gallery false     # the same without labels, for screenshots
/pz gallery clear     # remove the gallery (the pedestals stay)
```

## Documentation

English is the main version; the same documents are available [in Russian](docs/ru/README.md).

| Document | English | Русский |
|---|---|---|
| How to install the mod, play on a server, update | [INSTALL](docs/en/INSTALL.md) | [INSTALL](docs/ru/INSTALL.md) |
| Full guide: what the mod lets you do | [FEATURES](docs/en/FEATURES.md) | [FEATURES](docs/ru/FEATURES.md) |
| Characters: a card for every plant, zombie and machine | [characters](docs/en/characters/README.md) | [characters](docs/ru/characters/README.md) |
| How to add a character, style guide | [WORKSHOP](docs/en/WORKSHOP.md) | [WORKSHOP](docs/ru/WORKSHOP.md) |
| Structures: what is in the world, planned ones with sketches, how to make one | [STRUCTURES](docs/en/STRUCTURES.md) | [STRUCTURES](docs/ru/STRUCTURES.md) |
| Upstream changes and fixes to the original | [UPSTREAM](docs/en/UPSTREAM.md) | [UPSTREAM](docs/ru/UPSTREAM.md) |

## License and credits

The code is based on [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) (© joshxviii) and is distributed under the [Mozilla Public License 2.0](LICENSE). Files of the original that were changed in this fork remain under MPL-2.0.

Plants vs. Zombies is a trademark of PopCap Games / Electronic Arts. This project is not affiliated with them.
