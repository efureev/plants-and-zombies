---
name: verify
description: Check Sprout Squad works, by eye — build, GameTests, the dedicated server, and screenshots of the real game client taken by a script (the client autopilot). Use before calling any change done, especially new or changed characters, models, textures and animations.
---

# Verify Sprout Squad

Green tests are not proof. Finish with screenshots of the real client and look at them.

JDK: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`. Pillow for image work: a venv in the scratchpad
(`python3 -m venv <dir> && <dir>/bin/pip install pillow numpy`).

## 1. Build and GameTests

```bash
./gradlew build
./gradlew runGameTestServer        # exits by itself; "All N required tests passed :)"
```
On failure: `grep -A10 "required tests failed"` in the output. `sproutsquad:squad_plants` lists what a new plant
still lacks (textures, icon, names, recipe, advancement).

## 2. Dedicated server

`./gradlew runServer` must reach `Done (…)! For help` with no `ERROR`/`Exception` (common code must not touch
client classes). Gradle does not pass stdin, so `stop` cannot be typed: once `Done` is in the log, kill the
server process (only the one with `--nogui`, never a process under `.claude/worktrees`).

## 3. The client, driven by a script (autopilot)

There is no keyboard or screen access from the terminal (macOS refuses keystrokes and screen capture),
so the client plays a script: `dev.efureev.sproutsquad.debug.client.Autopilot`.

1. **World** (once): a singleplayer copy of the server world —
   `rsync -a --exclude session.lock run/world/ run/saves/verify/`.
2. **Early window off** while checking: `earlyWindowControl = false` in `run/config/fml.toml`
   (macOS hangs in `SDL_GL_SwapWindow` when the window is not visible). Put it back to `true` afterwards.
3. **Script** (a text file in the scratchpad), one step per line; a line starting with `#` is a comment (only
   whole lines: `#` inside a command is a tag):
   - `wait <ticks>` (20 a second), `cmd <command without slash>` (as the player, full permissions),
     `hud on|off`, `shot <name>` → `run/screenshots/<name>.png`, `quit`.
   - The step after a `cmd` waits until the server has run it, but the result reaches the screen a few ticks
     later: put `wait 5` (or more) between a `cmd` and a `shot`.
   - A flat stage high up, cleared, daytime:
     ```
     wait 40
     cmd gamemode creative
     cmd weather clear
     cmd time set 6000
     cmd fill -12 149 -12 12 149 12 minecraft:grass_block
     cmd fill -12 150 -12 12 165 12 minecraft:air
     cmd kill @e[type=!minecraft:player,distance=..40]
     ```
   - Portraits: summon frozen and facing the camera, `{NoAI:1b,Rotation:[180f,0f],Invulnerable:1b}`, the camera
     at lower z with `cmd tp @s <x> 150 <z> facing <x> 150.9 <z>`. Put a reference next to it (the Sunflower,
     Citron, Super Brainz).
   - Behaviour (sun, attacks, sleeping): summon without NoAI; plants turn their heads at random, so take
     several shots. Night: `cmd time set 18000`, wait ~200 ticks.
   - Always end with `quit`.
4. **Run** under a log watchdog (silence over 45 s → thread dump with `jcmd <pid> Thread.print`, then kill):
   ```bash
   ./gradlew runClient -Pautopilot=<script>
   ```
   The log shows every step (`Autopilot: …`) and every screen change (`Autopilot: screen …`). A screen that stays
   (`LoadingErrorScreen` with errors, `DisconnectedScreen` = no `verify` world) stops the script: read the log.
   Mod loading *warnings* (Kotlin for Forge's `logoFile`) are passed automatically.
5. **Look** at every screenshot (downscale to ~1200 px wide to read it), crop and enlarge details. Show the
   owner the ones that matter, opened with `open <path>`.

## What to look at for a character

- From the front and at 3/4, next to its reference: silhouette, size, face readable.
- By day and at night (asleep pose, `_sleep` texture, glowing/emissive parts).
- Its action: glow before it, the result (sun count, projectiles, effects) in the world.
- The docs picture: `python3 tools/docs/render_character.py <id>` / `tools/docs/refresh.py` matches the game.

## Checklist from past misses

Run the items whose trigger the change touches.

- **A glow or emissive layer** (`_glow`, `_emissive`, `SquadPlantLook.lookGlow`): the texture lookup appends the
  layer's suffix to the plant's (`baby`, `sleep` while dying, a magic name) and falls back to a *plain* texture,
  drawing the whole plant full-bright. Check a baby (`{Age:-240000}`) and a plant dying during the glow: either
  give the variant its own sheet (`<id>_baby_glow.png`) or return no glow for it.
- **Server code that places things at a model part** (sun from a head, a projectile from a mouth): the model may turn
  that part with the head (`state.yRot`, i.e. `yHeadRot`), not the body. Use the same angle on the server, and check
  in game with the plant looking sideways.
- **A spawn point outside the plant's hitbox** (a head reaching past the box): it can land inside the next block.
  A `Sun` pushes itself out only a quarter of a block and starts a little above the point, so test against a
  two-block wall with the plant off its block's centre (`DoubleSunflowerGameTests.sunNotInWall`).
- **Who hands out a new plant**: the Poppy Rattle gives every `#sproutsquad:plant` not in
  `poppy_rattle_excluded.json`. Either exclude the plant or list the Poppy Rattle where the docs say how to get it.
