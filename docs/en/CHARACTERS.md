# Sprout Squad characters

The mod's own characters. How to come up with and add a character, the checklist and the style guide — `docs/en/WORKSHOP.md`.

Code lives in the `dev.efureev.sproutsquad.content.*` package, Blockbench sources and sketches in `art/<id>/`.

## Registry

| Id | Name | Type | Status |
|---|---|---|---|
| `potling` | Potling (Russian: Горшочек) | plant | in code |

Statuses: `idea` → `model` → `in code` → `done`.

---

### Potling (Russian: Горшочек)

| | |
|---|---|
| Id | `sproutsquad:potling` |
| Type | plant |
| Idea by | the mod's mascot (icon and logo) |
| Sketch | mod icon `src/main/resources/sprout_squad_icon.png`; textures are drawn by `art/potling/draw_potling.py` |
| Status | in code |

**What it does:** a terracotta pot with a face and a sprout. About once a minute, if the sun is visible, it shakes itself and drops a sun; at night it sleeps (the sprout nods, the leaves droop). Does not attack. It is also the template for new plants: the whole path from `docs/en/WORKSHOP.md` was tested on it.

**Stats:**

| Stat | Value | Closest counterpart in the mod |
|---|---|---|
| Health | 12 | Sunflower — 20, Puff Shroom — 12 |
| Damage | — | — |
| Sun | about once every 60 s while the sun is visible | Sunflower — about once every 45 s |
| Sun cost | 3 | Sunflower — 5 |

**How to get:** shapeless recipe — flower pot + wheat seeds + 3 sun → Potling seed packet. Together with planting (3 sun) it costs more than a Sunflower, so the recipe does not become an endless sun farm.

**Sounds:** the original mod's sounds for now.

**Notes:** cheaper and weaker than the Sunflower — a starter plant for the first evening.

---

## Card template

Copy the block below for a new character.

```markdown
### <English name> (Russian: <Russian name>)

| | |
|---|---|
| Id | `sproutsquad:<id>` |
| Type | plant / zombie / other |
| Idea by | |
| Sketch | `art/<id>/sketch.png` |
| Status | idea |

**What it does:** attack, defense, special ability.

**Stats:**

| Stat | Value | Closest counterpart in the mod |
|---|---|---|
| Health | | |
| Damage | | |
| Attack speed | | |
| Sun cost (plants) | | |
| Where it spawns (zombies) | | |

**How to get:** recipe / loot / trade / seed mutation.

**Sounds:**

**Notes:**
```
