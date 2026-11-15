#!/usr/bin/env python3
"""Textures for the Needle Spruce plant.

Usage (needs Pillow), from the repository root:
    python3 art/needle_spruce/draw_needle_spruce.py [preview_dir]

Writes:
    src/main/resources/assets/sproutsquad/textures/entity/plant/needle_spruce/needle_spruce.png         (128x64)
    .../needle_spruce_angry.png, needle_spruce_strain.png, needle_spruce_sleep.png                     (128x64)
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/needle_spruce.png                    (16x16)
and, if preview_dir is given, needle_spruce_faces.png there (the four faces side by side, x8).

The UV layout follows NeedleSpruceModel.createBodyLayer() (box UV: top and bottom faces in the first
row of a box, then east, north (front), west, south). Colours and faces: the approved design (source
drawing in art/needle_spruce/sketch.jpg); style guide in docs/en/WORKSHOP.md.
"""
import os
import random
import sys
from PIL import Image

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
ENTITY_DIR = os.path.join(ROOT, "entity", "plant", "needle_spruce")
ICON_PATH = os.path.join(ROOT, "item", "plant_icon", "needle_spruce.png")


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# needles: colder and darker than the Peashooter greens, so the tree reads as a spruce
EDGE, G1, G2, G3, G4 = c("1f3a22"), c("2b4f2c"), c("355f33"), c("42733c"), c("5a8f4c")
# bark: the orange-brown trunk of the drawing
B0, B1, B2, B3 = c("4e2c17"), c("6b3d20"), c("87512b"), c("a3683a")
# fangs: ivory
F1, F2, F3 = c("c9c09c"), c("e4ddc0"), c("f6f2e2")
# face
WHITE, PUPIL, DARK, SWEAT = c("f2f4ee"), c("5b1a14"), c("111f0a"), c("9fd8f5")
CLEAR = (0, 0, 0, 0)

# Box UV origins (u, v) and sizes (w, h, d) — must match NeedleSpruceModel.createBodyLayer()
TIER1 = (0, 0, 20, 12, 20)
TIER2 = (0, 32, 16, 9, 16)
TIER3 = (64, 32, 11, 7, 11)
TOP = (80, 16, 4, 4, 4)
TRUNK_LOWER = (80, 0, 6, 9, 6)
TRUNK_UPPER = (104, 0, 6, 7, 6)
FANG_LONG = [(96, 16, 3, 3, 3), (108, 16, 2, 2, 2), (116, 16, 1, 3, 1)]
FANG_SHORT = [(96, 24, 3, 2, 3), (108, 24, 2, 2, 2), (116, 24, 1, 2, 1)]

# the face on the front of the bottom tier: 20x12, eyes 3x3 at x 4..6 and 13..15, rows 3..5
EYES_X = (4, 13)
EYES_Y = 3


def needles(img, x0, y0, w, h, kind, rng):
    base = {"top": G3, "bottom": G1}.get(kind, G2)
    dim = {"top": G2, "bottom": EDGE}.get(kind, G1)
    for j in range(h):
        for i in range(w):
            col = dim if rng.random() < 0.3 else base
            if kind == "side":
                if j == 0:
                    col = G4 if rng.random() < 0.25 else G3
                elif j == h - 1 or i == 0 or i == w - 1:
                    col = G1
            elif i in (0, w - 1) or j in (0, h - 1):
                col = dim
            img.putpixel((x0 + i, y0 + j), col)


def bark(img, x0, y0, w, h, kind, rng):
    for j in range(h):
        for i in range(w):
            col = B2 if i % 2 == 0 else B1
            if rng.random() < 0.12:
                col = B0
            if kind in ("top", "bottom"):
                col = B3 if abs(i - w / 2 + 0.5) + abs(j - h / 2 + 0.5) < 2 else B2
            elif i in (0, w - 1):
                col = B1
            img.putpixel((x0 + i, y0 + j), col)


def fang(img, x0, y0, w, h, kind, rng):
    for j in range(h):
        for i in range(w):
            col = {"top": F3, "bottom": F1}.get(kind, F2)
            if kind == "side" and w > 1 and (i in (0, w - 1) or j == h - 1):
                col = F1
            img.putpixel((x0 + i, y0 + j), col)


def box(img, spec, paint, seed, front=None):
    """Paints one box in box-UV layout; `front` paints the north face instead of `paint`."""
    u, v, w, h, d = spec
    rng = random.Random(seed)
    paint(img, u + d, v, w, d, "top", rng)
    paint(img, u + d + w, v, w, d, "bottom", rng)
    paint(img, u, v + d, d, h, "side", rng)                      # east
    (front or paint)(img, u + d, v + d, w, h, "side", rng)       # north = front
    paint(img, u + d + w, v + d, d, h, "side", rng)              # west
    paint(img, u + 2 * d + w, v + d, w, h, "side", rng)          # south


def px(img, x, y, col):
    img.putpixel((x, y), col)


def face_painter(mood):
    """The front of the bottom tier with eyes for `mood`: normal, angry, strain or sleep."""
    def paint(img, x0, y0, w, h, kind, rng):
        needles(img, x0, y0, w, h, "side", random.Random(11))
        for k, ex in enumerate(EYES_X):
            ox, oy = x0 + ex, y0 + EYES_Y
            inner = 2 if k == 0 else 0           # the eye's side towards the middle of the face
            outer = 2 - inner
            step = 1 if k == 0 else -1
            if mood in ("normal", "angry"):
                for j in range(3):
                    for i in range(3):
                        px(img, ox + i, oy + j, WHITE)
                px(img, ox + 1, oy + 1, PUPIL)
                px(img, ox + 1, oy + 2, PUPIL)
                if mood == "normal":
                    px(img, ox + inner, oy - 1, WHITE)   # the white corner of the drawing
                else:
                    # brows slope down to the middle and cover the inner top corner of the eye
                    px(img, ox + inner, oy, DARK)
                    px(img, ox + outer - step, oy - 2, DARK)
                    px(img, ox + outer, oy - 2, DARK)
                    px(img, ox + outer + step, oy - 1, DARK)
                    px(img, ox + inner, oy - 1, DARK)
            elif mood == "strain":
                # ">" on the left eye, "<" on the right: they point at each other
                for i, j in ((0, 0), (1, 0), (2, 1), (0, 2), (1, 2)):
                    px(img, ox + i if k == 0 else ox + 2 - i, oy + j, DARK)
                if k == 1:
                    px(img, ox + 4, oy - 1, SWEAT)
                    px(img, ox + 4, oy, SWEAT)
            else:  # sleep: closed eyes
                for i in range(3):
                    px(img, ox + i, oy + 1, DARK)
    return paint


def draw_body(mood):
    img = Image.new("RGBA", (128, 64), CLEAR)
    box(img, TIER1, needles, 300, front=face_painter(mood))
    box(img, TIER2, needles, 400)
    box(img, TIER3, needles, 500)
    box(img, TOP, needles, 600)
    box(img, TRUNK_LOWER, bark, 100)
    box(img, TRUNK_UPPER, bark, 200)
    for i, spec in enumerate(FANG_LONG + FANG_SHORT):
        box(img, spec, fang, 700 + i)
    return img


def draw_icon():
    img = Image.new("RGBA", (16, 16), CLEAR)
    tiers = [(7, 8, 1, 2, G3), (5, 10, 3, 5, G2), (4, 11, 5, 7, G3), (3, 12, 7, 10, G2), (2, 13, 10, 12, G2)]
    for a, b, y0, y1, col in tiers:
        for j in range(y0, y1):
            for i in range(a, b + 1):
                px(img, i, j, EDGE if i in (a, b) else (G4 if j == y0 else col))
    for j in range(12, 15):
        px(img, 7, j, B1)
        px(img, 8, j, B2)
    px(img, 5, 10, WHITE)
    px(img, 10, 10, WHITE)
    for i in (5, 6, 9, 10):  # the fangs under the front
        px(img, i, 12, F2)
    return img


def main():
    os.makedirs(ENTITY_DIR, exist_ok=True)
    names = {"normal": "needle_spruce", "angry": "needle_spruce_angry",
             "strain": "needle_spruce_strain", "sleep": "needle_spruce_sleep"}
    bodies = {}
    for mood, name in names.items():
        bodies[mood] = draw_body(mood)
        bodies[mood].save(os.path.join(ENTITY_DIR, name + ".png"))
    os.makedirs(os.path.dirname(ICON_PATH), exist_ok=True)
    draw_icon().save(ICON_PATH)

    if len(sys.argv) > 1:
        out = sys.argv[1]
        os.makedirs(out, exist_ok=True)
        u, v, w, h, d = TIER1
        sheet = Image.new("RGBA", (4 * (w + 2), h), CLEAR)
        for i, mood in enumerate(names):
            sheet.paste(bodies[mood].crop((u + d, v + d, u + d + w, v + d + h)), (i * (w + 2), 0))
        sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(os.path.join(out, "needle_spruce_faces.png"))


if __name__ == "__main__":
    main()
