# Squash

<p align="center"><img src="../../images/characters/squash.png" alt="Squash" width="320"></p>

**Plant · crusher** · Plants & Zombies (original); attack by Sprout Squad

A single-use plant that leaps onto a nearby zombie and flattens it. In the original mod the Squash had no attack; the leap and slam were added by Sprout Squad.

| | |
|---|---|
| Id | `sproutsquad:squash` |
| Health | 20 |
| Sun cost | 4 |
| Size | 0.8 × 0.9 blocks |
| Attack | one slam: 1000 damage (40 to bosses) to every valid target under it; single use |
| Trigger | a target within 3 blocks horizontally and 1.5 blocks up or down |
| At night | stays awake |
| How to get | grows wild in plains, savannas and the Dappled Forest; a wild one is tamed with sun (~45% per sun) |

## Abilities

- Winds up for about 2 s, leaps, hangs 2 blocks above the target, slams down at ~3.65 s and disappears at 4 s.
- During the leap it follows the target, but no further than ~4.5 blocks from where it stood.
- The slam hits every valid target in a 1.6 × 2 × 1.6 block area around the landing point, so zombies standing together are squashed at once.
- Squashes only zombies; a tamed Squash also squashes other hostile mobs. It never hurts players, plants, villagers or pets.
- If the target dies or walks away before the leap, it cancels and waits for the next one; once in the air, it always finishes.
- Does not attack while worn on a player's head or riding a Minecart with Plant Pot.
- Survives on any block and does not turn to look around.

## Interactions

- If it wore a [Pumpkin](pumpkin.md), the Pumpkin is left at the landing spot.
- Sprout Squad code: `dev/efureev/sproutsquad/fixes/SquashAttackGoal.kt`.
