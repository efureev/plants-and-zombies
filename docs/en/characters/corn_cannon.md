# Corn Cannon

<p align="center"><img src="../../images/characters/corn_cannon.png" alt="Corn Cannon" width="320"></p>

**Plant · artillery** · Sprout Squad

A long corn husk lying on a rosette of leaves, with an eye on each side, a curly tendril at the back and a big corn cob sticking out of the front. It lifts its nose and launches the cob like a rocket in a high arc; the cob blows up where it lands and the cannon grows a new one in 3 seconds.

| | |
|---|---|
| Id | `sproutsquad:corn_cannon` |
| Health | 90 |
| Sun cost | 14 |
| Size | 1.2 × 0.9 blocks (the model is about 1.8 blocks long) |
| Attack | a corn cob every 3.5 s; it explodes on impact: 8 damage at the centre, down to 4 at the edge, radius 2.5 blocks |
| Range | 5 to 40 blocks, no line of sight needed |
| At night | does not sleep |
| Limit | 10 within 32 blocks |
| How to get | crafting: a [Kernel Pult](kernelpult.md) seed packet in the middle and 8 sun around it → Corn Cannon seed packet; does not grow wild |

## Abilities

- Targets zombies of any kind; a tamed Corn Cannon also other monsters, but never creepers.
- It does not see zombies within 5 blocks: it does not aim at one that close, and lets go of a target that comes within 5 blocks.
- It does not aim at a zombie under a roof, a tree, scaffolding or in a cave: the cob comes down from the sky and would only hit the roof. Water is no roof: the cob flies through it at full speed, so a zombie at the bottom of a pool is still a target.
- When three of its blasts in a row burst right over its target or next to it behind cover (an overhang, a wall) without reaching anyone, it leaves that zombie alone for 5 seconds and picks another. A blast in plain view that a walking zombie simply dodged does not count, nor does one that reached another enemy; either also breaks the run.
- Before a shot it follows the cob's path to where it would burst: if that blast would not reach the zombie (a wall or leaves by the cannon, a hill, a house or a tree canopy in the way, a zombie too high to reach), it does not fire and leaves that zombie alone for 5 seconds. After three such shots in a row it rests for 5 seconds.
- Hypnotized zombies and a player's machines are allies: it never aims at them, and its cob never hurts them.
- A shot: the nose lifts and the eyes squint, the cob leaves about half a second later, and the husk kicks back. The empty husk is dark inside until the new cob has grown back out of it. A shot called off during the aim (the zombie came too close or went under cover) uses up no cob: the cannon goes through the motion without firing.
- The cob flies in a high arc, over walls and fences, and aims ahead of moving targets. It has the same cone at both ends and leaves no flame or smoke.
- The blast hurts only what the cannon shoots at. Players, pets, villagers and plants are not hurt, and the cob flies through them. A wall between the blast and a zombie shields it. It breaks no blocks and sets no fire, but the block it lands on reacts as to an arrow (a target block gives a signal). Popcorn flies out of the blast.
- A bigger cannon (the scale attribute) makes a bigger blast.

## Notes

- It needs open sky above it: under a roof or a tree it does not shoot at all, since every cob would hit the ceiling. Scaffolding above it counts as a roof too: a rising cob would get stuck in it.
- About twice as strong as the [Melon Pult](melonpult.md), as its author wanted; the price is the Kernel Pult and 8 sun in the recipe, 14 sun to plant and the limit of 10.
- The [Poppy Rattle](poppy_rattle.md) never hands it out.
- Uses the original mod's sounds for now: a low shot and the missile's explosion.

## Interactions

- The seed packet recipe in a crafting table:

  | | | |
  |---|---|---|
  | Sun | Sun | Sun |
  | Sun | Kernel Pult seed packet | Sun |
  | Sun | Sun | Sun |

- Zombies held by butter from a [Kernel Pult](kernelpult.md) are easy targets for its slow cob.

## Card

| | |
|---|---|
| Idea | the idea's author: a corn cannon that fires its cob like a rocket, 90 health, 14 sun, 8 damage, a 3-second reload, crafted from a Kernel Pult and sun |
| Sketch | `art/corn_cannon/sketch.jpg`; textures are drawn by `art/corn_cannon/draw_corn_cannon.py` |
| Status | in code |
