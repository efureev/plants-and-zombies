# Zombie Turret

**Machine · shooter** · Plants & Zombies (original); Sprout Squad made player-built turrets fight for the player

A stationary laser gun. Engineer zombies build it to fire at players and plants; a player who has its blueprint can build one to guard the base against monsters.

| | |
|---|---|
| Id | `sproutsquad:zombie_turret` |
| Health | 8 |
| Armor | 7 |
| Size | 0.8 × 1.0 blocks |
| Movement | none: it stays snapped to the centre of its block |
| Attack | a laser bolt about every 0.8 s, up to 20 blocks; 0.5 damage (hostile) or 2 damage (player-built) |
| Knockback resistance | 0.2 |
| Blueprint cost | 12 Brainz Alloy |
| Spawns | built by an [Engineer Zombie](engineer_zombie.md); never spawns on its own |
| How to get a blueprint | [Engineer Zombie](engineer_zombie.md) drop (8.5 %, killed by a player or a plant); treasure in the hero mail for a Robo Army raid wave |

## Abilities

- Shoots straight laser bolts with no gravity and a wide spread; it does not lead moving targets.
- Never moves, does not take fall damage and cannot be hypnotized.
- In Creative, pick block on a turret gives its blueprint.

## Two kinds

**Hostile (Engineer-built).** An [Engineer Zombie](engineer_zombie.md) assembles a random machine (this turret, the [Electro Turret](electro_turret.md) or the [Zombie Drone](zombie_drone.md)) next to itself when it has a target and fewer than two hostile machines are within 16 blocks. It joins the raid, targets players and plants, and is immune to the zombies' own damage types (its lasers never hurt zombies). A turret placed from a spawn egg is hostile too.

**Player-built (from a blueprint).** Use a `sproutsquad:blueprint` for the Zombie Turret on a block. The build needs 12 `sproutsquad:brainz_alloy` in the inventory (none in Creative), costs one of the blueprint's 20 uses, and fails if the spot is blocked or already occupied by a creature. The turret faces the player and is marked as the player's machine. It then:

- shoots only monsters hostile to players: not creepers, endermen, zombified piglins or other neutral mobs, not hypnotized zombies, not plants;
- never hits players, plants or other players' machines, and plants never target or hit it;
- hits for its full attack damage of 2 instead of 0.5;
- ignores the raid flag and never despawns, even on Peaceful.

## Interactions

- Brainz Alloy: shapeless recipe, 1 Brainzium + 4 copper ingots + 4 iron ingots → 16 alloy; Engineers also drop 1–2 (30 %).
- Plants fight hostile turrets like any zombie; [Hypno Shroom](hypnoshroom.md) cannot turn them.
- The [Electro Turret](electro_turret.md) is the short-range, electric counterpart.
