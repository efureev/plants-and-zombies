#!/usr/bin/env python3
"""Textures for Potling, the Sprout Squad mascot plant.

Usage (needs Pillow), from the repository root:
    python3 art/potling/draw_potling.py [preview_dir]

Writes:
    src/main/resources/assets/sproutsquad/textures/entity/plant/potling/potling.png        (64x64)
    src/main/resources/assets/sproutsquad/textures/entity/plant/potling/potling_sleep.png  (64x64)
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/potling.png             (16x16)
and, if preview_dir is given, potling_preview.png there (UV sheet + flat front view, x8).

The UV layout follows PotlingModel.createBodyLayer() (box UV: top/bottom faces in the first
row of a box, then east, north (front), west, south). Style: docs/ROADMAP.md, section 8.3 —
light from above, no black outlines, eyes #111f0a with a 1px shine, 1-2px mouth.
"""
import os
import sys
from PIL import Image

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
ENTITY_DIR = os.path.join(ROOT, "entity", "plant", "potling")
ICON_PATH = os.path.join(ROOT, "item", "plant_icon", "potling.png")


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# terracotta (mascot colours from the mod icon, art/logo/draw_icon.py)
POT_DEEP, POT_SH, POT, POT_HI = c("6e3528"), c("8c4838"), c("b4654f"), c("c97758")
RIM, RIM_HI = c("cf7c60"), c("e3967a")
SOIL_SH, SOIL, SOIL_HI = c("3f2a1b"), c("5a3d28"), c("70503a")
# greens: Peashooter scale (ROADMAP 8.1)
LEAF_DEEP, LEAF_SH, LEAF, LEAF_HI, LEAF_TIP = c("3f6d1f"), c("4a7e26"), c("79b930"), c("98e244"), c("bdf77c")
# face (ROADMAP 8.3)
EYE, SHINE, BLUSH, MOUTH = c("111f0a"), c("ffffff"), c("e88d7a"), c("4a2219")
CLEAR = (0, 0, 0, 0)


def fill(img, x, y, w, h, col):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            img.putpixel((xx, yy), col)


def rows(img, x, y, pattern, palette):
    """Paint a small pixel pattern: each string is a row, each char a palette key ('.' = skip)."""
    for dy, line in enumerate(pattern):
        for dx, ch in enumerate(line):
            if ch != ".":
                img.putpixel((x + dx, y + dy), palette[ch])


def pot_side(img, x, y, w, h, highlight_col=None):
    """A wall of the pot: under-rim shadow on top, darker bottom row, optional 1px light stripe."""
    fill(img, x, y, w, h, POT)
    fill(img, x, y, w, 1, POT_SH)
    fill(img, x, y + h - 1, w, 1, POT_SH)
    if highlight_col is not None:
        fill(img, x + highlight_col, y + 1, 1, h - 2, POT_HI)
    # a few clay specks, at most two tones per spot
    for sx, sy in ((w - 2, 2), (2, h - 2)):
        if 0 < sx < w and 0 < sy < h - 1:
            img.putpixel((x + sx, y + sy), POT_HI if (sx + sy) % 2 else POT_SH)


def draw_body(sleeping):
    img = Image.new("RGBA", (64, 64), CLEAR)

    # ---- pot walls: box 8x6x8 at (0,0) ----
    fill(img, 8, 0, 8, 8, POT_SH)          # top (hidden under the rim)
    fill(img, 16, 0, 8, 8, POT_DEEP)       # bottom
    pot_side(img, 0, 8, 8, 6, highlight_col=1)   # east
    pot_side(img, 8, 8, 8, 6)                    # north = front, face drawn below
    pot_side(img, 16, 8, 8, 6)                   # west
    pot_side(img, 24, 8, 8, 6, highlight_col=6)  # south

    # face on the front (8x6): eyes 2x2 with a shine, blush, 2px mouth
    fx, fy = 8, 8
    if sleeping:
        for ex in (1, 5):                      # closed eyes: little dashes
            fill(img, fx + ex, fy + 2, 2, 1, EYE)
    else:
        for ex in (1, 5):
            fill(img, fx + ex, fy + 1, 2, 2, EYE)
            img.putpixel((fx + ex, fy + 1), SHINE)
    img.putpixel((fx + 0, fy + 3), BLUSH)
    img.putpixel((fx + 7, fy + 3), BLUSH)
    fill(img, fx + 3, fy + 4, 2, 1, MOUTH)

    # ---- rim: box 10x2x10 at (0,14) ----
    # top face (10x10): light lip around the soil
    fill(img, 10, 14, 10, 10, RIM_HI)
    fill(img, 11, 15, 8, 8, SOIL)
    fill(img, 11, 15, 8, 1, SOIL_SH)       # inner shadow under the far lip
    fill(img, 11, 15, 1, 8, SOIL_SH)
    for p in ((14, 18), (16, 20), (13, 21), (17, 17)):
        img.putpixel(p, SOIL_HI)
    fill(img, 20, 14, 10, 10, POT_SH)      # bottom face: the ring under the lip
    for i, x in enumerate((0, 10, 20, 30)):  # four sides 10x2: light top row, base below
        fill(img, x, 24, 10, 1, RIM_HI)
        fill(img, x, 25, 10, 1, RIM)

    # ---- base: box 6x2x6 at (32,0) ----
    fill(img, 38, 0, 6, 6, POT_SH)         # top (hidden)
    fill(img, 44, 0, 6, 6, POT_DEEP)       # bottom
    for x in (32, 38, 44, 50):
        fill(img, x, 6, 6, 1, POT_SH)
        fill(img, x, 7, 6, 1, POT_DEEP)

    # ---- stem: box 1x5x1 at (56,0) ----
    img.putpixel((57, 0), LEAF_HI)
    img.putpixel((58, 0), LEAF_SH)
    for i, x in enumerate((56, 57, 58, 59)):
        fill(img, x, 1, 1, 5, (LEAF, LEAF_HI, LEAF, LEAF_SH)[i])
        img.putpixel((x, 5), LEAF_SH)

    # ---- leaves (flat planes) ----
    pal = {"d": LEAF_DEEP, "s": LEAF_SH, "l": LEAF, "h": LEAF_HI, "t": LEAF_TIP}
    # left leaf 5x3: top face at (43,8), bottom at (48,8); column 0 is the tip (x=-5)
    left_top = [".thhl",
                "hllss",
                ".llll"]
    left_bottom = [".slls",
                   "sssdd",
                   ".ssss"]
    rows(img, 43, 8, left_top, pal)
    rows(img, 48, 8, left_bottom, pal)
    # right leaf 6x4: top face at (44,12), bottom at (50,12); column 0 is the base at the stem
    right_top = [".lhht.",
                 "sslhht",
                 "slsslh",
                 ".llll."]
    right_bottom = [".sssl.",
                    "ddsssl",
                    "dsddss",
                    ".ssss."]
    rows(img, 44, 12, right_top, pal)
    rows(img, 50, 12, right_bottom, pal)
    return img


def draw_icon():
    """16x16 overlay for the seed packet, same footprint as the original plant icons."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    pal = {"d": LEAF_DEEP, "s": LEAF_SH, "l": LEAF, "h": LEAF_HI, "t": LEAF_TIP,
           "R": RIM_HI, "r": RIM, "P": POT_HI, "p": POT, "q": POT_SH, "e": EYE, "S": SOIL}
    icon = [".......hh",
            ".lh...lht",
            "lls..ssl.",
            ".sls.s...",
            "....s....",
            ".RRRRRRR.",
            ".rrrrrrr.",
            "..PpppPp.",
            "..pepeqp.",
            "..ppppqq.",
            "...qqqq..",
            ]
    rows(img, 4, 3, icon, pal)
    return img


def front_view(body):
    """Flat front view of the pot (no leaves), for the preview only."""
    v = Image.new("RGBA", (10, 10), CLEAR)
    v.paste(body.crop((10, 24, 20, 26)), (0, 0))   # rim north
    v.paste(body.crop((8, 8, 16, 14)), (1, 2))     # walls north (face)
    v.paste(body.crop((38, 6, 44, 8)), (2, 8))     # base north
    return v


def main():
    os.makedirs(ENTITY_DIR, exist_ok=True)
    awake, asleep = draw_body(False), draw_body(True)
    awake.save(os.path.join(ENTITY_DIR, "potling.png"))
    asleep.save(os.path.join(ENTITY_DIR, "potling_sleep.png"))
    icon = draw_icon()
    icon.save(ICON_PATH)

    if len(sys.argv) > 1:
        k = 8
        sheet = Image.new("RGBA", (64 * k * 2 + 3 * 16 * k // 2, 64 * k), c("3c3c46"))
        for i, im in enumerate((awake, asleep)):
            sheet.alpha_composite(im.resize((64 * k, 64 * k), Image.NEAREST), (i * 64 * k, 0))
        sheet.alpha_composite(front_view(awake).resize((10 * k * 2, 10 * k * 2), Image.NEAREST), (128 * k + 8, 8))
        sheet.alpha_composite(front_view(asleep).resize((10 * k * 2, 10 * k * 2), Image.NEAREST), (128 * k + 8, 8 + 22 * k))
        sheet.alpha_composite(icon.resize((16 * k, 16 * k), Image.NEAREST), (128 * k + 8, 46 * k))
        sheet.save(os.path.join(sys.argv[1], "potling_preview.png"))


if __name__ == "__main__":
    main()
