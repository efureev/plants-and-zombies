#!/usr/bin/env python3
"""Model boxes and textures for the Corn Cannon plant and its corn cob rocket.

Usage (needs Pillow), from the repository root:
    python3 art/corn_cannon/draw_corn_cannon.py            # writes the textures and the model boxes below

Writes:
    src/main/resources/assets/sproutsquad/textures/entity/plant/corn_cannon/corn_cannon.png  (256x128)
    .../corn_cannon_aim.png (squinting while it fires), corn_cannon_sleep.png (closed eyes)   (256x128)
    src/main/resources/assets/sproutsquad/textures/entity/projectile/corn_cob.png            (128x64)
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/corn_cannon.png          (16x16)
    src/main/resources/assets/sproutsquad/textures/particle/popcorn_0..3.png                (8x8)
    the boxes between the `generated` markers of CornCannonModel.kt and CornCobModel.kt

The design: an ear of corn whose husk has opened at the front into a cannon's mouth, the cob looking out of
it; an eye bulges on each side over the mouth, corn silk on top, the curly tendril of the original drawing at the back.
It lies on a hub with six long corn leaves that stays put while the husk turns. Drawn at its real size,
1 texel = 1/16 block (about 3.6 blocks long).

Concept space (1 unit = 1 texel): x runs from the tail to the nose, y up from the ground, z across.
Model space (CornCannonModel): front = north (-z), y down from the root at 24, so a box (x, y, z, len, h, w)
becomes addBox(z, -(y + h), NOSE_Z - (x + len), w, h, len) before the part's pivot is taken off.
Box UV (Minecraft): first row top (w x len) and bottom; second row west, north, east, south. On the top face
the first row is the tail end; on west/east the columns run tail→nose (west) and nose→tail (east).
Colours: the Peashooter greens and the sun yellows of the style guide (docs/en/WORKSHOP.md).
"""
import math
import os
import re
import sys
from PIL import Image


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# greens of the style guide (Peashooter), dark to light
G = [c(h) for h in ("2d5213", "3f6d1f", "4a7e26", "5f9a2c", "79b930", "84c738", "98e244", "a7ec58", "bdf77c")]
# pale inside of the husk leaves
PALE = [c(h) for h in ("98b84a", "b4d064", "cde483", "e3f2a6")]
# kernels: highlight, light, base, shadow, seam
K_HI, K1, K2, K3, KG = c("fff6a8"), c("f7e64d"), c("f6cf37"), c("ecb427"), c("cb8a17")
# silk
S1, S2, S3 = c("d96c1c"), c("ecb427"), c("f7e64d")
EYE, HL = c("111f0a"), c("f2f4ee")
HOLE, HOLE2 = c("142608"), c("1f3a0d")
POP1, POP2, POP3 = c("fffbe8"), c("f3e9c4"), c("e0c98a")
CLEAR = (0, 0, 0, 0)

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
PLANT_DIR = os.path.join(ROOT, "entity", "plant", "corn_cannon")
MODEL_DIR = os.path.join("src", "main", "kotlin", "dev", "efureev", "sproutsquad", "client", "model")
TEX_W, TEX_H = 256, 128
COB_TEX_W, COB_TEX_H = 128, 64

# The nose of the cob is at x = 58; the model is centred on the entity: x = 29 is the turning axis.
NOSE_Z = 29
AXIS_Y = 12          # the husk's long axis, above the ground


class Box:
    def __init__(self, part, name, x, y, z, length, h, w, paint, **ctx):
        self.part, self.name = part, name
        self.x, self.y, self.z, self.len, self.h, self.w = x, y, z, length, h, w
        self.paint, self.ctx = paint, ctx


def octagon(part, name, x0, x1, w, h, cut, paint, axis_y=AXIS_Y, **ctx):
    """A rounded log along x: a wide box and a tall one, the tall one a pixel longer at each end (no faces in one plane)."""
    return [
        Box(part, name + "_wide", x0 + 1, axis_y - (h - 2 * cut) / 2, -w / 2, x1 - x0 - 2, h - 2 * cut, w, paint, cap=False, **ctx),
        Box(part, name + "_tall", x0, axis_y - h / 2, -(w - 2 * cut) / 2, x1 - x0, h, w - 2 * cut, paint, cap=True, **ctx),
    ]


def round_log(part, name, x0, x1, d, paint, axis_y=AXIS_Y, **ctx):
    """A round log along x of diameter d (12 or 8): a square a pixel in from the rim the whole length, and a wide
    and a tall bar a pixel shorter at each end (their end faces fall inside the square, so none share a visible plane)."""
    sq, bar = d - 2, d // 2
    return [
        Box(part, name + "_core", x0, axis_y - sq / 2, -sq / 2, x1 - x0, sq, sq, paint, **ctx),
        Box(part, name + "_wide", x0 + 1, axis_y - bar / 2, -d / 2, x1 - x0 - 2, bar, d, paint, **ctx),
        Box(part, name + "_tall", x0 + 1, axis_y - d / 2, -bar / 2, x1 - x0 - 2, d, bar, paint, **ctx),
    ]


# ---------------------------------------------------------------- the cannon
BOXES = []
# husk: tail cap, rear, main barrel; the main barrel's front face is the dark mouth
BOXES += octagon("husk", "tail", 0, 5, 12, 12, 3, "husk", seg=0)
BOXES += octagon("husk", "rear", 5, 14, 16, 16, 3, "husk", seg=1)
BOXES += octagon("husk", "main", 14, 35, 20, 20, 3, "husk", seg=2, mouth=True)
# an eye bulging on each side over the mouth, a pixel out from the husk so the side with the eye shows whole
BOXES += [
    Box("husk", "eye_left", 28, 16, -11, 6, 8, 7, "eye", side=-1),
    Box("husk", "eye_right", 28, 16, 4, 6, 8, 7, "eye", side=1),
]
# the cob, slid into the husk from its back end
BOXES += round_log("cob", "cob", 29, 52, 12, "cob")
BOXES += round_log("cob", "cob_tip", 51, 55, 8, "cob")
BOXES += [Box("cob", "cob_end", 55, AXIS_Y - 2, -2, 2, 4, 4, "cob")]
# husk leaves opened round the mouth: left, right and under it (over it the eyes look out)
BOXES += [
    Box("petal_left", "petal_left", 34, AXIS_Y - 8, -10, 13, 16, 1, "petal", side=-1),
    Box("petal_right", "petal_right", 34, AXIS_Y - 8, 9, 13, 16, 1, "petal", side=1),
    Box("petal_low", "petal_low", 34, 2, -7, 10, 1, 14, "petal", side=0),
]
# corn silk on top, behind the eyes
BOXES += [
    Box("silk", "silk_1", 20, 21, -3, 1, 6, 1, "silk", k=1),
    Box("silk", "silk_2", 22, 21, -1, 1, 9, 1, "silk", k=0),
    Box("silk", "silk_3", 21, 21, 0, 1, 7, 1, "silk", k=2),
    Box("silk", "silk_4", 23, 21, 1, 1, 8, 1, "silk", k=0),
    Box("silk", "silk_5", 20, 21, 2, 1, 5, 1, "silk", k=1),
]
# the curly tendril of the drawing, out of the tail
BOXES += [
    Box("tendril", "tendril_1", -5, AXIS_Y - 1, -1, 5, 2, 2, "stem"),
    Box("tendril", "tendril_2", -7, AXIS_Y - 1, -1, 2, 11, 2, "stem"),
    Box("tendril", "tendril_3", -7, AXIS_Y + 10, -1, 7, 2, 2, "stem"),
    Box("tendril", "tendril_4", -2, AXIS_Y + 6, -1, 2, 4, 2, "stem"),
    Box("tendril", "tendril_5", -4, AXIS_Y + 6, -1, 2, 2, 2, "stem"),
    Box("tendril", "tendril_leaf", -11, AXIS_Y + 12, -3, 5, 1, 6, "small_leaf"),
]
# the ground: a round hub and six long corn leaves (each its own part, turned about the hub)
BOXES += octagon("ground", "hub", 29 - 10, 29 + 10, 20, 20, 5, "hub", axis_y=1)
# the hub's octagon is flat: 2 high (the cut runs across, not up)
for b in BOXES:
    if b.part == "ground" and b.name.startswith("hub"):
        b.y, b.h = 0, 3 if b.ctx["cap"] else 2
LEAF_YAWS = [30, 90, 150, 210, 270, 330]
for k, yaw in enumerate(LEAF_YAWS):
    BOXES += [
        Box(f"leaf_{k}", f"leaf_{k}_base", 29 + 8, 1, -3, 9, 1, 6, "ground_leaf", seg=0),
        Box(f"leaf_{k}", f"leaf_{k}_tip", 29 + 17, 0, -3, 10, 1, 6, "ground_leaf", seg=1),
    ]

# Parts: parent, pivot in concept space, rotation (degrees, model axes).
PARTS = {
    "body": ("root", None, (0, 0, 0)),
    "husk": ("body", (14, 2, 0), (0, 0, 0)),
    "cob": ("husk", (29, AXIS_Y, 0), (0, 0, 0)),
    "petal_left": ("husk", (34, AXIS_Y, -10), (0, 30, 0)),
    "petal_right": ("husk", (34, AXIS_Y, 10), (0, -30, 0)),
    "petal_low": ("husk", (34, 3, 0), (12, 0, 0)),
    "silk": ("husk", (22, 22, 0), (-25, 0, 0)),
    "tendril": ("husk", (0, AXIS_Y, 0), (0, 0, 0)),
    "ground": ("root", (29, 0, 0), (0, 0, 0)),
}
for k, yaw in enumerate(LEAF_YAWS):
    PARTS[f"leaf_{k}"] = ("ground", (29, 0, 0), (0, yaw, 0))
PART_ORDER = ["body", "husk", "cob", "petal_left", "petal_right", "petal_low", "silk", "tendril", "ground"] + \
    [f"leaf_{k}" for k in range(len(LEAF_YAWS))]

# The flying cob: the same cob with the cone at both ends, along x here (CornCobModel builds it along y).
COB_BOXES = [
    Box("cob", "end_back", -6, -2, -2, 2, 4, 4, "cob"),
    *round_log("cob", "tip_back", -4, 0, 8, "cob", axis_y=0),
    *round_log("cob", "cob", -1, 24, 12, "cob", axis_y=0),
    *round_log("cob", "tip", 23, 27, 8, "cob", axis_y=0),
    Box("cob", "end", 27, -2, -2, 2, 4, 4, "cob"),
]
# CornCobModel builds it along y (x → -y, y → x): the same boxes in model dimensions, for the UV and the paint
COB_MODEL = [Box(b.part, b.name, b.x, b.y, b.z, b.w, b.len, b.h, b.paint, axis="y") for b in COB_BOXES]


# ---------------------------------------------------------------- UV packing
def uv_size(b):
    return int(2 * (b.len + b.w)), int(b.len + b.h)


def pack(boxes, width, height):
    """Shelf packing (tallest first). Boxes of one size and painter that look alike share one rectangle."""
    shared, out = {}, {}
    x = y = shelf = 0
    for b in sorted(boxes, key=lambda b: (-uv_size(b)[1], -uv_size(b)[0])):
        key = (b.len, b.h, b.w, b.paint, tuple(sorted((k, v) for k, v in b.ctx.items() if k != "side")))
        if b.paint in ("eye", "petal", "silk"):
            key = b.name  # painted per side
        if key in shared:
            out[b.name] = shared[key]
            continue
        bw, bh = uv_size(b)
        if x + bw > width:
            x, y, shelf = 0, y + shelf, 0
        if y + bh > height:
            sys.exit(f"UV does not fit: {b.name}")
        out[b.name] = shared[key] = (x, y)
        x, shelf = x + bw, max(shelf, bh)
    return out


UV = pack(BOXES, TEX_W, TEX_H)
COB_UV = pack(COB_MODEL, COB_TEX_W, COB_TEX_H)


# ---------------------------------------------------------------- painters
# Each painter gets (img, x0, y0, w, h, face, box, mood) for one face; w x h is the face's texture rectangle.
def put(img, x, y, col):
    img.putpixel((x, y), col)


def fill(img, x0, y0, w, h, f):
    for j in range(h):
        for i in range(w):
            col = f(i, j)
            if col is not None:
                put(img, x0 + i, y0 + j, col)


def shade(level, light):
    """A green by height on the barrel: light from above."""
    return G[max(0, min(len(G) - 1, level + light))]


def husk(img, x0, y0, w, h, face, b, mood):
    """Husk leaves running along the ear: seams along x, lighter on top, a darker lip at each segment's front."""
    seg = b.ctx["seg"]
    if face in ("west", "east"):
        def side(i, j):
            front = (w - 1 - i) if face == "west" else i  # distance from the nose end
            y = b.y + b.h - 1 - j                         # height in concept space
            rel = (y - 2) / 20
            col = shade(3 + round(rel * 3), 0)
            band = (y + seg) % 5
            if band == 0:
                col = shade(2 + round(rel * 3), 0)        # seam between two husk leaves
            elif band == 1 and rel > 0.35:
                col = shade(4 + round(rel * 3), 0)        # the leaf's lit edge
            if front == 0 or (front == 1 and band in (2, 3)):
                col = shade(2 + round(rel * 2), 0)        # leaf tips overlapping the next segment
            return col
        fill(img, x0, y0, w, h, side)
    elif face == "top":
        def top(i, j):
            front = h - 1 - j
            zc = abs(i - (w - 1) / 2) / max(1, w / 2)       # 0 in the middle, 1 at the edge
            col = shade(7 - round(zc * 2.5), 0)
            if (i + seg) % 5 == 0:
                col = shade(5 - round(zc * 2), 0)
            if front == 0:
                col = shade(5, 0)
            return col
        fill(img, x0, y0, w, h, top)
    elif face == "bottom":
        fill(img, x0, y0, w, h, lambda i, j: G[2] if (i + seg) % 5 else G[1])
    elif face == "north" and b.ctx.get("mouth"):
        # the opening the cob comes out of: dark, a ring of pale husk inside the lip
        cx, cy = (w - 1) / 2, (h - 1) / 2
        def mouth(i, j):
            r = max(abs(i - cx), abs(j - cy)) + 0.4 * min(abs(i - cx), abs(j - cy))
            y = b.y + b.h - 1 - j
            if r < 6.3:
                return HOLE if r < 5 else HOLE2
            if r < 7.6:
                return PALE[1] if y > AXIS_Y else PALE[0]
            return shade(4 + (1 if y > AXIS_Y + 4 else 0), 0)
        fill(img, x0, y0, w, h, mouth)
    else:  # an end of a segment seen end-on: rings, darker at the rim
        cx, cy = (w - 1) / 2, (h - 1) / 2
        def end(i, j):
            r = max(abs(i - cx) / max(1, w / 2), abs(j - cy) / max(1, h / 2))
            return G[3] if r > 0.8 else (G[4] if r > 0.5 else G[5])
        fill(img, x0, y0, w, h, end)


def eye(img, x0, y0, w, h, face, b, mood):
    """A green bulb with a dark eye on its outer side only (style guide: dark eye, 1 px highlight)."""
    side = b.ctx["side"]
    base = lambda i, j: G[7] if j < 2 else (G[6] if j < h - 2 else G[5])
    if face == "top":
        fill(img, x0, y0, w, h, lambda i, j: G[8] if 0 < i < w - 1 and 0 < j < h - 1 else G[7])
        return
    if face == "bottom":
        fill(img, x0, y0, w, h, lambda i, j: G[4])
        return
    fill(img, x0, y0, w, h, base)
    outer = (face == "west" and side < 0) or (face == "east" and side > 0)
    if outer:
        # on the outer side, near the front: west columns run tail→nose, east nose→tail
        px = w - 4 if face == "west" else 1
        draw_eye(img, x0 + px, y0 + 2, mood, width=3, hl_right=face == "east")


def draw_eye(img, x, y, mood, width=3, hl_right=False):
    brow = G[3]
    for i in range(width):
        put(img, x + i, y - 1, brow)
    if mood == "normal":
        for j in range(4):
            for i in range(width):
                put(img, x + i, y + j, EYE)
        put(img, x + (width - 1 if hl_right else 0), y, HL)
    elif mood == "aim":
        for i in range(width):
            put(img, x + i, y, G[3])          # lids half down: a squint
            put(img, x + i, y + 1, G[3])
            put(img, x + i, y + 2, EYE)
            put(img, x + i, y + 3, EYE)
        put(img, x + (width - 1 if hl_right else 0), y + 2, HL)
    else:  # sleep: a dash
        for i in range(width):
            put(img, x + i, y + 3, EYE)


def kernels(img, x0, y0, w, h, face, b, mood):
    """Rows of kernels along the cob: 2 x 2 kernels, a darker seam between rows, staggered along the row."""
    if b.ctx.get("axis") == "y":          # the flying cob, built standing: every side runs along it top to bottom
        along_x, along_y = False, face not in ("top", "bottom")
    else:
        along_x = face in ("west", "east")  # columns run along the cob
        along_y = face in ("top", "bottom") # rows run along the cob
    def k(i, j):
        if along_x:
            row, pos = j, i
        elif along_y:
            row, pos = i, j
        else:
            row, pos = j, i + (j // 3) % 2  # the end: a grid
        if row % 3 == 2:
            return KG
        stagger = (row // 3) % 2
        p = (pos + stagger * 1) % 3
        if p == 2:
            return K3
        lit = row % 3 == 0 and p == 0
        col = K_HI if lit else (K1 if face != "bottom" else K2)
        kid = (row // 3) * 7 + (pos + stagger) // 3 * 13
        if kid % 11 == 3 and not lit:
            col = K2                                 # a few kernels a shade riper
        if b.ctx.get("axis") != "y" and face in ("west", "east") and j > h * 0.6:
            col = K2 if not lit else K1
        return col
    fill(img, x0, y0, w, h, k)


def petal(img, x0, y0, w, h, face, b, mood):
    """A husk leaf bent outward: green outside, pale inside, its tip rounded off toward the nose."""
    side = b.ctx["side"]
    # which face looks inward (toward the cob): for the side leaves the face toward z = 0, for the low leaf the top
    if side == 0:
        inner, outer = "top", "bottom"
    else:
        inner, outer = ("east", "west") if side < 0 else ("west", "east")
    L = b.len
    def tip_cut(along, across, across_n):
        """True where the pointed tip is cut away: `along` counts from the nose end."""
        edge = min(across, across_n - 1 - across)
        return edge < (across_n / 2 - 1) * (1 - along / 5) if along < 5 else False
    if face in (inner, outer):
        is_inner = face == inner
        if face in ("west", "east"):
            def f(i, j):
                along = (w - 1 - i) if face == "west" else i
                if tip_cut(along, j, h):
                    return CLEAR
                mid = abs(j - (h - 1) / 2) < 0.6
                if is_inner:
                    return PALE[1] if mid or j % 4 == 0 else (PALE[3] if j < h / 2 else PALE[2])
                return G[4] if mid or j % 4 == 0 else (G[6] if j < h / 2 else G[5])
        else:
            def f(i, j):
                along = (h - 1 - j) if face == "top" else j
                if tip_cut(along, i, w):
                    return CLEAR
                mid = abs(i - (w - 1) / 2) < 0.6
                if is_inner:
                    return PALE[1] if mid or i % 4 == 0 else PALE[2]
                return G[3] if mid or i % 4 == 0 else G[4]
        for j in range(h):
            for i in range(w):
                put(img, x0 + i, y0 + j, f(i, j))
    elif face == "north":
        fill(img, x0, y0, w, h, lambda i, j: CLEAR)   # the tip: cut away
    else:
        # thin edges: as the leaf, cut near the tip
        def edge(i, j):
            n = w if w > 1 else h
            if face in ("west", "east"):
                along = (w - 1 - i) if face == "west" else i
            elif face in ("top", "bottom"):
                along = (h - 1 - j) if face == "top" else j
            else:
                along = L
            return CLEAR if along < 2 else G[3]
        fill(img, x0, y0, w, h, edge)


def silk(img, x0, y0, w, h, face, b, mood):
    k = b.ctx["k"]
    cols = [S3, S2, S2, S1, S1, S1, S1, S1]
    fill(img, x0, y0, w, h, lambda i, j: cols[min(j + k, 7)] if face not in ("top", "bottom") else S3)


def stem(img, x0, y0, w, h, face, b, mood):
    fill(img, x0, y0, w, h, lambda i, j: G[3] if face == "top" else (G[1] if face == "bottom" else (G[2] if j else G[3])))


def small_leaf(img, x0, y0, w, h, face, b, mood):
    if face == "top":
        fill(img, x0, y0, w, h, lambda i, j: None if (i in (0, w - 1) and j in (0, h - 1)) else (G[4] if i == w // 2 else G[6]))
    else:
        fill(img, x0, y0, w, h, lambda i, j: G[3])


def hub(img, x0, y0, w, h, face, b, mood):
    if face == "top":
        cx, cy = (w - 1) / 2, (h - 1) / 2
        fill(img, x0, y0, w, h, lambda i, j: G[2] if ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5 % 4 < 1 else G[3])
    else:
        fill(img, x0, y0, w, h, lambda i, j: G[1] if j else G[2])


def ground_leaf(img, x0, y0, w, h, face, b, mood):
    """A long corn leaf lying on the ground, a pale midrib, pointed at its outer end."""
    seg = b.ctx["seg"]
    if face == "top":
        def f(i, j):
            along = j if seg == 1 else None      # rows run inner (first row) → outer? top: first row = tail end
            # top face rows run from the part's -x... the leaf points along +x of concept space, i.e. toward the
            # nose of an unturned leaf: the last row is its outer end
            out = h - 1 - j
            mid = abs(i - (w - 1) / 2)
            if seg == 1 and mid > (out + 1) * 0.45 + 0.2:
                return CLEAR                     # the pointed tip
            if mid < 0.6:
                return G[6]                      # midrib
            return G[4] if (j + seg) % 6 else G[3]
        fill(img, x0, y0, w, h, f)
    elif face == "bottom":
        fill(img, x0, y0, w, h, lambda i, j: G[1])
    else:
        fill(img, x0, y0, w, h, lambda i, j: CLEAR if (seg == 1 and face == "north") else G[2])


PAINTERS = {
    "husk": husk, "eye": eye, "cob": kernels, "petal": petal, "silk": silk, "stem": stem,
    "small_leaf": small_leaf, "hub": hub, "ground_leaf": ground_leaf,
}


def paint_box(img, u, v, b, mood, dims=None):
    """Box UV of a box w (x) by h (y) by length (z)."""
    w, h, d = dims or (b.w, b.h, b.len)
    w, h, d = int(w), int(h), int(d)
    p = PAINTERS[b.paint]
    p(img, u + d, v, w, d, "top", b, mood)
    p(img, u + d + w, v, w, d, "bottom", b, mood)
    p(img, u, v + d, d, h, "west", b, mood)
    p(img, u + d, v + d, w, h, "north", b, mood)
    p(img, u + d + w, v + d, d, h, "east", b, mood)
    p(img, u + 2 * d + w, v + d, w, h, "south", b, mood)


def cannon_texture(mood):
    img = Image.new("RGBA", (TEX_W, TEX_H), CLEAR)
    for b in BOXES:
        u, v = UV[b.name]
        paint_box(img, u, v, b, mood)
    return img


def cob_texture():
    img = Image.new("RGBA", (COB_TEX_W, COB_TEX_H), CLEAR)
    for b in COB_MODEL:
        u, v = COB_UV[b.name]
        paint_box(img, u, v, b, "normal")
    return img


def icon():
    """16x16: the cannon in profile, nose to the right."""
    rows = [
        "................",
        "................",
        ".tt.............",
        "t..t...ss.......",
        "t.t...sEE.......",
        "t....hhEEhp.....",
        ".t.hhhhhhhhpkk..",
        "..hhhhhhhhhKkkKk",
        "..HHhhhhhhhkKkkk",
        "...HHHHHHHHpkk..",
        "....dddddddp....",
        "..gggggggggggg..",
        "................",
        "................",
        "................",
        "................",
    ]
    pal = {"t": G[2], "s": S1, "E": EYE, "h": G[6], "H": G[4], "p": PALE[1], "k": K1, "K": K3, "d": G[3], "g": G[2]}
    img = Image.new("RGBA", (16, 16), CLEAR)
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal:
                put(img, i, j, pal[ch])
    put(img, 7, 4, HL)
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
                put(img, i, j, POP1 if j < 2 or not below else (POP3 if (i + j) % 3 == 0 else POP2))
    return img


# ---------------------------------------------------------------- Kotlin
def fmt(v):
    return f"{float(v):g}f"


def model_point(p):
    x, y, z = p
    return z, -y, NOSE_Z - x


def part_offset(name):
    parent, pivot, _ = PARTS[name]
    if pivot is None:
        return (0, 24, 0)
    mine = model_point(pivot)
    if parent == "root":
        return (mine[0], mine[1] + 24, mine[2])
    ppivot = PARTS[parent][1]
    base = model_point(ppivot) if ppivot is not None else (0, 0, 0)
    return tuple(a - b for a, b in zip(mine, base))


def kotlin_parts():
    lines = []
    var = {"root": "mesh.root"}
    for name in PART_ORDER:
        parent, pivot, rot = PARTS[name]
        boxes = [b for b in BOXES if b.part == name]
        pm = model_point(pivot) if pivot is not None else (0, 0, 0)
        chain = []
        for b in boxes:
            u, v = UV[b.name]
            mx, my, mz = b.z, -(b.y + b.h), NOSE_Z - (b.x + b.len)
            chain.append((f"                .texOffs({u}, {v}).addBox({fmt(mx - pm[0])}, {fmt(my - pm[1])}, {fmt(mz - pm[2])}, "
                          f"{fmt(b.w)}, {fmt(b.h)}, {fmt(b.len)})", f"  // {b.name}"))
        off = part_offset(name)
        if any(rot):
            pose = "PartPose.offsetAndRotation(" + ", ".join(fmt(o) for o in off) + ", " + \
                   ", ".join(fmt(round(math.radians(r), 5)) for r in rot) + ")"
        else:
            pose = "PartPose.offset(" + ", ".join(fmt(o) for o in off) + ")"
        if chain:
            chain[-1] = (chain[-1][0] + ",", chain[-1][1])
            body = "CubeListBuilder.create()\n" + "\n".join(code + note for code, note in chain)
        else:
            body = "CubeListBuilder.create(),"
        lines.append(f"            val {name} = {var[parent]}.addOrReplaceChild(\"{name}\", {body}\n                {pose})")
        var[name] = name
    return "\n".join(lines)


def kotlin_cob():
    lines = []
    for b in COB_BOXES:
        u, v = COB_UV[b.name]
        # along y, nose to -y: concept x → -y, concept y → x, concept z → z
        lines.append(f"                .texOffs({u}, {v}).addBox({fmt(b.y)}, {fmt(-(b.x + b.len) + 12)}, {fmt(b.z)}, "
                     f"{fmt(b.h)}, {fmt(b.len)}, {fmt(b.w)})" + ("," if b is COB_BOXES[-1] else "") + f"  // {b.name}")
    return "            mesh.root.addOrReplaceChild(\"cob\", CubeListBuilder.create()\n" + "\n".join(lines) + \
        "\n                PartPose.offsetAndRotation(0f, 24f, 0f, 0f, -1.5708f, 1.5708f))"


def write_generated(path, code, tex_w, tex_h):
    text = open(path, encoding="utf-8").read()
    new, n = re.subn(r"(// region generated by art/corn_cannon/draw_corn_cannon\.py)(.*?)(\n[ \t]*// endregion)",
                     lambda m: m.group(1) + "\n" + code + m.group(3), text, flags=re.S)
    if n != 1:
        sys.exit(f"no generated region in {path}")
    new = re.sub(r"LayerDefinition\.create\(mesh, \d+, \d+\)", f"LayerDefinition.create(mesh, {tex_w}, {tex_h})", new)
    open(path, "w", encoding="utf-8").write(new)


def main():
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
    write_generated(os.path.join(MODEL_DIR, "CornCannonModel.kt"), kotlin_parts(), TEX_W, TEX_H)
    write_generated(os.path.join(MODEL_DIR, "CornCobModel.kt"), kotlin_cob(), COB_TEX_W, COB_TEX_H)


if __name__ == "__main__":
    main()
