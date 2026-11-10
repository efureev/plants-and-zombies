#!/usr/bin/env python3
"""README banner for Sprout Squad: chunky 3D pixel wordmark in the spirit of the original mod logo.

Usage (needs Pillow): python3 art/logo/draw_banner.py docs/images/logo.png 4
Also writes *_preview_light.png / *_preview_dark.png next to the output.
"""
import random
import sys
from PIL import Image, ImageDraw

OUT = sys.argv[1]
SCALE = int(sys.argv[2]) if len(sys.argv) > 2 else 4

W, H = 240, 140            # base pixel grid
CELL = 3                   # glyph cell -> base pixels
rng = random.Random(7)


def c(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


OUTLINE = c("15120e")

GLYPHS = {
    "S": [".######.", "########", "##......", "#######.", ".#######", "......##", "......##", "########", ".######."],
    "P": ["#######.", "########", "##....##", "##....##", "########", "#######.", "##......", "##......", "##......"],
    "R": ["#######.", "########", "##....##", "##....##", "#######.", "######..", "##..##..", "##...##.", "##....##"],
    "O": [".######.", "########", "##....##", "##....##", "##....##", "##....##", "##....##", "########", ".######."],
    "U": ["##....##", "##....##", "##....##", "##....##", "##....##", "##....##", "##....##", "########", ".######."],
    "T": ["########", "########", "...##...", "...##...", "...##...", "...##...", "...##...", "...##...", "...##..."],
    "Q": [".######..", "########.", "##....##.", "##....##.", "##....##.", "##..#.##.", "##...###.", "#########", ".######.#"],
    "A": [".######.", "########", "##....##", "##....##", "########", "########", "##....##", "##....##", "##....##"],
    "D": ["#######.", "########", "##....##", "##....##", "##....##", "##....##", "##....##", "########", "#######."],
}


def word_mask(text, x0, y0, jitter):
    """Set of base pixels covered by the word's letter faces."""
    pts = set()
    x = x0
    for i, ch in enumerate(text):
        oy = jitter[i % len(jitter)]
        for gy, row in enumerate(GLYPHS[ch]):
            for gx, bit in enumerate(row):
                if bit == "#":
                    for dy in range(CELL):
                        for dx in range(CELL):
                            pts.add((x + gx * CELL + dx, y0 + oy + gy * CELL + dy))
        x += len(GLYPHS[ch][0]) * CELL + 5
    return pts


def extrude(face, depth, dx_per, dy_per):
    """Pixels of the extrusion behind `face`, mapped to their depth k (1 = nearest the face)."""
    side = {}
    for k in range(depth, 0, -1):
        for (x, y) in face:
            p = (x + round(k * dx_per), y + round(k * dy_per))
            if p not in face:
                side[p] = k
    return side


def paint(canvas, pts, color_fn):
    px = canvas.load()
    for (x, y) in pts:
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = color_fn(x, y)


def outline(canvas, pts, width=2):
    px = canvas.load()
    ring = set()
    for (x, y) in pts:
        for dx in range(-width, width + 1):
            for dy in range(-width, width + 1):
                if abs(dx) + abs(dy) <= width + 1:
                    q = (x + dx, y + dy)
                    if q not in pts:
                        ring.add(q)
    for (x, y) in ring:
        if 0 <= x < W and 0 <= y < H:
            px[x, y] = OUTLINE


def speckle(base, light, dark, p_light=0.18, p_dark=0.14):
    cache = {}

    def fn(x, y):
        if (x, y) not in cache:
            r = rng.random()
            cache[(x, y)] = light if r < p_light else dark if r < p_light + p_dark else base
        return cache[(x, y)]
    return fn


def draw_word(canvas, text, x0, y0, jitter, face_fn, side_fn, top_hi, edge_hi, edge_sh, fringe=None):
    face = word_mask(text, x0, y0, jitter)
    side = extrude(face, depth=7, dx_per=0.3, dy_per=1.0)
    allp = face | set(side)
    outline(canvas, allp)
    paint(canvas, set(side), lambda x, y: side_fn(x, y, side[(x, y)]))
    paint(canvas, face, face_fn)
    # bevel: light top/left edge, dark bottom/right edge of every letter face
    paint(canvas, {(x, y) for (x, y) in face if (x + 1, y) not in face or (x, y + 1) not in face}, lambda x, y: edge_sh)
    paint(canvas, {(x, y) for (x, y) in face if (x - 1, y) not in face}, lambda x, y: edge_hi)
    paint(canvas, {(x, y) for (x, y) in face if (x, y - 1) not in face}, lambda x, y: top_hi)
    if fringe:
        blades = set()
        for (x, y) in face:
            if (x, y - 1) not in face and rng.random() < 0.28:
                blades.add((x, y - 1))
                if rng.random() < 0.35:
                    blades.add((x, y - 2))
        outline(canvas, blades, width=1)
        paint(canvas, face | set(side), lambda x, y: canvas.getpixel((x, y)))
        paint(canvas, blades, lambda x, y: fringe)
    return allp


img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

# ---------------------------------------------------------------- SPROUT ---
grass = speckle(c("6cc43c"), c("8edc55"), c("55a52f"))
grass_side_green = speckle(c("4f9a2c"), c("5fae36"), c("3f7f22"), 0.2, 0.2)
dirt = speckle(c("7a5235"), c("936444"), c("5e3e27"), 0.2, 0.2)


drip = {}


def sprout_side(x, y, k):
    if k <= 2:
        return grass_side_green(x, y)
    if k <= 4:
        # irregular grass drips into the dirt, consistent per column
        depth = drip.setdefault(x, rng.choice([2, 2, 2, 3, 3, 4]))
        if k <= depth:
            return grass_side_green(x, y)
    return dirt(x, y)


SPROUT_X, SPROUT_Y = 34, 22
draw_word(img, "SPROUT", SPROUT_X, SPROUT_Y, [0, -2, 1, -1, 0, 2], grass, sprout_side, c("c4f58e"), c("8edc55"), c("4c9a2a"))

# ----------------------------------------------------------------- SQUAD ---
clay = speckle(c("c06c4f"), c("d6825f"), c("a65a42"))
clay_side = speckle(c("7d3f31"), c("8c4838"), c("68342a"), 0.2, 0.2)
SQUAD_X, SQUAD_Y = 82, 72
draw_word(img, "SQUAD", SQUAD_X, SQUAD_Y, [1, -1, 1, -1, 2], clay, lambda x, y, k: clay_side(x, y), c("f2ab86"), c("d6825f"), c("8f4c3a"))

# --------------------------------------------------- sprout from the "O" ---
sp = Image.new("RGBA", (W, H), (0, 0, 0, 0))
d = ImageDraw.Draw(sp)
LEAF, LEAF_HI, LEAF_SH, RIB, STEM = c("5cb83a"), c("9be862"), c("3d8a26"), c("2f6e1d"), c("4a9a2e")
ox = SPROUT_X + 3 * (8 * CELL + 5) + 11          # centre of the "O"
top = SPROUT_Y - 1
d.rectangle([ox, top - 9, ox + 1, top + 1], fill=STEM)
d.polygon([(ox, top - 6), (ox - 5, top - 11), (ox - 11, top - 13), (ox - 14, top - 11), (ox - 10, top - 7), (ox - 4, top - 4)], fill=LEAF)
d.line([(ox - 1, top - 6), (ox - 7, top - 10), (ox - 12, top - 12)], fill=RIB)
d.point([(ox - 11, top - 12), (ox - 9, top - 12), (ox - 6, top - 10), (ox - 12, top - 11)], fill=LEAF_HI)
d.point([(ox - 9, top - 7), (ox - 5, top - 5)], fill=LEAF_SH)
d.polygon([(ox + 1, top - 8), (ox + 5, top - 14), (ox + 11, top - 18), (ox + 16, top - 18), (ox + 14, top - 13), (ox + 8, top - 8), (ox + 2, top - 5)], fill=LEAF)
d.line([(ox + 2, top - 7), (ox + 8, top - 13), (ox + 14, top - 17)], fill=RIB)
d.point([(ox + 12, top - 17), (ox + 10, top - 16), (ox + 7, top - 14), (ox + 13, top - 16)], fill=LEAF_HI)
d.point([(ox + 11, top - 11), (ox + 6, top - 8)], fill=LEAF_SH)
sp_pts = {(x, y) for x in range(W) for y in range(H) if sp.getpixel((x, y))[3] > 0}
outline(img, sp_pts, width=1)
img.alpha_composite(sp)

# ---------------------------------------- golden badge with the pot mascot ---
B = 40
badge = Image.new("RGBA", (B, B), (0, 0, 0, 0))
bd = ImageDraw.Draw(badge)
GOLD, GOLD_HI, GOLD_SH, SKY, SKY_HI = c("f4c13a"), c("fff09a"), c("c9861a"), c("8fd3ff"), c("b9e6ff")
bd.rectangle([0, 0, B - 1, B - 1], fill=GOLD_SH)
bd.rectangle([0, 0, B - 2, B - 2], fill=GOLD)
bd.line([(0, 0), (B - 2, 0)], fill=GOLD_HI)
bd.line([(0, 0), (0, B - 2)], fill=GOLD_HI)
bd.line([(1, 1), (B - 3, 1)], fill=GOLD_HI)
bd.rectangle([4, 4, B - 5, B - 5], fill=GOLD_SH)
bd.rectangle([5, 5, B - 5, B - 5], fill=SKY)
bd.rectangle([5, 5, B - 5, 8], fill=SKY_HI)
bd.rectangle([5, B - 9, B - 5, B - 5], fill=c("6cc43c"))
bd.line([(5, B - 9), (B - 5, B - 9)], fill=c("8edc55"))
POT, POT_HI, POT_RIM, POT_RIM_HI, POT_SH, SOIL = c("b4654f"), c("c97758"), c("cf7c60"), c("e3967a"), c("8c4838"), c("5a3d28")
K = c("2a1c14")
# pot (outlined)
bd.rectangle([10, 19, 29, 23], fill=K)
bd.polygon([(11, 23), (28, 23), (26, 33), (13, 33)], fill=K)
bd.rectangle([11, 20, 28, 22], fill=POT_RIM)
bd.line([(11, 20), (28, 20)], fill=POT_RIM_HI)
bd.line([(13, 20), (26, 20)], fill=SOIL)
bd.polygon([(12, 23), (27, 23), (25, 32), (14, 32)], fill=POT)
bd.line([(13, 24), (14, 31)], fill=POT_HI)
bd.line([(26, 23), (24, 32)], fill=POT_SH)
bd.rectangle([16, 25, 17, 27], fill=c("1a1410"))
bd.rectangle([22, 25, 23, 27], fill=c("1a1410"))
bd.point([(16, 25), (22, 25)], fill=c("ffffff"))
bd.point([(14, 28), (15, 28), (24, 28), (25, 28)], fill=c("e88d7a"))
bd.line([(18, 29), (19, 30), (20, 30), (21, 29)], fill=c("3a1c15"))
# sprout
bd.rectangle([19, 11, 20, 19], fill=K)
bd.line([(19, 12), (19, 19)], fill=STEM)
bd.polygon([(19, 15), (15, 10), (10, 9), (9, 11), (12, 14), (18, 16)], fill=K)
bd.polygon([(19, 14), (15, 11), (11, 10), (12, 12), (17, 15)], fill=LEAF)
bd.polygon([(20, 13), (24, 8), (30, 6), (31, 8), (27, 12), (21, 15)], fill=K)
bd.polygon([(20, 13), (24, 9), (29, 7), (27, 11), (21, 14)], fill=LEAF)
bd.point([(12, 11), (13, 11), (27, 8), (28, 8), (25, 9)], fill=LEAF_HI)
rot = badge.rotate(-10, resample=Image.NEAREST, expand=True)
bx, by = SQUAD_X - rot.width + 3, SQUAD_Y - 18
rot_pts = {(bx + x, by + y) for x in range(rot.width) for y in range(rot.height) if rot.getpixel((x, y))[3] > 0}
outline(img, rot_pts, width=2)
img.alpha_composite(rot, (bx, by))

# ------------------------------------------------------------- finish ---
bbox = img.getbbox()
img = img.crop((bbox[0] - 4, bbox[1] - 4, bbox[2] + 4, bbox[3] + 4))

# sticker border: light ring around the whole logo so it reads on dark backgrounds too
sticker = Image.new("RGBA", img.size, (0, 0, 0, 0))
sp_ = sticker.load()
src = img.load()
w_, h_ = img.size
solid = {(x, y) for x in range(w_) for y in range(h_) if src[x, y][3] > 0}
for (x, y) in solid:
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if abs(dx) + abs(dy) <= 3:
                q = (x + dx, y + dy)
                if q not in solid and 0 <= q[0] < w_ and 0 <= q[1] < h_:
                    sp_[q] = c("fff6dc")
sticker.alpha_composite(img)
img = sticker
big = img.resize((img.width * SCALE, img.height * SCALE), Image.NEAREST)
big.save(OUT)
for name, bg in (("light", "ffffff"), ("dark", "0d1117")):
    p = Image.new("RGBA", (big.width + 60, big.height + 60), c(bg))
    p.alpha_composite(big, (30, 30))
    p.save(OUT.replace(".png", f"_preview_{name}.png"))
print(img.size, big.size)
