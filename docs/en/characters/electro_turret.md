# Electro Turret

<p align="center"><img src="../../images/characters/electro_turret.png" alt="Electro Turret" width="320"></p>

**Machine · electric** · Plants & Zombies (original); Sprout Squad made player-built turrets fight for the player

A stationary turret that strikes nearby targets with an electric arc and leaves them Electrified. Engineer zombies build it against players and plants; a player can build one from a blueprint as a short-range guard.

| | |
|---|---|
| Id | `sproutsquad:electro_turret` |
| Health | 8 |
| Armor | 7 |
| Size | 0.8 × 1.0 blocks |
| Movement | none: it stays snapped to the centre of its block |
| Attack | an electric zap about every 1.5 s, reach about 8 blocks, 2 damage + Electrified for 9 s |
| Knockback resistance | 0.2 |
| Blueprint cost | 10 Brainz Alloy |
| Spawns | built by an [Engineer Zombie](engineer_zombie.md); never spawns on its own |
| How to get a blueprint | [Engineer Zombie](engineer_zombie.md) drop (8.5 %, killed by a player or a plant); treasure in the hero mail for a Robo Army raid wave |

## Abilities

- The zap is instant: no projectile flies, so it cannot miss.
- Electrified: while the effect lasts, electric damage to the target also arcs to one more creature within 3 blocks on the target's side, for half the damage; an Electrified creature standing in water keeps taking small zaps.
- Never moves, does not take fall damage and cannot be hypnotized.
- In Creative, pick block on the turret gives its blueprint.

## Two kinds

**Hostile (Engineer-built).** An [Engineer Zombie](engineer_zombie.md) assembles a random machine (this turret, the [Zombie Turret](zombie_turret.md) or the [Zombie Drone](zombie_drone.md)) next to itself when it has a target and fewer than two hostile machines are within 16 blocks. It joins the raid and zaps players and plants that come close. A turret from a spawn egg is hostile too.

**Player-built (from a blueprint).** Use a `sproutsquad:blueprint` for the Electro Turret on a block: it needs 10 `sproutsquad:brainz_alloy` in the inventory (none in Creative) and uses one of the blueprint's 20 uses. The turret faces the player and is marked as the player's machine. It then:

- zaps only monsters hostile to players: not creepers, neutral mobs (endermen, zombified piglins), hypnotized zombies or plants;
- is never targeted or hit by plants, and its attacks spare players, plants and other players' machines;
- ignores the raid flag and never despawns, even on Peaceful.

## Interactions

- Brainz Alloy: shapeless recipe, 1 Brainzium + 4 copper ingots + 4 iron ingots → 16 alloy.
- Plants fight hostile Electro Turrets like any zombie; [Hypno Shroom](hypnoshroom.md) cannot turn them.
- The [Zombie Turret](zombie_turret.md) reaches 20 blocks but does not electrify.
