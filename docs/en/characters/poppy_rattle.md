# Poppy Rattle

<p align="center"><img src="../../images/characters/poppy_rattle.png" alt="Poppy Rattle" width="320"></p>

**Plant · seed producer** · Sprout Squad

A poppy seed head with a face, a crown-shaped lid and four red petals. Once by day and once by night it grows a seed packet of a random plant and keeps it under its lid until its owner comes for it. Now and then it shakes like a maraca: the seeds rattle inside.

| | |
|---|---|
| Id | `sproutsquad:poppy_rattle` |
| Health | 16 |
| Sun cost | 6 |
| Size | 0.6 × 1.0 blocks |
| Seeds | 1 seed packet a day and 1 a night, holds up to 2 |
| Attack | none |
| At night | does not sleep; its eyes glow |
| Asleep | only while it grows its own seeds on farmland, like any plant |
| Limit | 4 within 32 blocks |
| How to get | shapeless recipe: poppy + torch + 6 sun → Poppy Rattle seed packet; does not grow in the wild |

## Abilities

- Once a day it grows a seed packet of a plant that is awake by day (any plant but the mushrooms), once a night one of a plant that is awake at night (any plant but the Sunflower and the Potling). The moment is random, within the first half of the day or the night.
- Cheaper plants come up more often: one costing up to 4 sun four times as often as one costing 11 or more. The same plant never comes twice in a row. It never hands out a Marigold, a Citron, a Royal Sunflower or another Poppy Rattle.
- It holds up to two packets, and you can see how many: the lid is shut when it is empty, ajar with one packet showing, flipped open with two packets fanned out. While it is full it grows nothing.
- Right-click it with an empty hand to take the packets (its owner, or anyone with co-op planting). Sneaking with an empty hand still puts it on your head.
- A night slept through in a bed still gives its packet. On farmland, watered, it sleeps while growing its own seed packet like any plant (its petals fold up, no glowing eyes) and keeps counting days and nights. A newly planted Poppy Rattle starts with the next day or night; worn on a head it grows nothing.
- When it dies or is dug up with a shovel, its packets drop. Carried in a Gardening Glove it keeps them.
- Days and nights follow the clock, so a daytime thunderstorm is not a night. In the Nether and the End, where days do not change, it grows nothing new.

<!-- pictures: poppy_rattle_night -->
<p><img src="../../images/characters/poppy_rattle_night.png" alt="Poppy Rattle at night" title="Poppy Rattle at night" width="112"></p>

## Card

| | |
|---|---|
| Idea | the idea's author: a plant that gives a random plant's seeds once by day and once by night |
| Sketch | textures are drawn by `art/poppy_rattle/draw_poppy_rattle.py` |
| Status | in code |
