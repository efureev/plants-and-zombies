# Zombie Drone

**Machine · flying melee** · Plants & Zombies (original); Sprout Squad made player-built drones fight for the player

A small flying robot that strikes its target in melee. Hostile drones come with Engineer zombies and the Robo Army; a player can build one from a blueprint to chase monsters.

| | |
|---|---|
| Id | `sproutsquad:zombie_drone` |
| Health | 8 |
| Armor | 7 |
| Size | 0.8 × 1.0 blocks |
| Movement | flies (flying speed 0.22), hovering about 2.5 blocks above the ground |
| Attack | a melee hit about every 1 s, 3 damage; notices targets up to 28 blocks away |
| Knockback resistance | −2: hits throw it far back |
| Blueprint cost | 8 Brainz Alloy |
| Spawns | built by an [Engineer Zombie](engineer_zombie.md); 2–10 per wave in the Robo Army raid wave |
| How to get a blueprint | [Engineer Zombie](engineer_zombie.md) drop (8.5 %, killed by a player or a plant); treasure in the hero mail for a Robo Army raid wave |

## Abilities

- Has no gravity and takes no fall damage; with no target it drifts to random spots nearby, keeping its hover height.
- Flies to its target and attacks in melee; the cheapest machine to build.
- Cannot be hypnotized. In Creative, pick block on a drone gives its blueprint.

## Two kinds

**Hostile.** An [Engineer Zombie](engineer_zombie.md) builds a random machine (this drone, the [Zombie Turret](zombie_turret.md) or the [Electro Turret](electro_turret.md)) when it has a target and fewer than two hostile machines are within 16 blocks. The Robo Army special wave (raid waves 10–16, only for players who have seen the end credits) also brings drones directly, alongside [Robo-Zombies](robo_zombie.md), Engineers and [Soldier Zombies](soldier_zombie.md). Hostile drones join the raid and attack players and plants. A drone from a spawn egg is hostile too.

**Player-built (from a blueprint).** Use a `sproutsquad:blueprint` for the Zombie Drone on a block: it needs 8 `sproutsquad:brainz_alloy` in the inventory (none in Creative) and uses one of the blueprint's 20 uses. The drone is marked as the player's machine. It then:

- hunts only monsters hostile to players: not creepers, neutral mobs (endermen, zombified piglins), hypnotized zombies or plants;
- is never targeted or hit by plants and does not attack players or other players' machines;
- ignores the raid flag and never despawns, even on Peaceful;
- does not follow the player and does not guard its build spot: with no target it keeps drifting around.

## Interactions

- Brainz Alloy: shapeless recipe, 1 Brainzium + 4 copper ingots + 4 iron ingots → 16 alloy.
- Plants fight hostile drones like any zombie.
