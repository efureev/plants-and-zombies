# Syncing with the original

Original: https://github.com/joshxviii/plants-and-zombies (remote `upstream`, branch `26.3`).

Fork point: `977cfb46` (tag `upstream-base-977cfb46`, mod v1.5.2).

Changes from the original are brought over only via `git cherry-pick`.

Since milestone M8 the fork has its own mod id `sproutsquad`, while the original uses `plantz`. After every cherry-pick:

```bash
python3 tools/upstream/rename_modid.py .                 # strings in code and resources
python3 tools/upstream/nbt_rename.py <new .nbt files>    # structures (requires nbtlib)
```

Move any files that land in `assets/plantz` or `data/plantz` to `assets/sproutsquad` / `data/sproutsquad`.

## Viewing new commits

```bash
git fetch upstream
git log --oneline upstream-base-977cfb46..upstream/26.3
```

## Log

| Date | Upstream commit | Fork commit | What | Note |
|---|---|---|---|---|
| — | — | — | — | nothing brought over yet |

## Original-mod bugs fixed in the fork

Found while launching the port; they are present in the Fabric version too. Fixed in the `fix/upstream-data` branch; these files may conflict when changes are brought over from the original.

| Problem | Fix |
|---|---|
| The `bigger_three`/`biggest_three` advancements did not load: the parent `plantz:robo_zombie` does not exist | The parent is `plantz:big_three`; the Robo-Zombie criterion label is fixed |
| The taco recipe uses the empty tags `c:crops/tomato`, `c:crops/onion`, `c:foods/leafy_green` (without farming mods) | The recipe and its unlock advancement load only when the tags are non-empty (`neoforge:conditions`) |
| 58 untranslated subtitles; five death sounds use the key `"subtitles"`, the gnome's keys lack the prefix, `scaredyshroom.peek` and `super_brainz.punch/lazer` have other sounds' subtitles | Keys in `sounds.json` fixed, texts added to `en_us` and `ru_ru` |
| `music.gnome_space` pointed to the nonexistent `music/raid/low.ogg` | Now points to `music/raid/loon_griefers_low.ogg` |
| The sounds `entity.gnome.voice_1.jump`, `voice_2.jump`, `entity.snowchunk` are registered but not defined in `sounds.json` | Added (the jump uses the shared gnome sound, the snow chunk uses the vanilla snow sound) |
| No texture for the Giga-Gargantuar spawn egg | Drawn in the style of the Giga-Football Zombie egg: charcoal clothing, bright red details |
| The Time Machine portal particle texture has the wrong namespace | `plantz:block/time_machine/portal1` |
| Door Shield model: wrong door texture paths, no particle texture | Paths `minecraft:block/oak_door_*`, `particle` |
| `LivingEntityAccessor` — `@Invoker` on the mod's own methods (Mixin warnings) | Replaced with a plain interface `PazEffectFlags` |
| The pot particle textures `plant_pot_particle` and `zen_plant_pot_particle` are 14×14, so Minecraft disabled mipmaps for the whole block atlas; half of the Zen Plant Pot texture is transparent | Redrawn at 16×16: the center is the block's top face, surrounded by a border (terracotta / bamboo stand) |
| Russian localization: 111 untranslated strings, 13 obsolete keys, 3 strings missing `%s`, typos | Translation completed to 100%, obsolete keys removed |
| F2: Brainzium cannot be obtained in survival (the only source is the ZOMBOSS wave reward with weight 0) | Temporary source: 25% in the boss bonus (`raid/mail_reward_bonus`), a rare chance in the treasure of post-credits waves (`raid/army/treasure`, `raid/league/treasure`) |
| F2: the Party Hat never drops (the disco wave zombies have a 0 chance, no loot) | Backup Dancer loot table (8%); the Disco Party wave has its own reward `raid/mail_reward_disco` with the treasure `raid/disco/treasure` (`WaveType.DISCO_PARTY`, `PazLootTables.MAIL_REWARD_DISCO`) |
| F2: `smooth_brainz_alloy_block` has no recipe, loot table, name or place in the creative tab | Furnace and stonecutter recipes from the alloy block, loot table, translation, creative tab, unlock in `recipes/brainz_alloy_blocks` |
| F2: the Lawn Flamingo `lawn_flamingo` breaks into nothing | Block loot table |
| F2: empty treasures `raid/winter`, `raid/zomboween`, `raid/zomboss`; the Zomboween reward used `default_hard/treasure`; `raid/zomboween/seeds` has an inverted roll range (min 2, max 1) | Treasures filled; the Zomboween reward points to its own treasure, range 1–2; the Zomboss reward also rolls its own treasure |
| F2: no loot tables for Imp, Pirate Captain, Pirate Captain Ghost, Zombat, Zombie Goat, Backup Dancer, Gnome | Added, as for the other zombies — only when killed by a player or a plant; the gnome drops a statuette of its own color (`minecraft:nbt` predicate) |
| F2: the Hypnosis, Buttered and Electricity potions are registered but cannot be brewed | Brewing recipes `recipe/brewing/*` (in 26.3 brewing is defined by recipes): Awkward Potion + Red Mushroom, Honeycomb or Lightning Rod (`#minecraft:lightning_rods`), gunpowder and dragon's breath as for vanilla potions; arrow names |
| F2: the Brainz Flag is only found in the mausoleum treasure chest (the first raid depends on a graveyard) | The Grave Digger drops the flag (5%), the Wandering Trader sells it (rare trade, 12 emeralds) |
| F2: the taco can only be crafted with farming mods | Added a vanilla recipe `taco_from_vanilla` (bread, cooked beef, beetroot, carrot); the tag-based recipe is untouched |
| F2: no mobs spawned in Ancient Egypt, Dark Ages, Far Future and Jurassic Period: the lists were in the old `spawners` field, which the 26.x biome codec ignores | The lists moved to the `minecraft:gameplay/natural_mob_spawns` attribute, as in vanilla biomes. The ocelot in the Jurassic Period stays in `monster` — that is how it is defined in vanilla jungles too |
| F2: Squash has no attack (`attackGoals() {}`), although it grows in the wild | `SquashAttackGoal`: leaps onto a zombie within 3 blocks, squashes it in one hit and disappears (single-use, as in the original game). Squashes only zombies (a tamed one also other monsters), leaves nearby players, plants, villagers and pets alone; bosses take 40 damage instead of being killed. If the target died or left before the jump, it stays in place. Does not attack from a pot on someone's head or from a minecart |
| F2: a machine built from a blueprint is hostile to the player — the owner is never set (the line is commented out in `BlueprintItem`), the "owner" target goals come after a `return` and never run; the machine goes off to break the flag, despawns like a monster, and none of the mod's zombies attack zombies | A "player machine" mark (`dev.efureev.sproutsquad.machines.PlayerMachines`, saved in the world): the machine attacks only hostile mobs (not creepers or neutral mobs), plants and player machines are allies (they do not target each other, and stray projectiles hit neither them nor players), leaves the flag alone, does not despawn (on Peaceful difficulty as well); the Engineer does not treat other machines as its own. Works even when the owner is offline: the turret's projectiles hurt zombies even though the owner is not in the world |
| F2: a player turret with a 0.5 shot took more than a minute to kill an ordinary zombie | The player turret's shot uses the attack attribute (2); enemy turrets are unchanged |
| F2: the Lawn Mower is a stub with no behavior and breaks from any hit | `LawnMowerDrive`: waits for a monster to come close, drives forward (up to a wall or 40 blocks), mows down every monster in its path (bosses take 40 damage) and disappears; does not touch plants or players. Triggers only on a monster in front of it, is placed with its handle toward the player. Only a player can break it |
| F2: blueprints cannot be obtained in survival | The Engineer drops a complete blueprint of one of its machines with an 8.5% chance (loot table, if killed by a player or a plant); the Robo Army treasure holds blueprints of all four machines |
| F2: the Zombie Goat exists only as a spawn egg | Spawns at night on slopes and peaks, in groves and on windswept hills (`#sproutsquad:zombie/has_zombie_goat`; not in meadows or cherry groves), 40% of the time with an imp rider if one fits (except from spawn eggs and commands) |
| F2: a player on balloons gets kicked from the server for flying (TODO in `entity/Balloon.kt`) | While a balloon is actually pulling the player (more than 2 blocks above them) or their vehicle, the server's "floating in the air" counters are reset (`fixes/BalloonFlight.kt`, fields opened in `accesstransformer.cfg`); the flight anti-cheat still works in all other cases |
| The seed timer of plants ran several times per tick (the grow check, called by goals, rendering and `isGrowingSeeds`, also counted the timer down and counted rain) | The check is pure; the timer and rain run once per server tick, so seeds take the configured time |
| Flint and steel never lit Cherry Bomb, Potato Mine, Doom Shroom or Ice Shroom (`ExplosivePlant` required `cooldown < 0`) | Lights any idle, awake explosive plant; the lighter wears, someone else's tamed plant follows the ownership rules |
| Browncoats never spawned as babies (`PazZombie.finalizeSpawn` read `isBaby` before setting it) | Vanilla baby odds (5%) for zombies with baby models (Browncoat) |
| Spawn rules: Digger in mineshafts and Yeti on snow/ice skipped the Peaceful and light checks; Doom Shroom on netherrack skipped the spawn check and then dried out; Lightning Reed spawned on blocks it cannot live on | Operator precedence fixed; Doom Shroom survives on netherrack; Lightning Reed needs plantable ground |
| `plantCooldownEnabled` never applied to planting from seed packets, and the cooldown group sat only on the stack that was used | Planting puts the plant type on cooldown for every stack of it (`fixes/PlantingCooldown.kt`); a gardening glove move does not |
| The Plant Pot Helmet farmer trade was in no trade level | Farmer level 4 |
| `ZombieRobot` dropped its saved action timer; the Engineer searched 28 blocks for its machines but its range was 16; the Newspaper Zombie loot table used the Baron Von Bats random sequence | Fixed |
| Digger Zombie never mined blocks (`digger_breakable` tag, mining animation and `MinerNodeEvaluator` unused) | `DiggerMineGoal`: digs through `#sproutsquad:digger_breakable` blocks only when its target cannot be reached on foot; respects `mobGriefing` and claim protection |
| 16 dye-colour and rainbow Marigold textures were never used | Marigolds can be dyed with any dye; a Marigold named "Rainbow" is rainbow (`fixes/MarigoldColors.kt`) |
| Citron never switched to its ball form (`BALL_TRANSFORMATION` is never set) | A gardening glove push turns it into a ball for 15 s |
| Emissive layers fell back to plain variants: with no `<variant>_emissive` texture, the lookup drew e.g. the `_sleep`, `_baby` or `_damage_low` texture full-bright, so sleeping plants, damaged Wall Nuts and (once babies spawn) baby zombies glowed | The glow layer is `<drawn texture>_emissive.png` or nothing (`getEmissiveTextureLocation`) |
| A Mailbox loaded without an `EjectTimer` tag (placed by a structure template or by a command with NBT) crashed: `getInt("EjectTimer").get()` | Missing timer reads as 0 (`MailboxBlockEntity.loadAdditional`) |
| A mod zombie placed by a structure during world generation (the Mausoleum's Grave Digger, the Robo Junkyard's turrets) was set up with the main world's random generator on a generation thread: `finalizeSpawn` handed `level() as ServerLevel` to the vanilla setup instead of the generation region, and `ServerLevel.random` throws when two threads use it at once, which could crash the server | The vanilla setup gets the level it was given; the `ServerLevel` is used only to find a raid (`PazZombie.finalizeSpawn`) |
| On Hard a battle machine that was hit could call a reinforcement of its own type, like any zombie (the vanilla setup gives each a 0–10% chance, a leader 50–75% more), at a random spot 7–40 blocks away; machines have no spawn rules, so the turret or drone could appear in the air or underground | A machine never comes as a reinforcement: the machines' spawn rule refuses it (`machines/MachineReinforcements.kt`); in the fork this also keeps a player's machine from calling a hostile one |
| A mob wearing a plant in a Plant Pot Helmet ticked twice per game tick: the plant's tick ticked its wearer once more (`ServerLevelMixin`), so a mob wearing a plant moved and attacked twice as fast, and a player's server-side counters (such as the block breaking check) ran double | The wearer ticks once; the plant is moved onto the wearer's head after the wearer's tick (`ServerLevelMixin.tickSyncPlant`) |
| Taking off a Ducky Tube left the mob's water pathfinding penalty at 0, so it kept walking into water; putting one on kept the penalty (only a mob loaded from a save got 0) | Putting the tube on sets the penalty to 0, taking it off restores the mob's own value (`LivingEntityMixin`) |
| Frozen mobs kept the pose of the renderer, i.e. of their entity type: every frozen zombie of a type took the pose and the facing of the last one of that type drawn unfrozen | The pose is kept per mob (`LivingEntityRendererMixin`) |
| Every letter loaded the target mailbox's chunk on the server thread (`getBlockEntity` loads the chunk), so the mail queue for unloaded mailboxes was never used; queued mail would also have stayed in the sender (the Mailbox slot, the Collection Box), to be queued again on the next send or redstone pulse. The server took a Collection Box's new target from the client for any box in the level and for any position | Mail for an unloaded mailbox is queued without loading its chunk and leaves the sender (`SendMailRequestPayload.trySendStack`, `MailCollectionBoxEntity.trySendMail`); the target is changed only for the box the player has open, and only to a mailbox the server listed (`UpdateCollectionBoxPayload`) |
| Pumpkin armor from a Pumpkin seed packet ignored the sun cost check: a player without sun armored a plant, and the cost was taken from whatever sun they had | Without enough sun the packet does nothing (`pumpkinSeedInteraction`) |
| A client connected to a dedicated server saved that server's config into its own `config/sproutsquad/server.json` when a tooltip asked for a cost missing from the synced config; in singleplayer the client thread changed the config map the server thread was using | Missing defaults are written only by the server thread (`PazConfig.getSunCost`/`getAlloyCost`); the singleplayer client keeps its server's config object |
