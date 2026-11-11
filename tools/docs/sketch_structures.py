#!/usr/bin/env python3
"""Concept sketches of the planned structures (docs/<locale>/STRUCTURES.md): every structure is
put together here from real Minecraft and mod blocks and drawn like the structures in the docs
(tools/docs/render_structure.py). The mod's characters stand inside as their pictures from
docs/images; underground structures are shown cut open: no ceiling, the near walls cut low.

Usage (needs Pillow and numpy), from the repository root:
    python3 tools/docs/sketch_structures.py                 # every sketch and both overview sheets
    python3 tools/docs/sketch_structures.py garden_shack    # one
A sketch is not the structure: sizes and rooms are approximate, it is something to talk over.

A scene can also become the mod's real structure template (EXPORTS):
    python3 tools/docs/sketch_structures.py --export garden_shack
writes data/sproutsquad/structure/<id>/<variant>.nbt: the blocks inside the export bounds (empty
places as air, so the structure clears grass and flowers), block entities with their loot tables
or items, and the characters marked as entities. Once a template is reworked in the game with a
structure block, the .nbt is the source and the export is not run again.
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, "tools/docs")
import render_item as R  # noqa: E402
import render_structure as RS  # noqa: E402
import pictures as P  # noqa: E402

OUT = "docs/images/sketches/structures"
CHARACTERS = "docs/images/characters"

# --- animated textures (water, lava, lamps): the first frame only
_load_texture = R.load_texture


def first_frame(texture_id):
    image = _load_texture(texture_id)
    return image.crop((0, 0, image.width, image.width)) if image.height > image.width else image


R.load_texture = first_frame

# --- blocks without a plain model: the boxes of render_structure.py, plus a coffin for the sketches
box = RS.box
CUSTOM = {
    "sketch:water": RS.EXTRA_MODELS["minecraft:water"],
    "sketch:chest": RS.EXTRA_MODELS["minecraft:chest"],
    "sketch:pot": RS.EXTRA_MODELS["minecraft:decorated_pot"],
    "sketch:bell": RS.EXTRA_MODELS["minecraft:bell"],
    "sketch:coffin": {"textures": {}, "elements": [box([2, 0, 0], [14, 6, 16], "minecraft:block/dark_oak_planks"),
                                                   box([5, 6, 3], [11, 7, 13], "minecraft:block/gold_block")]},
}
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def ns(block):
    return block if ":" in block else "minecraft:" + block


def default_props(block):
    """The properties of the blockstate's first variant, so a block needs only the ones that differ."""
    if block.startswith("sketch:"):
        return {}
    space, path = R.split_id(block)
    try:
        state = json.loads(R.read_asset(space, f"blockstates/{path}.json"))
    except SystemExit:
        return {}
    if "variants" in state:
        key = next(iter(state["variants"]))
        return dict(kv.split("=") for kv in key.split(",") if kv)
    return {}


class Scene:
    def __init__(self, seed=1):
        self.grid = {}
        self.sprites = []
        self.entities = []
        self.rng = random.Random(seed)

    def put(self, x, y, z, block, **props):
        block = ns(block)
        merged = default_props(block)
        merged.update({k: (str(v).lower() if isinstance(v, bool) else str(v)) for k, v in props.items()})
        self.grid[(x, y, z)] = (block, merged)

    def fill(self, x0, y0, z0, x1, y1, z1, block, chance=1.0, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if chance >= 1 or self.rng.random() < chance:
                        self.put(x, y, z, block, **props)

    def clear(self, x0, y0, z0, x1, y1, z1):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.grid.pop((x, y, z), None)

    def age(self, mapping, chance):
        """Ageing: some blocks are swapped (bricks to cracked or mossy ones)."""
        for pos, (block, props) in list(self.grid.items()):
            if block in mapping and self.rng.random() < chance:
                self.grid[pos] = (ns(self.rng.choice(mapping[block])), props)

    def sprite(self, name, x, y, z, height, entity=None):
        """A character drawn on the sketch; with an entity id it is also placed in an exported template."""
        self.sprites.append((name, (x, y, z), height))
        if entity:
            self.entities.append((entity, (x, y, z)))

    def connect(self):
        """Fences, bars, walls and panes connect to their neighbours as in the game."""
        dirs = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}
        def family(b):
            n = b.split(":")[1]
            if n.endswith("_fence") or n.endswith("_fence_gate"):
                return "fence"
            if n.endswith("bars") or n.endswith("glass_pane"):
                return "pane"
            if n.endswith("_wall"):
                return "wall"
            return None
        for pos, (block, props) in list(self.grid.items()):
            kind = family(block)
            if kind is None or block.endswith("_fence_gate"):
                continue
            links = {}
            for d, (dx, dy, dz) in dirs.items():
                other = self.grid.get((pos[0] + dx, pos[1], pos[2] + dz))
                linked = bool(other) and (family(other[0]) == kind or (kind == "fence" and other[0].endswith("_fence_gate"))
                                          or RS.occludes(other[0], other[1]))
                links[d] = linked
            if kind == "wall":
                for d, linked in links.items():
                    props[d] = "low" if linked else "none"
                straight = (links["north"] and links["south"] and not links["east"] and not links["west"]) or \
                           (links["east"] and links["west"] and not links["north"] and not links["south"])
                props["up"] = "false" if straight else "true"
            else:
                for d, linked in links.items():
                    props[d] = "true" if linked else "false"
            props["waterlogged"] = "false"


# --- faces of a scene: structure_faces() of render_structure.py, from a grid in memory
def custom_faces(block, props):
    model = CUSTOM[block]
    tints = [{"value": model["tint"]}] * 3 if "tint" in model else []
    faces = R.model_faces(model, tints=tints)
    culls = [face.get("cullface") for el in model["elements"] for face in el["faces"].values()]
    return [(*f, c) for f, c in zip(faces, culls)]


def scene_faces(scene):
    faces = []
    for pos, (block, props) in scene.grid.items():
        if block.startswith("sketch:"):
            parts = [(None, 0, FACING_Y.get(props.get("facing", "north"), 0))]
        elif block.split(":")[1] in RS.SKIP:
            continue
        else:
            parts = RS.models_of(block, props)
        for model_id, rx, ry in parts:
            matrix = RS.turn(rx, ry)
            own = custom_faces(block, props) if model_id is None else RS.block_faces(R.load_model(model_id))
            for pts, image, uv, normal, flat, cull in own:
                if cull:
                    d = np.rint(matrix @ np.array(RS.DIRECTIONS[cull], dtype=float)).astype(int)
                    other = scene.grid.get(tuple(int(p + q) for p, q in zip(pos, d)))
                    if other and (other[0] == block and (block.startswith("sketch:") or "glass" in block or block.endswith("ice")) or
                                  (not other[0].startswith("sketch:") and other[0].split(":")[1] not in RS.SKIP
                                   and RS.occludes(*other))):
                        continue
                base = np.array(pos, dtype=float) * 16
                faces.append(([matrix @ (p - 8) + 8 + base for p in pts], image, uv, matrix @ normal, flat))
    return faces


BACKGROUND = (238, 240, 234)
SPRITE_SCALE = 1.4  # characters a bit larger than in the game, to be seen
_sprite_cache = {}


def sprite_image(name):
    if name not in _sprite_cache:
        image = Image.open(os.path.join(CHARACTERS, name + ".png")).convert("RGBA")
        _sprite_cache[name] = image.crop(image.getbbox())
    return _sprite_cache[name]


def draw(scene, out, size=900, yaw=35.0, pitch=30.0):
    """render_item.draw() with characters, sorted by depth together with the faces."""
    view = R.yaw_pitch(yaw, pitch)
    light = np.array([-0.35, 1.0, 0.6]); light /= np.linalg.norm(light)
    items = []
    for pts, image, uv, normal, flat in scene_faces(scene):
        cam = [view @ (p - 8) for p in pts]
        n = view @ normal
        if n[2] < 0:
            if not flat:
                continue
            normal = -normal
        o, s_end, t_end = cam
        a, b = s_end - o, t_end - o
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(normal, light)))
        items.append((float((o + a / 2 + b / 2)[2]), "face", (o, a, b, image, uv, shade)))
    for name, (x, y, z), height in scene.sprites:
        height *= SPRITE_SCALE
        foot = view @ (np.array([x + 0.5, y, z + 0.5]) * 16 - 8)
        head = view @ (np.array([x + 0.5, y + height, z + 0.5]) * 16 - 8)
        middle = view @ (np.array([x + 0.5, y + 0.5, z + 0.5]) * 16 - 8)
        items.append((float(middle[2]), "sprite", (name, foot, head)))
    items.sort(key=lambda f: f[0])
    pts2 = []
    for _, kind, data in items:
        if kind == "face":
            o, a, b = data[:3]
            pts2 += [o, o + a, o + b, o + a + b]
        else:
            pts2 += [data[1], data[2]]
    xs, ys = [p[0] for p in pts2], [p[1] for p in pts2]
    min_x, max_x, min_y, max_y = min(xs), max(xs), min(ys), max(ys)
    ss = 3
    px = size * ss
    scale = px * 0.92 / max(max_x - min_x, max_y - min_y)

    def screen(p):
        return np.array([(p[0] - (min_x + max_x) / 2) * scale + px / 2, -(p[1] - (min_y + max_y) / 2) * scale + px / 2])

    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    for _, kind, data in items:
        if kind == "sprite":
            name, foot, head = data
            image = sprite_image(name)
            h = max(4, int(abs(screen(head)[1] - screen(foot)[1])))
            w = max(4, int(image.width * h / image.height))
            sprite = image.resize((w, h), Image.LANCZOS)
            f = screen(foot)
            canvas.alpha_composite(sprite, dest=(int(f[0] - w / 2), int(f[1] - h * 0.93)))
            continue
        o, a, b, image, (u1, v1, u2, v2), shade = data
        so = screen(o)
        sa, sb = screen(o + a) - so, screen(o + b) - so
        det = sa[0] * sb[1] - sa[1] * sb[0]
        if abs(det) < 1e-6:
            continue
        w, h = image.size
        tu1, tv1, tu2, tv2 = (u1 / 16 * w, v1 / 16 * h, u2 / 16 * w, v2 / 16 * h)
        eu, ev = 0.02 * (1 if tu2 >= tu1 else -1), 0.02 * (1 if tv2 >= tv1 else -1)
        tu1, tu2, tv1, tv2 = tu1 + eu, tu2 - eu, tv1 + ev, tv2 - ev
        inv = np.array([[sb[1], -sb[0]], [-sa[1], sa[0]]]) / det
        a11, a12 = inv[0] * (tu2 - tu1)
        a21, a22 = inv[1] * (tv2 - tv1)
        quad = [so, so + sa, so + sa + sb, so + sb]
        x0 = max(0, int(math.floor(min(q[0] for q in quad))) - 1); y0 = max(0, int(math.floor(min(q[1] for q in quad))) - 1)
        x1 = min(px, int(math.ceil(max(q[0] for q in quad))) + 1); y1 = min(px, int(math.ceil(max(q[1] for q in quad))) + 1)
        if x1 <= x0 or y1 <= y0:
            continue
        c1 = tu1 - (a11 * (so[0] - x0) + a12 * (so[1] - y0))
        c2 = tv1 - (a21 * (so[0] - x0) + a22 * (so[1] - y0))
        bx = (x1 - x0, y1 - y0)
        warped = image.transform(bx, Image.AFFINE, (a11, a12, c1, a21, a22, c2), resample=Image.NEAREST)
        mask = Image.new("L", bx, 0)
        ImageDraw.Draw(mask).polygon([(q[0] - x0, q[1] - y0) for q in quad], fill=255)
        r, g, bl, al = warped.split()
        r, g, bl = (ch.point(lambda v: int(v * shade)) for ch in (r, g, bl))
        canvas.alpha_composite(Image.merge("RGBA", (r, g, bl, Image.composite(al, Image.new("L", bx, 0), mask))), dest=(x0, y0))
    result = canvas.resize((size, size), Image.LANCZOS)
    background = Image.new("RGBA", result.size, BACKGROUND + (255,))
    background.alpha_composite(result)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    save(background.convert("RGB"), out)
    print(out, len(items), "faces and sprites")


def save(image, out):
    """256 colours are plenty for a sketch and keep the file small."""
    os.makedirs(os.path.dirname(out), exist_ok=True)
    image.quantize(colors=256, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE).save(out, optimize=True)


# ================================================================ structures

def stairs_roof(s, x0, x1, z0, z1, y, block, ridge="slab"):
    """A gable roof along x: stairs from the z0/z1 edges up to the ridge."""
    lo, hi, level = z0, z1, y
    while lo < hi:
        s.fill(x0, level, lo, x1, level, lo, block + "_stairs", facing="south", half="bottom", shape="straight")
        s.fill(x0, level, hi, x1, level, hi, block + "_stairs", facing="north", half="bottom", shape="straight")
        lo, hi, level = lo + 1, hi - 1, level + 1
    if lo == hi:
        s.fill(x0, level, lo, x1, level, lo, block + "_slab", type="bottom")


def garden_shack():
    s = Scene(3)
    s.fill(0, -1, 0, 20, -1, 20, "dirt")
    s.fill(0, 0, 0, 20, 0, 20, "grass_block", snowy="false")
    # fence with a gate
    for i in range(1, 18):
        for x, z in ((i, 1), (i, 17), (1, i), (17, i)):
            s.put(x, 1, z, "oak_fence")
    s.put(11, 1, 17, "oak_fence_gate", facing="south", open="true", in_wall="false", powered="false")
    # beds with water between them
    for x in range(8, 16):
        for z in range(8, 16):
            if x in (10, 13):
                s.put(x, 0, z, "sketch:water")
            else:
                s.put(x, 0, z, "farmland", moisture=7)
    s.fill(8, 1, 8, 8, 1, 15, "wheat", age=7)
    s.fill(9, 1, 8, 9, 1, 11, "carrots", age=7)
    s.fill(14, 1, 12, 14, 1, 15, "beetroots", age=3)
    # path from the gate to the shed
    for z in range(7, 18):
        s.put(11, 0, z, "dirt_path") if z > 15 or z < 8 else None
    s.fill(4, 0, 7, 11, 0, 7, "dirt_path")
    # 5×5 shed
    s.fill(2, 0, 2, 6, 0, 6, "oak_planks")
    s.fill(2, 1, 2, 6, 3, 6, "oak_planks")
    s.clear(3, 1, 3, 5, 3, 5)
    for x, z in ((2, 2), (6, 2), (2, 6), (6, 6)):
        s.fill(x, 1, z, x, 3, z, "oak_log", axis="y")
    s.clear(4, 1, 6, 4, 2, 6)
    s.put(4, 1, 6, "oak_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    s.put(4, 2, 6, "oak_door", facing="north", half="upper", hinge="left", open="false", powered="false")
    s.put(6, 2, 4, "glass")
    s.put(2, 2, 4, "glass")
    for y, z0, z1 in ((4, 2, 6), (5, 3, 5), (6, 4, 4)):
        s.fill(2, y, z0, 2, y, z1, "oak_planks"); s.fill(6, y, z0, 6, y, z1, "oak_planks")
    stairs_roof(s, 1, 7, 1, 7, 4, "spruce")
    s.put(3, 1, 3, "sketch:chest", facing="south")
    s.put(5, 1, 3, "crafting_table")
    # yard: composter, barrels, scarecrow, mailbox
    s.put(7, 1, 3, "composter", level=4)
    s.put(7, 1, 5, "barrel", facing="up", open="false"); s.put(7, 2, 5, "barrel", facing="up", open="false")
    s.put(4, 1, 12, "oak_fence"); s.put(4, 2, 12, "hay_block", axis="y"); s.put(4, 3, 12, "carved_pumpkin", facing="south")
    s.put(3, 2, 12, "oak_fence"); s.put(5, 2, 12, "oak_fence")
    s.put(12, 1, 18, "sproutsquad:mailbox", mailbox_state="has_mail", face="floor", facing="south")
    for _ in range(40):
        x, z = s.rng.randint(0, 20), s.rng.randint(0, 20)
        if (x, 1, z) not in s.grid and s.grid.get((x, 0, z), ("",))[0] == "minecraft:grass_block" and not (2 <= x <= 7 and 2 <= z <= 7):
            s.put(x, 1, z, s.rng.choice(["short_grass", "short_grass", "poppy", "dandelion", "cornflower"]))
    s.connect()
    s.sprite("sunflower", 9, 1, 13, 1.25, entity="sproutsquad:sunflower")
    s.sprite("peashooter", 12, 1, 9, 1.1, entity="sproutsquad:peashooter")
    s.sprite("wallnut", 15, 1, 9, 1.1, entity="sproutsquad:wallnut")
    s.sprite("potling", 12, 1, 14, 0.9, entity="sproutsquad:potling")
    s.sprite("browncoat", 19, 1, 15, 2.0)
    return s


def room(walls, x0, z0, x1, z1):
    """Walls of a cut-open room: north and west full height, south and east low."""
    for x in range(x0, x1 + 1):
        walls[(x, z0)] = walls.get((x, z0), False) or True
        walls.setdefault((x, z1), False)
    for z in range(z0, z1 + 1):
        walls[(x0, z)] = walls.get((x0, z), False) or True
        walls.setdefault((x1, z), False)


def crypt():
    s = Scene(5)
    walls = {}
    rooms = [(2, 7, 4, 13), (1, 13, 11, 21), (11, 16, 18, 18), (17, 12, 26, 22), (17, 4, 26, 12), (7, 4, 17, 11)]
    for r in rooms:
        room(walls, *r)
    for x0, z0, x1, z1 in rooms:
        s.fill(x0, 0, z0, x1, 0, z1, "polished_deepslate")
    for (x, z), tall in walls.items():
        s.fill(x, 1, z, x, 4 if tall else 1, z, "deepslate_bricks")
    # doorways between the rooms
    for x, z in ((3, 13), (11, 17), (17, 17), (4, 9), (21, 12), (22, 12), (23, 12)):
        s.clear(x, 1, z, x, 3, z)
    s.fill(21, 1, 12, 23, 3, 12, "iron_bars")
    s.age({"minecraft:deepslate_bricks": ["cracked_deepslate_bricks", "deepslate_tiles"]}, 0.2)
    # entrance: a column of earth with the pavilion on top, a doorway to the corridor
    s.fill(0, 0, 0, 6, 7, 6, "dirt")
    s.fill(0, 8, 0, 6, 8, 6, "podzol", snowy="false")
    for x, z in ((1, 1), (5, 1), (1, 5), (5, 5)):
        s.fill(x, 9, z, x, 11, z, "chiseled_tuff_bricks")
    s.fill(1, 12, 1, 5, 12, 5, "polished_tuff_slab", type="bottom")
    s.fill(2, 8, 2, 4, 8, 4, "tuff_brick_stairs", facing="south", half="bottom", shape="straight")
    s.clear(3, 1, 6, 3, 3, 6)
    s.put(3, 9, 3, "candle", candles=3, lit="true")
    # gravestone hall
    for x in (4, 6, 8):
        for z in (16, 19):
            s.put(x, 1, z, "sproutsquad:gravestone", facing="south")
    s.put(2, 1, 14, "cobweb"); s.put(10, 3, 14, "cobweb"); s.put(2, 1, 20, "bone_block", axis="y")
    for x, z in ((3, 8), (3, 11), (2, 15), (10, 20), (13, 17)):
        s.put(x, 1, z, "candle", candles=s.rng.randint(1, 4), lit="true")
    # chapel: stained glass, a coffin on a plinth, candles
    for z in range(5, 11, 2):
        s.put(7, 2, z, "purple_stained_glass"); s.put(7, 3, z, "purple_stained_glass")
    for x in range(9, 16, 3):
        s.put(x, 2, 4, "purple_stained_glass"); s.put(x, 3, 4, "purple_stained_glass")
    s.fill(11, 1, 6, 13, 1, 8, "polished_tuff")
    s.put(12, 2, 7, "sketch:coffin", facing="south")
    for x, z in ((9, 6), (15, 6), (9, 9), (15, 9), (11, 10), (13, 10)):
        s.put(x, 1, z, "candle", candles=4, lit="true")
    # trial hall and vault room
    s.put(22, 1, 17, "trial_spawner", trial_spawner_state="active", ominous="false")
    for x, z in ((19, 14), (25, 14), (19, 20), (25, 20)):
        s.fill(x, 1, z, x, 4, z, "chiseled_tuff")
    s.put(22, 1, 7, "vault", facing="south", vault_state="active", ominous="false")
    s.put(19, 1, 5, "sketch:chest", facing="south"); s.put(25, 1, 5, "sketch:chest", facing="south")
    s.put(20, 1, 8, "sketch:pot"); s.put(24, 1, 8, "sketch:pot")
    s.connect()
    s.sprite("browncoat", 5, 1, 18, 2.0)
    s.sprite("imp", 8, 1, 15, 1.2)
    s.sprite("zombat", 9, 2, 19, 0.8)
    s.sprite("browncoat_vampire", 21, 1, 19, 2.0)
    s.sprite("zombat", 24, 2, 16, 0.8)
    return s


def junkyard():
    s = Scene(7)
    s.fill(0, -1, 0, 25, -1, 25, "dirt")
    s.fill(0, 0, 0, 25, 0, 25, "coarse_dirt")
    s.fill(0, 0, 0, 25, 0, 25, "gravel", chance=0.2)
    s.fill(0, 0, 0, 25, 0, 25, "red_sand", chance=0.06)
    for i in range(0, 26):
        for x, z in ((i, 0), (i, 25), (0, i), (25, i)):
            if s.rng.random() < 0.85 and not (z == 25 and 10 <= x <= 14):
                s.put(x, 1, z, "iron_bars" if (x + z) % 3 else "sproutsquad:brainz_alloy_fence")
                if s.rng.random() < 0.5:
                    s.put(x, 2, z, "iron_bars")
    for cx, cz, r in ((6, 6, 3), (19, 6, 2), (6, 17, 2), (12, 9, 2)):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                height = r + 1 - max(abs(x - cx), abs(z - cz))
                for y in range(1, height + 1):
                    s.put(x, y, z, s.rng.choice(["sproutsquad:smooth_brainz_alloy_block", "sproutsquad:treaded_brainz_alloy_block",
                                                 "iron_block", "iron_block", "weathered_copper", "sproutsquad:brainz_alloy_block",
                                                 "sproutsquad:brainz_alloy_block"]))
    s.put(7, 4, 6, "anvil", facing="east"); s.put(19, 3, 6, "chain", axis="y")
    # 7×7 workshop without a roof, door to the south
    s.fill(15, 0, 14, 21, 0, 20, "sproutsquad:smooth_brainz_alloy_block")
    for x in range(15, 22):
        for z in range(14, 21):
            if x in (15, 21) or z in (14, 20):
                tall = x == 15 or z == 14
                s.fill(x, 1, z, x, 4 if tall else 2, z, "sproutsquad:reinforced_brainz_alloy_block" if (x in (15, 21) and z in (14, 20)) else "sproutsquad:brainz_alloy_block")
    s.clear(18, 1, 20, 18, 2, 20)
    s.put(16, 1, 15, "crafting_table"); s.put(17, 1, 15, "smithing_table"); s.put(18, 1, 15, "anvil", facing="east")
    s.put(20, 1, 15, "sketch:chest", facing="south"); s.put(20, 1, 17, "barrel", facing="up", open="false")
    # broken lawn mower
    s.fill(9, 1, 20, 10, 1, 21, "red_concrete"); s.put(8, 1, 20, "coal_block"); s.put(8, 1, 21, "coal_block")
    s.put(11, 1, 20, "iron_bars"); s.put(11, 2, 20, "iron_bars")
    s.connect()
    s.sprite("zombie_turret", 12, 3, 9, 1.4)
    s.sprite("electro_turret", 17, 1, 12, 1.3)
    s.sprite("engineer_zombie", 13, 1, 17, 2.0)
    s.sprite("robo_zombie", 4, 1, 12, 2.2)
    return s


def dig_site():
    s = Scene(11)
    s.fill(0, -4, 0, 23, -1, 21, "dirt")
    s.fill(0, 0, 0, 23, 0, 21, "grass_block", snowy="false")
    s.fill(0, 0, 0, 23, 0, 21, "coarse_dirt", chance=0.15)
    # stepped dig
    s.clear(4, -1, 5, 17, 0, 16)
    s.fill(4, -1, 5, 17, -1, 16, "coarse_dirt")
    s.clear(6, -2, 7, 15, -1, 14)
    s.fill(6, -2, 7, 15, -2, 14, "gravel")
    s.clear(8, -3, 9, 13, -2, 12)
    s.fill(8, -3, 9, 13, -3, 12, "gravel")
    s.fill(8, -3, 9, 13, -3, 12, "suspicious_gravel", chance=0.3, dusted=0)
    s.fill(6, -2, 7, 15, -2, 14, "suspicious_sand", chance=0.08, dusted=0)
    # the Rex skeleton
    s.fill(7, -2, 10, 14, -2, 10, "bone_block", axis="x")
    for x in (9, 10, 11, 12):
        s.put(x, -2, 9, "bone_block", axis="y"); s.put(x, -2, 11, "bone_block", axis="y")
        s.put(x, -1, 9, "bone_block", axis="z") if x % 2 else None
    s.fill(14, -2, 9, 15, -1, 11, "bone_block", axis="y")
    s.put(15, 0, 10, "bone_block", axis="x")
    s.put(8, -2, 8, "bone_block", axis="z"); s.put(8, -2, 12, "bone_block", axis="z")
    # scaffolding, ladders, lanterns
    for x, z in ((4, 5), (17, 16)):
        s.fill(x, 0, z, x, 3, z, "scaffolding", bottom="false", distance=0, waterlogged="false")
    s.fill(6, -1, 15, 6, 0, 15, "ladder", facing="north", waterlogged="false")
    # tents
    for x0, z0 in ((1, 16), (16, 0)):
        for y, a, b in ((1, 0, 4), (2, 1, 3)):
            s.fill(x0, y, z0 + a, x0 + 4, y, z0 + a, "green_wool"); s.fill(x0, y, z0 + b, x0 + 4, y, z0 + b, "green_wool")
        s.fill(x0, 3, z0 + 2, x0 + 4, 3, z0 + 2, "white_wool")
        s.fill(x0, 1, z0 + 1, x0, 2, z0 + 3, "green_wool")
        s.put(x0 + 2, 1, z0 + 2, "sketch:chest", facing="east")
        s.put(x0 + 5, 1, z0, "oak_fence"); s.put(x0 + 5, 2, z0, "lantern", hanging="false", waterlogged="false")
    s.put(19, 1, 19, "barrel", facing="up", open="false"); s.put(20, 1, 19, "barrel", facing="up", open="false")
    s.put(19, 2, 19, "barrel", facing="up", open="false"); s.put(21, 1, 17, "crafting_table")
    for _ in range(30):
        x, z = s.rng.randint(0, 23), s.rng.randint(0, 21)
        if (x, 1, z) not in s.grid and s.grid.get((x, 0, z), ("",))[0] == "minecraft:grass_block":
            s.put(x, 1, z, s.rng.choice(["fern", "fern", "short_grass"]))
    s.connect()
    s.sprite("digger_zombie", 11, -2, 13, 2.0)
    s.sprite("browncoat", 2, 1, 8, 2.0)
    return s


def pharaoh_tomb():
    s = Scene(13)
    s.fill(0, -1, 0, 30, 0, 32, "sand")
    for level in range(11):
        lo, hi = 5 + level, 25 - level
        block = "cut_sandstone" if level % 3 == 2 else "smooth_sandstone"
        s.fill(lo, level + 1, lo, hi, level + 1, hi, block)
    s.fill(15, 12, 15, 15, 12, 15, "gold_block")
    # entrance between the obelisks
    s.clear(14, 1, 19, 16, 3, 25)
    s.fill(13, 1, 25, 13, 4, 25, "chiseled_sandstone"); s.fill(17, 1, 25, 17, 4, 25, "chiseled_sandstone")
    for x in (11, 19):
        s.fill(x, 1, 28, x, 6, 28, "sandstone"); s.put(x, 7, 28, "gold_block")
    # cut open: the near quarter removed, inside the riddle hall and the burial chamber
    s.clear(16, 1, 16, 30, 13, 32)
    s.fill(16, 0, 16, 25, 0, 25, "smooth_sandstone")
    s.fill(20, 1, 16, 20, 1, 25, "cut_sandstone"); s.clear(20, 1, 20, 20, 1, 21)
    for x in (17, 18, 19):  # the riddle's "frames" on the pyramid wall
        s.put(x, 2, 15, "orange_glazed_terracotta", facing="south")
    s.fill(16, 1, 15, 19, 1, 15, "chiseled_sandstone")
    s.put(22, 1, 18, "quartz_block"); s.put(23, 1, 18, "quartz_block"); s.put(24, 1, 18, "gold_block")
    s.put(23, 1, 22, "vault", facing="south", vault_state="active", ominous="false")
    s.put(21, 1, 24, "sketch:chest", facing="north"); s.put(25, 1, 24, "sketch:chest", facing="north")
    for x, z in ((21, 17), (25, 17), (21, 21), (25, 21), (17, 22)):
        s.put(x, 1, z, "candle", candles=3, lit="true")
    s.put(17, 1, 24, "dispenser", facing="north", triggered="false"); s.put(18, 1, 24, "cobweb")
    s.connect()
    s.sprite("browncoat_desert", 18, 1, 19, 2.0)
    s.sprite("browncoat_desert", 15, 1, 29, 2.0)
    return s


def watchtower():
    s = Scene(17)
    s.fill(0, -1, 0, 25, 0, 25, "grass_block", snowy="false")
    s.fill(0, 0, 0, 25, 0, 25, "coarse_dirt", chance=0.12)
    # courtyard wall: north and west whole, south and east in ruins
    for i in range(2, 24):
        for x, z, tall in ((i, 2, True), (2, i, True), (i, 23, False), (23, i, False)):
            if not tall and s.rng.random() < 0.3:
                continue
            h = 4 if tall else s.rng.choice([1, 1, 2])
            s.fill(x, 1, z, x, h, z, "stone_bricks")
            if tall and i % 2 == 0:
                s.put(x, h + 1, z, "stone_brick_wall")
    s.clear(11, 1, 23, 14, 4, 23)
    s.fill(10, 1, 23, 10, 5, 23, "stone_bricks"); s.fill(15, 1, 23, 15, 5, 23, "stone_bricks")
    s.fill(11, 5, 23, 14, 5, 23, "stone_bricks"); s.fill(11, 4, 23, 14, 4, 23, "iron_bars")
    # 9×9 tower, five floors
    for y in range(1, 19):
        for x in range(4, 13):
            for z in range(4, 13):
                if x in (4, 12) or z in (4, 12):
                    s.put(x, y, z, "cobblestone" if y < 4 else "stone_bricks")
    for y in (6, 10, 14):
        for x, z in ((8, 12), (12, 8), (8, 4), (4, 8)):
            s.clear(x, y, z, x, y + 1, z)
    s.clear(8, 1, 12, 8, 2, 12)
    s.put(8, 1, 12, "spruce_door", facing="north", half="lower", hinge="left", open="false", powered="false")
    s.put(8, 2, 12, "spruce_door", facing="north", half="upper", hinge="left", open="false", powered="false")
    s.fill(4, 19, 4, 12, 19, 12, "stone_brick_slab", type="bottom")
    for x in range(4, 13, 2):
        for z in (4, 12):
            s.put(x, 20, z, "stone_bricks")
    for z in range(4, 13, 2):
        for x in (4, 12):
            s.put(x, 20, z, "stone_bricks")
    s.put(8, 20, 8, "sketch:bell", facing="south")
    s.age({"minecraft:stone_bricks": ["mossy_stone_bricks", "cracked_stone_bricks"],
           "minecraft:cobblestone": ["mossy_cobblestone"]}, 0.25)
    s.put(16, 1, 16, "campfire", lit="true", facing="south", signal_fire="false", waterlogged="false")
    s.put(14, 1, 8, "sketch:chest", facing="south"); s.put(15, 1, 8, "barrel", facing="up", open="false")
    s.put(18, 1, 6, "hay_block", axis="y"); s.put(18, 2, 6, "hay_block", axis="x")
    s.connect()
    s.sprite("browncoat", 17, 1, 19, 2.0)
    s.sprite("browncoat", 20, 1, 14, 2.0)
    s.sprite("zombat", 10, 21, 10, 0.8)
    return s


def time_capsule():
    s = Scene(19)
    s.fill(0, -8, 0, 23, -1, 23, "deepslate")
    s.fill(0, 0, 0, 23, 0, 23, "calcite")
    # the dome in the north-west
    cx, cz = 7, 7
    for x in range(cx - 6, cx + 7):
        for z in range(cz - 6, cz + 7):
            d = math.hypot(x - cx, z - cz)
            if d <= 6:
                s.put(x, 0, z, "sproutsquad:smooth_brainz_alloy_block")
            if 5.2 <= d <= 6.2:
                s.put(x, 1, z, "sproutsquad:reinforced_brainz_alloy_block")
            for y in range(2, 8):
                r = math.sqrt(max(0.0, 36 - (y - 1) ** 2))
                if abs(d - r) < 0.6:
                    s.put(x, y, z, "glass")
    s.put(cx, 1, cz, "sproutsquad:time_machine")
    for _ in range(14):
        x, z = s.rng.randint(0, 23), s.rng.randint(0, 23)
        if math.hypot(x - cx, z - cz) > 7 and not (x >= 11 and z >= 11):
            s.put(x, 1, z, "amethyst_cluster", facing="up", waterlogged="false")
    # cut open: the lab under the near quarter
    s.clear(11, -8, 11, 23, 0, 23)
    s.fill(11, -8, 11, 23, -8, 23, "sproutsquad:smooth_brainz_alloy_block")
    walls = {}
    for r in ((11, 11, 23, 16), (11, 16, 17, 23), (17, 16, 23, 23)):
        room(walls, *r)
    for (x, z), tall in walls.items():
        s.fill(x, -7, z, x, -3 if tall else -6, z, "sproutsquad:brainz_alloy_block")
    s.clear(14, -7, 16, 14, -5, 16); s.clear(17, -7, 19, 17, -5, 19)
    s.fill(18, -7, 16, 22, -5, 16, "light_blue_stained_glass")
    for x in range(13, 22, 2):
        s.put(x, -7, 13, "sproutsquad:brainz_alloy_slab", type="top", waterlogged="false")
        s.put(x, -6, 13, "sproutsquad:sun_battery", face="floor", facing="south")
    s.put(20, -7, 14, "sproutsquad:plant_pot", facing="south"); s.put(16, -7, 14, "sproutsquad:plant_pot", facing="south")
    for x in range(12, 17):
        for y in (-6, -4):
            s.put(x, y, 16, "redstone_lamp", lit="true")
    s.fill(12, -7, 21, 16, -7, 22, "observer", facing="up", powered="false")
    s.put(20, -7, 21, "vault", facing="south", vault_state="active", ominous="false")
    s.put(22, -7, 18, "copper_bulb", lit="true", powered="false"); s.put(18, -7, 22, "copper_bulb", lit="true", powered="false")
    s.connect()
    s.sprite("zombie_turret", 15, -7, 19, 1.4)
    s.sprite("robo_zombie", 18, -7, 13, 2.2)
    return s


def gnome_station():
    s = Scene(23)
    # asteroid
    for x in range(-2, 22):
        for z in range(-2, 22):
            for y in range(-10, 1):
                dx, dz, dy = (x - 10) / 11.5, (z - 10) / 11.5, (y + 1) / 9.5
                if dx * dx + dz * dz + dy * dy * (1.6 if y < -5 else 1.0) <= 1 + 0.12 * s.rng.random():
                    block = "stone" if y > -4 else "deepslate"
                    if s.rng.random() < 0.05:
                        block = "amethyst_block"
                    s.put(x, y, z, "end_stone" if y == 0 else block)
    # platform, dome, cage, landing pad
    s.fill(3, 0, 3, 16, 0, 16, "sproutsquad:smooth_brainz_alloy_block")
    cx, cz = 6, 6
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            d = math.hypot(x - cx, z - cz)
            for y in range(1, 5):
                r = math.sqrt(max(0.0, 9.5 - (y - 1) ** 2))
                if abs(d - r) < 0.6:
                    s.put(x, y, z, "glass")
    for x in range(11, 16):
        for z in range(4, 9):
            d = math.hypot(x - 13, z - 6)
            if 1.6 < d < 2.6:
                s.put(x, 0, z, "redstone_lamp", lit="true")
    s.put(13, 1, 6, "sproutsquad:golden_garden_gnome")
    for x in range(11, 14):
        for z in range(12, 15):
            if x in (11, 13) or z in (12, 14):
                s.fill(x, 1, z, x, 2, z, "iron_bars")
    s.fill(11, 3, 12, 13, 3, 14, "sproutsquad:brainz_alloy_slab", type="bottom", waterlogged="false")
    s.clear(19, -5, 9, 21, -2, 12)
    s.fill(18, -5, 9, 18, -2, 12, "amethyst_block", chance=0.5)
    s.put(4, 1, 14, "sketch:chest", facing="south"); s.put(5, 1, 14, "barrel", facing="up", open="false")
    s.connect()
    s.sprite("gnome", 12, 1, 13, 1.0)
    s.sprite("gnome", 8, 1, 11, 1.0)
    return s


def stranded_brig():
    s = Scene(29)
    s.fill(0, -1, 0, 31, 0, 22, "sand")
    for z in range(0, 6):
        s.fill(0, 0, z, 31, 0, z, "sketch:water")
    # hull: widest in the middle, narrower to the bow (x=27) and the stern (x=3)
    for x in range(3, 28):
        half = 3 if 7 <= x <= 22 else (2 if 5 <= x <= 24 else 1)
        for z in range(11 - half, 12 + half):
            for y in range(1, 5):
                edge = z in (11 - half, 11 + half) or x in (3, 27)
                bottom = y == 1
                if edge or bottom:
                    s.put(x, y, z, "spruce_planks" if y < 4 else "dark_oak_planks")
        s.fill(x, 4, 12 - half, x, 4, 10 + half, "dark_oak_planks")
    s.clear(15, 4, 10, 17, 4, 12)
    s.fill(14, 5, 11, 14, 15, 11, "spruce_log", axis="y")
    s.fill(14, 13, 9, 14, 13, 13, "spruce_fence")
    s.fill(14, 8, 9, 14, 12, 13, "white_wool")
    # captain's cabin at the stern
    s.fill(4, 5, 9, 8, 7, 13, "spruce_planks"); s.clear(5, 5, 10, 7, 6, 12)
    s.put(8, 6, 11, "glass"); s.put(6, 6, 13, "glass")
    s.fill(4, 8, 9, 8, 8, 13, "dark_oak_slab", type="bottom", waterlogged="false")
    s.put(6, 5, 11, "sketch:chest", facing="east")
    for x in (10, 18, 22):
        s.put(x, 3, 14, "dispenser", facing="south", triggered="false")
    s.connect()
    s.sprite("pirate_captain_ghost", 10, 5, 11, 2.2)
    s.sprite("browncoat_buccaneer", 20, 1, 18, 2.0)
    s.sprite("imp_pirate", 24, 5, 11, 1.2)
    return s


# id: (names, build, yaw, pitch); "cut open" marks a cutaway. A structure that is in the mod (EXPORTS)
# has its real pictures in docs/images/structures and is no longer sketched.
SKETCHES = {
    "graveyard_crypt": ({"en": "Crypt (cut open)", "ru": "Склеп (разрез)"}, crypt, 35, 48),
    "robo_junkyard": ({"en": "Robo Junkyard", "ru": "Свалка роботов"}, junkyard, 35, 38),
    "dig_site": ({"en": "Dig Site", "ru": "Раскопки"}, dig_site, 35, 40),
    "pharaoh_tomb": ({"en": "Pharaoh's Tomb (cut open)", "ru": "Гробница фараона (разрез)"}, pharaoh_tomb, 35, 30),
    "watchtower": ({"en": "Watchtower", "ru": "Сторожевая башня"}, watchtower, 35, 28),
    "time_capsule": ({"en": "Time Capsule (cut open)", "ru": "Капсула времени (разрез)"}, time_capsule, 35, 38),
    "gnome_station": ({"en": "Gnome Station", "ru": "Гномья станция"}, gnome_station, 35, 30),
    "stranded_brig": ({"en": "Stranded Brig", "ru": "Бриг на мели"}, stranded_brig, 35, 30),
}


# ================================================================ export to a structure template

DATA_VERSION = 5023  # of the game the templates are written for (as in the vanilla templates of 26.3)
STRUCTURE_DATA = "src/main/resources/data/sproutsquad/structure"


def gardener_letter():
    """The Gardener's Letter: a written book whose name and pages are translated by the game."""
    pages = [json.dumps({"translate": f"book.sproutsquad.gardener_letter.page{i}"}) for i in (1, 2, 3)]
    return {"Slot": P.Byte(0), "id": "minecraft:written_book", "count": 1, "components": {
        "minecraft:custom_name": {"translate": "item.sproutsquad.gardener_letter"},
        "minecraft:written_book_content": {"title": "The Gardener's Letter", "author": "-", "pages": pages, "resolved": P.Byte(1)},
        # the author line is a plain string, not translated: keep it out of the tooltip
        "minecraft:tooltip_display": {"hidden_components": ["minecraft:written_book_content"]},
    }}


def garden_shack_block_entity(block, props):
    if block == "minecraft:chest":
        return {"id": "minecraft:chest", "LootTable": "sproutsquad:chests/garden_shack/shed"}
    if block == "minecraft:barrel":
        return {"id": "minecraft:barrel", "LootTable": "sproutsquad:chests/garden_shack/barrel"}
    if block == "sproutsquad:mailbox":
        return {"id": "sproutsquad:mailbox", "Color": "white", "Items": [gardener_letter()]}
    return None


EXPORTS = {
    "garden_shack": {
        "build": garden_shack,
        "bounds": (0, 0, 0, 18, 7, 19),  # x0 y0 z0 x1 y1 z1; y=0 is the ground layer
        "variants": {  # wood of the shed: the generator picks one of the three
            "oak": {},
            "spruce": {"oak_planks": "spruce_planks", "oak_log": "spruce_log", "oak_door": "spruce_door",
                       "spruce_stairs": "dark_oak_stairs", "spruce_slab": "dark_oak_slab",
                       "oak_fence": "spruce_fence", "oak_fence_gate": "spruce_fence_gate"},
            "cherry": {"oak_planks": "cherry_planks", "oak_log": "cherry_log", "oak_door": "cherry_door",
                       "oak_fence": "cherry_fence", "oak_fence_gate": "cherry_fence_gate"},
        },
        "block_entity": garden_shack_block_entity,
    },
}
REAL_BLOCKS = {  # blocks that only the sketch draws as boxes
    "sketch:water": ("minecraft:water", {"level": "0"}),
    "sketch:chest": ("minecraft:chest", {"type": "single", "waterlogged": "false"}),
    "sketch:pot": ("minecraft:decorated_pot", {"cracked": "false", "waterlogged": "false"}),
    "sketch:bell": ("minecraft:bell", {"attachment": "floor", "powered": "false"}),
}


def export(name):
    spec = EXPORTS[name]
    x0, y0, z0, x1, y1, z1 = spec["bounds"]
    for variant, renames in spec["variants"].items():
        scene = spec["build"]()
        palette, index, blocks = [], {}, []
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    block, props = scene.grid.get((x, y, z), ("minecraft:air", {}))
                    if block in REAL_BLOCKS:
                        real, extra = REAL_BLOCKS[block]
                        props = {**extra, **({"facing": props["facing"]} if "facing" in props else {})}
                        block = real
                    if block.startswith("minecraft:"):
                        block = ns(renames.get(block.split(":")[1], block.split(":")[1]))
                    key = (block, tuple(sorted(props.items())))
                    if key not in index:
                        index[key] = len(palette)
                        palette.append({"id": block, **({"properties": dict(sorted(props.items()))} if props else {})})
                    entry = {"pos": [x - x0, y - y0, z - z0], "state": index[key]}
                    nbt = spec["block_entity"](block, props)
                    if nbt:
                        entry["nbt"] = nbt
                    blocks.append(entry)
        entities = []
        for entity, (x, y, z) in scene.entities:
            yaw = P.Float(scene.rng.uniform(-180, 180))
            entities.append({"pos": [x - x0 + 0.5, float(y - y0), z - z0 + 0.5], "blockPos": [x - x0, y - y0, z - z0],
                             "nbt": {"id": entity, "PersistenceRequired": P.Byte(1), "Rotation": [yaw, P.Float(0.0)]}})
        out = os.path.join(STRUCTURE_DATA, name, variant + ".nbt")
        P.write_nbt(out, {"size": [x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1], "entities": entities, "blocks": blocks,
                          "palette": palette, "DataVersion": DATA_VERSION})
        print(out, len(blocks), "blocks,", len(palette), "states,", len(entities), "entities")


def overview(names, locale):
    cell, label = 420, 36
    cols = 3
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * (cell + label)), (238, 240, 234))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", 22)
    except OSError:
        font = ImageFont.load_default()
    for i, name in enumerate(names):
        x, y = (i % cols) * cell, (i // cols) * (cell + label)
        image = Image.open(os.path.join(OUT, name + ".png")).convert("RGB").resize((cell, cell), Image.LANCZOS)
        sheet.paste(image, (x, y + label))
        d.text((x + 12, y + 6), SKETCHES[name][0][locale], fill=(30, 30, 30), font=font)
    out = os.path.join(OUT, f"overview_{locale}.png")
    save(sheet, out)
    print(out)


def main():
    if sys.argv[1:2] == ["--export"]:
        for name in sys.argv[2:] or list(EXPORTS):
            export(name)
        return
    names = sys.argv[1:] or list(SKETCHES)
    for name in names:
        title, build, yaw, pitch = SKETCHES[name]
        draw(build(), os.path.join(OUT, name + ".png"), yaw=yaw, pitch=pitch)
    if len(names) == len(SKETCHES):
        for locale in ("en", "ru"):
            overview(names, locale)


if __name__ == "__main__":
    main()
