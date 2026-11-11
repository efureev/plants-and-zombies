# Imp

<p align="center"><img src="../../images/characters/imp.png" alt="Imp" width="320"></p>

**Zombie · small** · Plants & Zombies (original)

A small, quick zombie with a toxic bite. The [Gargantuar](gargantuar.md) carries one on its back and throws it into battle, and in Sprout Squad Imps also ride [Zombie Goats](zombie_goat.md).

| | |
|---|---|
| Id | `sproutsquad:imp` |
| Health | 20 |
| Armor | 0 |
| Damage | 1.5 |
| Speed | 0.3 |
| Knockback resistance | 0.2 |
| Size | 0.45 × 0.95 blocks |
| Spawns | in the dark in `#sproutsquad:zombie/has_imp` (the [Browncoat](browncoat.md) biomes): weight 7, alone; in the Graveyard also by day; on 40 % of naturally spawned [Zombie Goats](zombie_goat.md) as a rider (Sprout Squad) |
| Raids | regular waves from wave 5; Half-Time Showdown (in Football Helmets), Winter Wonderland (as Yeti Imps), Disco of the Undead, Zomboween, Pirate Invasion (as Pirate Imps) |
| Leader / boss | can be a leader (`can_be_leader`); never a boss |
| Drops | 5 %: [Potato Mine](potatomine.md) seed packet, if killed by a player or a plant |
| Variants | Imp, Yeti Imp, Pirate Imp |

## Abilities

- Bite: a 20 % chance to give Toxic (damage over time) for 5 s on Normal and 12 s on Hard; none on Easy.
- Faster than a Browncoat; does not pick up items; breaks doors.
- Spawns with a Cone (5 %) or a bucket (1 %) on its head; 8 % of naturally spawned Imps above sea level hang on a balloon.
- Shares the [traits of all mod zombies](browncoat.md#traits-of-all-mod-zombies).

## Variants

| Variant | Where and difference |
|---|---|
| Imp | default |
| Yeti Imp (`yeti`) | always in snowy biomes (`#sproutsquad:zombie/has_browncoat_snow`) and in Winter Wonderland; instead of Toxic, a 50 % chance per bite to give Chilled II (7.5 s on Normal, 18 s on Hard, 1 s on Easy); cannot be frozen |
| Pirate Imp (`pirate`) | Pirate Invasion and the Pirate Gargantuar; carries a powder barrel that explodes (no block damage) when the Imp comes within 1 block of its target or is killed by a direct hit |

## Interactions

- [Gargantuar](gargantuar.md): carries an Imp on its back and throws it at a target more than 4.5 blocks away once it is down to 75 % health.
- Zombie Goat rider (Sprout Squad): a goat that spawns naturally, at world generation, from a spawner or in a structure gets an Imp on its back with a 40 % chance, if there is room above.
