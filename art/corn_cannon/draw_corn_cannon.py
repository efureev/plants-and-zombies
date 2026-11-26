#!/usr/bin/env python3
"""Textures for the Corn Cannon plant and its corn cob rocket.

Usage (needs Pillow), from the repository root:
    python3 art/corn_cannon/draw_corn_cannon.py            # writes the textures below
    python3 art/corn_cannon/draw_corn_cannon.py --kotlin   # prints the boxes for CornCannonModel / CornCobModel

Writes:
    src/main/resources/assets/sproutsquad/textures/entity/plant/corn_cannon/corn_cannon.png  (128x64)
    .../corn_cannon_aim.png (squinting while it fires), corn_cannon_sleep.png (closed eyes)   (128x64)
    src/main/resources/assets/sproutsquad/textures/entity/projectile/corn_cob.png            (64x32)
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/corn_cannon.png          (16x16)
    src/main/resources/assets/sproutsquad/textures/particle/popcorn_0..3.png                (8x8)

The boxes are the approved design (plan concept, 1 px = 1 texel): x runs from the tail to the nose, y up,
z across. Model space (CornCannonModel): front = north (-z), y down from the root at 24, so a box
(x, y, z, len, h, w) becomes addBox(z, -(y + h), NOSE_Z - (x + len), w, h, len) in body space.
Box UV (Minecraft): first row top (w x len) and bottom; second row west, north, east, south.
Colours: the Peashooter greens and the sun yellows of the style guide (docs/en/WORKSHOP.md).
"""
import os
import sys
from PIL import Image


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


D0, D1, D2, D3, D4, D5, D6, D7 = (c(h) for h in ("2d5213", "3f6d1f", "4a7e26", "5f9a2c", "79b930", "84c738", "98e244", "a7ec58"))
K_HI, K1, K2, K3, KG = c("fbf08a"), c("f7e64d"), c("f6cf37"), c("ecb427"), c("c98f1c")
EYE, HL = c("111f0a"), c("f2f4ee")
HOLE, HOLE2 = c("1b300c"), c("24400f")
POP1, POP2, POP3 = c("fffbe8"), c("f3e9c4"), c("e0c98a")
CLEAR = (0, 0, 0, 0)
DARKER = {D7: D6, D6: D5, D5: D4, D4: D3, D3: D2, D2: D1, D1: D0, D0: D0}

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
PLANT_DIR = os.path.join(ROOT, "entity", "plant", "corn_cannon")

# Nose of the husk sits this far forward of the body origin: the box (husk + cob, x 0..29) is centred on it.
NOSE_Z = 14

# (part, name, x, y, z, length, height, width, painter) — x from the tail forward, y up, z across.
BOXES = [
    ("husk", "husk_back", 0, 3, -3, 2, 6, 6, "husk"),
    ("husk", "husk_rear", 2, 2, -4, 4, 8, 8, "husk"),
    ("husk", "husk_mid", 6, 1, -5, 8, 10, 10, "husk_eyes"),
    ("husk", "husk_front", 14, 1, -5, 4, 10, 10, "husk_mouth"),
    ("husk", "lid", 18, 10, -5, 5, 2, 10, "lid"),
    ("husk", "lid_tip", 23, 11, -3, 3, 2, 6, "leaf_light"),
    ("husk", "lid_curl", 26, 13, -1, 2, 1, 2, "leaf_light"),
    ("husk", "cradle", 18, 1, -4, 4, 1, 8, "leaf_dark"),
    ("husk", "leaf_left", 18, 2, -5, 3, 4, 1, "leaf"),
    ("husk", "leaf_right", 18, 2, 4, 3, 4, 1, "leaf"),
    ("husk", "hole", 17, 2, -4, 1, 8, 8, "hole"),
    ("cob", "cob", 15, 2, -4, 11, 8, 8, "cob"),
    ("cob", "cob_tip", 26, 3, -3, 2, 6, 6, "cob"),
    ("cob", "cob_end", 28, 4, -2, 1, 4, 4, "cob"),
    ("tendril", "tendril_1", -3, 6, -1, 3, 1, 1, "stem"),
    ("tendril", "tendril_2", -4, 6, -1, 1, 5, 1, "stem"),
    ("tendril", "tendril_3", -3, 10, -1, 2, 1, 1, "stem"),
    ("tendril", "tendril_4", -2, 8, -1, 1, 2, 1, "stem"),
    ("tendril", "tendril_leaf", -5, 11, -1, 2, 1, 3, "leaf"),
    ("ground", "pad", 3, 0, -4, 12, 1, 8, "ground"),
    ("ground", "ground_leaf_fl", 13, 0, -8, 4, 1, 4, "ground_leaf"),
    ("ground", "ground_leaf_fr", 13, 0, 4, 4, 1, 4, "ground_leaf"),
    ("ground", "ground_leaf_bl", 3, 0, -8, 4, 1, 4, "ground_leaf"),
    ("ground", "ground_leaf_br", 3, 0, 4, 4, 1, 4, "ground_leaf"),
]
# Part pivots in concept coordinates: the husk tilts about its bottom near the tail, the cob slides
# back into the husk from its own back end, the tendril sways from where it leaves the husk.
PIVOTS = {"husk": (6, 1, 0), "cob": (15, 6, 0), "tendril": (0, 6, 0), "ground": (0, 0, 0)}
PARENT = {"husk": "body", "cob": "husk", "tendril": "husk", "ground": "body"}

# The flying cob: the same cob with the cone at both ends.
COB_BOXES = [
    ("cob_end_back", -3, 4, -2, 1, 4, 4),
    ("cob_tip_back", -2, 3, -3, 2, 6, 6),
    ("cob", 0, 2, -4, 12, 8, 8),
    ("cob_tip", 12, 3, -3, 2, 6, 6),
    ("cob_end", 14, 4, -2, 1, 4, 4),
]


def uv_size(length, h, w):
    return 2 * (length + w), length + h


def pack(boxes, width, height):
    """Shelf packing of the box UV rectangles (tallest first); boxes of one size and painter share one rectangle."""
    shared = {}
    order = sorted(boxes, key=lambda b: -uv_size(*b[1:4])[1])
    out, x, y, shelf = {}, 0, 0, 0
    for name, length, h, w, painter in order:
        key = (length, h, w, painter)
        if key in shared and painter != "husk_eyes":
            out[name] = shared[key]
            continue
        bw, bh = uv_size(length, h, w)
        if x + bw > width:
            x, y, shelf = 0, y + shelf, 0
        if y + bh > height:
            sys.exit(f"UV does not fit: {name}")
        out[name] = (x, y)
        shared.setdefault(key, (x, y))
        x, shelf = x + bw, max(shelf, bh)
    return out


UV = pack([(b[1], b[5], b[6], b[7], b[8]) for b in BOXES], 128, 64)
# the flying cob is modelled along y (CornCobModel, like the original's MissileModel): depth = width, height = length
COB_UV = pack([(b[0], b[6], b[4], b[6], "cob") for b in COB_BOXES], 64, 32)


# ---- painters: (img, x0, y0, w, h, face, box) where face is top/bottom/west/north/east/south --------
def fill(img, x0, y0, w, h, f):
    for j in range(h):
        for i in range(w):
            img.putpixel((x0 + i, y0 + j), f(i, j))


def husk_side(i, j, w, h):
    rel = 1 - j / max(1, h - 1)               # 1 at the top row
    col = D6 if rel > 0.72 else (D5 if rel > 0.38 else (D4 if rel > 0.12 else D3))
    if j in (round(h * 0.3), round(h * 0.62)):  # leaf veins along the husk
        col = DARKER[col]
    return col


def husk(img, x0, y0, w, h, face, box, mood=None):
    if face == "top":
        fill(img, x0, y0, w, h, lambda i, j: D5 if i in (w // 3, w - 1 - w // 3) else (D6 if 0 < i < w - 1 else D5))
    elif face == "bottom":
        fill(img, x0, y0, w, h, lambda i, j: D2)
    elif face in ("west", "east"):
        fill(img, x0, y0, w, h, lambda i, j: husk_side(i, j, w, h))
    else:  # the ends: rings of a husk seen end-on, darker at the rim
        def end(i, j):
            col = husk_side(i, j, w, h)
            return DARKER[col] if i in (0, w - 1) else col
        fill(img, x0, y0, w, h, end)


def eyes(img, x0, y0, w, h, face, mood):
    """An eye on each side of the middle husk box, close to its front end."""
    if face not in ("west", "east"):
        return
    # west: the first column is the back (z1); east: the first column is the front (z0)
    front_cols = [w - 1, w - 2, w - 3] if face == "west" else [0, 1, 2]
    toward_front = front_cols[0]
    top = 4                                    # rows 4..6 of the 10-high side
    for k, col in enumerate(front_cols):
        img.putpixel((x0 + col, y0 + top - 1), D3)                 # brow, a tone darker
        if mood == "normal":
            for j in range(3):
                img.putpixel((x0 + col, y0 + top + j), EYE)
        elif mood == "aim":
            img.putpixel((x0 + col, y0 + top), D3)                 # the lid comes down
            img.putpixel((x0 + col, y0 + top + 1), EYE)
        else:  # sleep: a dash
            img.putpixel((x0 + col, y0 + top + 2), EYE)
    if mood == "normal":
        img.putpixel((x0 + toward_front, y0 + top), HL)


def husk_eyes(mood):
    def paint(img, x0, y0, w, h, face, box):
        husk(img, x0, y0, w, h, face, box)
        eyes(img, x0, y0, w, h, face, mood)
    return paint


def husk_mouth(img, x0, y0, w, h, face, box):
    husk(img, x0, y0, w, h, face, box)
    if face == "north":  # the opening the cob comes out of
        fill(img, x0 + 1, y0 + 1, w - 2, h - 2, lambda i, j: HOLE if 0 < i < w - 3 and 0 < j < h - 3 else HOLE2)


def lid(img, x0, y0, w, h, face, box):
    if face == "bottom":
        fill(img, x0, y0, w, h, lambda i, j: D3)
    else:
        fill(img, x0, y0, w, h, lambda i, j: D5 if (face == "top" and i in (w // 3, w - 1 - w // 3)) else D6)


def leaf(light, dark):
    def paint(img, x0, y0, w, h, face, box):
        if face == "bottom":
            fill(img, x0, y0, w, h, lambda i, j: dark)
        else:
            fill(img, x0, y0, w, h, lambda i, j: dark if (i + j) % 4 == 0 else light)
    return paint


def solid(col):
    return lambda img, x0, y0, w, h, face, box: fill(img, x0, y0, w, h, lambda i, j: col)


def cob(img, x0, y0, w, h, face, box):
    def kernel(i, j):
        a, b = i % 3, j % 3
        if a == 2 and b == 2:
            return KG
        if a == 2 or b == 2:
            return K3
        if a == 0 and b == 0:
            return K_HI
        return K2 if face == "bottom" else K1
    fill(img, x0, y0, w, h, kernel)


def ground(img, x0, y0, w, h, face, box):
    if face == "top":
        fill(img, x0, y0, w, h, lambda i, j: D3 if (i + j) % 5 else D2)
    else:
        fill(img, x0, y0, w, h, lambda i, j: D2)


def ground_leaf(img, x0, y0, w, h, face, box):
    if face == "top":
        fill(img, x0, y0, w, h, lambda i, j: D3 if i == j or i + j == w - 1 else D4)
    else:
        fill(img, x0, y0, w, h, lambda i, j: D3)


PAINTERS = {
    "husk": husk, "husk_mouth": husk_mouth, "lid": lid,
    "leaf": leaf(D5, D4), "leaf_light": leaf(D7, D6), "leaf_dark": leaf(D3, D2),
    "hole": solid(HOLE), "cob": cob, "stem": leaf(D2, D1), "ground": ground, "ground_leaf": ground_leaf,
}


def paint_box(img, u, v, length, h, w, painter):
    """Box UV of a box w (x) by h (y) by length (z)."""
    d = length
    painter(img, u + d, v, w, d, "top", None)
    painter(img, u + d + w, v, w, d, "bottom", None)
    painter(img, u, v + d, d, h, "west", None)
    painter(img, u + d, v + d, w, h, "north", None)
    painter(img, u + d + w, v + d, d, h, "east", None)
    painter(img, u + 2 * d + w, v + d, w, h, "south", None)


def cannon_texture(mood):
    img = Image.new("RGBA", (128, 64), CLEAR)
    for part, name, x, y, z, length, h, w, painter in BOXES:
        u, v = UV[name]
        p = husk_eyes(mood) if painter == "husk_eyes" else PAINTERS[painter]
        paint_box(img, u, v, length, h, w, p)
    return img


def cob_texture():
    img = Image.new("RGBA", (64, 32), CLEAR)
    for name, x, y, z, length, h, w in COB_BOXES:
        u, v = COB_UV[name]
        paint_box(img, u, v, w, length, w, cob)
    return img


def icon():
    """16x16: the cannon in profile, nose to the right."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    rows = [
        "................",
        "................",
        "...........l....",
        "..t......lll....",
        ".t.t.hhhhhhLL...",
        ".tt.hhhhhhhhkkk.",
        "...hhhhheehhkKkk",
        "...hhhhheehhkkKk",
        "...hhhhhhhhhkKk.",
        "....dddddddhkk..",
        ".....ddddddd....",
        "...gggggggggg...",
        "................",
        "................",
        "................",
        "................",
    ]
    pal = {"l": D7, "L": D6, "t": D2, "h": D5, "d": D3, "e": EYE, "k": K1, "K": K3, "g": D2}
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal:
                img.putpixel((i, j), pal[ch])
    img.putpixel((8, 6), HL)
    return img


def popcorn(k):
    """8x8 popcorn puffs, four shapes."""
    shapes = [
        ["..##....", ".####...", ".#####..", "..####..", "...##...", "........", "........", "........"],
        ["........", "..###...", ".#####..", ".####...", "..##....", "........", "........", "........"],
        ["........", "...##...", "..####..", ".#####..", "..###...", "...#....", "........", "........"],
        ["........", "........", "..###...", ".####...", ".###....", "........", "........", "........"],
    ]
    img = Image.new("RGBA", (8, 8), CLEAR)
    for j, row in enumerate(shapes[k]):
        for i, ch in enumerate(row):
            if ch == "#":
                below = j + 1 < 8 and shapes[k][j + 1][i] == "#"
                img.putpixel((i, j), POP1 if j < 2 or not below else (POP3 if (i + j) % 3 == 0 else POP2))
    return img


# ---- Kotlin -------------------------------------------------------------------------------------
def kotlin_box(x, y, z, length, h, w, pivot=(0, 0, 0)):
    """Concept box -> addBox(...) in the space of a part pivoted at the concept point `pivot`."""
    mx, my, mz = z, -(y + h), NOSE_Z - (x + length)
    px, py, pz = pivot[2], -pivot[1], NOSE_Z - pivot[0]
    return f"{fmt(mx - px)}, {fmt(my - py)}, {fmt(mz - pz)}, {fmt(w)}, {fmt(h)}, {fmt(length)}"


def fmt(v):
    return f"{float(v):g}f"


def print_kotlin():
    print("// CornCannonModel boxes (generated by art/corn_cannon/draw_corn_cannon.py --kotlin)")
    def pm(p):
        return p[2], -p[1], NOSE_Z - p[0]
    for part in ("husk", "cob", "tendril", "ground"):
        pv = PIVOTS[part]
        parent = (0, 0, 0) if PARENT[part] == "body" else pm(PIVOTS[PARENT[part]])
        off = [a - b for a, b in zip(pm(pv), parent)]
        print(f"// part {part}: parent {PARENT[part]}, PartPose.offset({', '.join(fmt(o) for o in off)})")
        for p, name, x, y, z, length, h, w, _ in BOXES:
            if p == part:
                u, v = UV[name]
                print(f'    "{name}" texOffs({u}, {v}).addBox({kotlin_box(x, y, z, length, h, w, pv)})')
    print("// CornCobModel boxes (along y, nose to -y, centred)")
    for name, x, y, z, length, h, w in COB_BOXES:
        u, v = COB_UV[name]
        print(f'    "{name}" texOffs({u}, {v}).addBox({fmt(z)}, {fmt(6 - (x + length))}, {fmt(z)}, {fmt(w)}, {fmt(length)}, {fmt(w)})')


def main():
    if "--kotlin" in sys.argv:
        print_kotlin()
        return
    os.makedirs(PLANT_DIR, exist_ok=True)
    cannon_texture("normal").save(os.path.join(PLANT_DIR, "corn_cannon.png"))
    cannon_texture("aim").save(os.path.join(PLANT_DIR, "corn_cannon_aim.png"))
    cannon_texture("sleep").save(os.path.join(PLANT_DIR, "corn_cannon_sleep.png"))
    os.makedirs(os.path.join(ROOT, "entity", "projectile"), exist_ok=True)
    cob_texture().save(os.path.join(ROOT, "entity", "projectile", "corn_cob.png"))
    icon().save(os.path.join(ROOT, "item", "plant_icon", "corn_cannon.png"))
    os.makedirs(os.path.join(ROOT, "particle"), exist_ok=True)
    for k in range(4):
        popcorn(k).save(os.path.join(ROOT, "particle", f"popcorn_{k}.png"))


if __name__ == "__main__":
    main()
