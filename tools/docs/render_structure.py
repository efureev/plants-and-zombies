#!/usr/bin/env python3
"""Renders a structure of the mod (data/sproutsquad/structure/<path>.nbt) from its blocks, for
the docs: every block's model from its blockstate (variants or multipart, with the x/y rotation),
faces hidden by a neighbouring full opaque block left out, drawn like render_item.py draws
block models. Block entities without a JSON model (chests) and jigsaw/structure blocks are not
drawn. A structure that the world generation sinks into the ground (a negative start_height
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
    """How many bottom layers the world generation puts underground: -start_height of the
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
            return -height
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
        for model_id, rx, ry in models_of(block, props):
            model = R.load_model(model_id)
            matrix = turn(rx, ry)
            for pts, image, uv, normal, flat, cull in block_faces(model):
                if cull:
                    direction = np.rint(matrix @ np.array(DIRECTIONS[cull], dtype=float)).astype(int)
                    neighbour = grid.get(tuple(int(p + d) for p, d in zip(pos, direction)))
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
