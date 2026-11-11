#!/usr/bin/env python3
"""Renders a structure of the mod (data/sproutsquad/structure/<path>.nbt) from its blocks, for
the docs: every block's model from its blockstate (variants or multipart, with the x/y rotation),
faces hidden by a neighbouring full opaque block left out, drawn like render_item.py draws
block models. Jigsaw and structure blocks are not drawn; blocks without a JSON model are
drawn as simple boxes (EXTRA_MODELS: water, chests, decorated pots, bells). A structure that the world generation sinks into the ground (a negative start_height
projected to the surface, like the Mausoleum's crypt) is drawn from the ground up; --whole draws
all of it.

Usage (needs Pillow and numpy), from the repository root:
    python3 tools/docs/render_structure.py graveyard/mausoleum [--out docs/images/structures/graveyard_mausoleum.png]
                                           [--yaw 35] [--pitch 30] [--size 512]
"""
import argparse
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render_item as R  # noqa: E402
from pictures import read_nbt  # noqa: E402,F401

DATA = "src/main/resources/data/sproutsquad/structure"
SKIP = {"air", "cave_air", "void_air", "jigsaw", "structure_block", "structure_void", "barrier", "light"}
DIRECTIONS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
TINT = {"value": 0x7CBD6B}  # grass and leaves: a plains-like green


def box(f, t, texture, tint=-1):
    faces = {n: {"texture": texture, "cullface": n, **({"tintindex": tint} if tint >= 0 else {})}
             for n in ("north", "south", "west", "east", "up", "down")}
    return {"from": f, "to": t, "faces": faces}


# Blocks the game draws in code (fluids, block entity renderers): simple boxes in their place.
# A chest's lock is on its north face, so facing turns it like a blockstate y rotation.
EXTRA_MODELS = {
    "minecraft:water": {"textures": {}, "tint": 0x3F76E4, "same_culls": True,
                        "elements": [box([0, 0, 0], [16, 14, 16], "minecraft:block/water_still", 0)]},
    "minecraft:chest": {"textures": {}, "elements": [
        box([1, 0, 1], [15, 10, 15], "minecraft:block/oak_planks"),
        box([1, 10, 1], [15, 14, 15], "minecraft:block/stripped_oak_log"),
        box([7, 7, 0], [9, 11, 1], "minecraft:block/iron_block")]},
    "minecraft:decorated_pot": {"textures": {}, "elements": [
        box([2, 0, 2], [14, 12, 14], "minecraft:block/terracotta"),
        box([5, 12, 5], [11, 16, 11], "minecraft:block/terracotta")]},
    "minecraft:bell": {"textures": {}, "elements": [
        box([1, 0, 7], [3, 16, 9], "minecraft:block/stone"), box([13, 0, 7], [15, 16, 9], "minecraft:block/stone"),
        box([1, 13, 7], [15, 15, 9], "minecraft:block/dark_oak_planks"),
        box([5, 5, 5], [11, 12, 11], "minecraft:block/gold_block"), box([4, 4, 4], [12, 5, 12], "minecraft:block/gold_block")]},
}
EXTRA_MODELS["minecraft:trapped_chest"] = EXTRA_MODELS["minecraft:chest"]
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def extra_parts(block, props):
    """(model, x, y) for a block in EXTRA_MODELS, or None."""
    model = EXTRA_MODELS.get(block)
    if model is None:
        return None
    return [(model, 0, FACING_Y.get(props.get("facing", "north"), 0) if block != "minecraft:bell" else 0)]


def extra_faces(model):
    tints = [{"value": model["tint"]}] * 3 if "tint" in model else []
    faces = R.model_faces(model, tints=tints)
    culls = [face.get("cullface") for el in model["elements"] for face in el["faces"].values()]
    return [(*f, c) for f, c in zip(faces, culls)]


def state_of(entry):
    """(block id, properties) of a palette entry; both the old (Name/Properties) and the new keys."""
    block = entry.get("Name") or entry.get("id")
    props = entry.get("Properties") or entry.get("properties") or {}
    return (block if ":" in block else "minecraft:" + block), props


def matches(when, props):
    if "OR" in when:
        return any(matches(w, props) for w in when["OR"])
    if "AND" in when:
        return all(matches(w, props) for w in when["AND"])
    return all(str(props.get(k, "")) in str(v).split("|") for k, v in when.items())


def models_of(block, props):
    """[(model id, x, y)] the blockstate picks for these properties."""
    ns, path = R.split_id(block)
    try:
        state = json.loads(R.read_asset(ns, f"blockstates/{path}.json"))
    except SystemExit:
        return []
    picked = []
    if "variants" in state:
        for key, variant in state["variants"].items():
            pairs = [kv.split("=") for kv in key.split(",") if kv]
            if all(str(props.get(k, "")) == v for k, v in pairs):
                picked.append(variant[0] if isinstance(variant, list) else variant)
                break
    for part in state.get("multipart", []):
        if "when" not in part or matches(part["when"], props):
            apply = part["apply"]
            picked.append(apply[0] if isinstance(apply, list) else apply)
    return [(v["model"], v.get("x", 0), v.get("y", 0)) for v in picked]


def turn(x, y):
    """The blockstate rotation (the game applies x, then y, both clockwise)."""
    return R.rot_y(math.radians(-y)) @ R.rot_x(math.radians(-x))


_full = {}


def occludes(block, props):
    """A full cube whose six faces are opaque hides the faces that touch it."""
    key = (block, tuple(sorted(props.items())))
    if key not in _full:
        result = False
        models = models_of(block, props)
        if len(models) == 1:
            model = R.load_model(models[0][0])
            elements = model.get("elements", [])
            if len(elements) == 1 and elements[0]["from"] == [0, 0, 0] and elements[0]["to"] == [16, 16, 16] \
                    and len(elements[0].get("faces", {})) == 6:
                textures = {R.resolve(model["textures"], face["texture"]) for face in elements[0]["faces"].values()}
                result = all(R.load_texture(t).getchannel("A").getextrema()[0] == 255 for t in textures if t)
        _full[key] = result
    return _full[key]


def ground_level(structure):
    """How many bottom layers the world generation puts underground, from the start_height of the
    jigsaw structure whose start pool places this template, when it is projected to the surface."""
    root = "src/main/resources/data/sproutsquad/worldgen"
    location = "sproutsquad:" + structure
    pools = set()
    for dirpath, _, files in os.walk(os.path.join(root, "template_pool")):
        for f in files:
            pool = json.load(open(os.path.join(dirpath, f)))
            if any(e.get("element", {}).get("location") == location for e in pool.get("elements", [])):
                rel = os.path.relpath(os.path.join(dirpath, f), os.path.join(root, "template_pool"))[:-5]
                pools.add("sproutsquad:" + rel)
    for f in os.listdir(os.path.join(root, "structure")):
        config = json.load(open(os.path.join(root, "structure", f)))
        height = config.get("start_height", {}).get("absolute", 0)
        if config.get("start_pool") in pools and config.get("project_start_to_heightmap") and height < 0:
            return -height - 1  # the layer at start_height -1 replaces the top ground block: it is the visible floor
    return 0


def structure_faces(path, from_y=0):
    data = read_nbt(path)
    palette = [state_of(entry) for entry in (data.get("palette") or data["palettes"][0])]
    grid = {}
    for block in data["blocks"]:
        grid[tuple(block["pos"])] = palette[block["state"]]
    faces = []
    for pos, (block, props) in grid.items():
        if block.split(":")[1] in SKIP or pos[1] < from_y:
            continue
        for model, rx, ry in extra_parts(block, props) or models_of(block, props):
            own = extra_faces(model) if isinstance(model, dict) else block_faces(R.load_model(model))
            matrix = turn(rx, ry)
            for pts, image, uv, normal, flat, cull in own:
                if cull:
                    direction = np.rint(matrix @ np.array(DIRECTIONS[cull], dtype=float)).astype(int)
                    neighbour = grid.get(tuple(int(p + d) for p, d in zip(pos, direction)))
                    if neighbour and neighbour[0] == block and (isinstance(model, dict) and model.get("same_culls")
                                                                or "glass" in block or block.endswith("ice")):
                        continue  # water next to water, glass next to the same glass
                    if neighbour and neighbour[0].split(":")[1] not in SKIP and occludes(*neighbour):
                        continue
                base = np.array(pos, dtype=float) * 16
                faces.append(([matrix @ (p - 8) + 8 + base for p in pts], image, uv, matrix @ normal, flat))
    return faces


def block_faces(model):
    """model_faces() of render_item.py, plus each face's cullface."""
    out = []
    plain = R.model_faces(model, tints=[TINT, TINT, TINT])
    culls = [face.get("cullface") for element in model.get("elements", []) for face in element.get("faces", {}).values()
             if R.resolve(model.get("textures", {}), face.get("texture", "#0"))]
    for face, cull in zip(plain, culls):
        out.append((*face, cull))
    return out


def render(structure, out, yaw=35.0, pitch=30.0, size=512, whole=False):
    path = os.path.join(DATA, structure + ".nbt")
    if not os.path.exists(path):
        sys.exit(f"no structure {path}")
    faces = structure_faces(path, 0 if whole else ground_level(structure))
    if not faces:
        sys.exit(f"nothing to draw in {path}")
    R.draw(faces, out, size, R.yaw_pitch(yaw, pitch), shadow=False)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("structure", help="path under data/sproutsquad/structure, without .nbt")
    ap.add_argument("--out")
    ap.add_argument("--yaw", type=float, default=35.0)
    ap.add_argument("--pitch", type=float, default=30.0)
    ap.add_argument("--size", type=int, default=512)
    ap.add_argument("--whole", action="store_true", help="draw the underground part too")
    args = ap.parse_args()
    out = args.out or f"docs/images/structures/{args.structure.replace('/', '_')}.png"
    render(args.structure, out, args.yaw, args.pitch, args.size, args.whole)


if __name__ == "__main__":
    main()
