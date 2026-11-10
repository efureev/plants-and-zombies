#!/usr/bin/env python3
"""Mod icon for Sprout Squad: the pot mascot in a golden frame.

Usage (needs Pillow): python3 art/logo/draw_icon.py src/main/resources/sprout_squad_icon.png 8
Draws on a 32x32 pixel grid and scales up with nearest-neighbour (8 -> 256x256).
Also writes *_preview.png next to the output (on a dark and a light background).
"""
import sys
from PIL import Image, ImageDraw

OUT = sys.argv[1]
SCALE = int(sys.argv[2]) if len(sys.argv) > 2 else 8
N = 32


def c(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


K = c("2a1c14")                                   # outline
GOLD, GOLD_HI, GOLD_SH, GOLD_DEEP = c("f4c13a"), c("fff09a"), c("c9861a"), c("9a6410")
SKY_TOP, SKY_BOT = c("bfe9ff"), c("86cdfb")
SUN, SUN_HI, SUN_SH = c("ffe066"), c("fff7c2"), c("f2b53a")
GRASS, GRASS_HI, GRASS_SH, DIRT = c("6cc43c"), c("8edc55"), c("4f9a2c"), c("7a5235")
LEAF, LEAF_HI, LEAF_SH, STEM = c("5cb83a"), c("9be862"), c("3d8a26"), c("4a9a2e")
POT, POT_HI, POT_SH, RIM, RIM_HI, SOIL = c("b4654f"), c("c97758"), c("8c4838"), c("cf7c60"), c("e3967a"), c("5a3d28")
EYE, SHINE, BLUSH, MOUTH = c("1a1410"), c("ffffff"), c("e88d7a"), c("3a1c15")

img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# ---- golden frame -------------------------------------------------------
d.rounded_rectangle([0, 0, 31, 31], radius=5, fill=K)
d.rounded_rectangle([1, 1, 30, 30], radius=4, fill=GOLD)
d.line([(4, 1), (27, 1)], fill=GOLD_HI)            # top highlight
d.line([(1, 4), (1, 27)], fill=GOLD_HI)            # left highlight
d.point([(2, 2), (3, 2), (2, 3)], fill=GOLD_HI)
d.line([(4, 30), (27, 30)], fill=GOLD_SH)          # bottom shade
d.line([(30, 4), (30, 27)], fill=GOLD_SH)          # right shade
d.point([(29, 29), (28, 29), (29, 28)], fill=GOLD_SH)
d.rounded_rectangle([3, 3, 28, 28], radius=2, fill=GOLD_DEEP)   # inner bevel

# ---- scene --------------------------------------------------------------
for y in range(4, 28):                              # sky gradient
    t = (y - 4) / 23
    col = tuple(round(SKY_TOP[i] + (SKY_BOT[i] - SKY_TOP[i]) * t) for i in range(3)) + (255,)
    d.line([(4, y), (27, y)], fill=col)
d.point([(4, 4), (27, 4), (4, 27), (27, 27)], fill=GOLD_DEEP)  # round inner corners

# sun (top-left corner), with short rays
d.ellipse([5, 4, 10, 9], fill=SUN)
d.point([(6, 5), (7, 5), (6, 6)], fill=SUN_HI)
d.point([(9, 8), (10, 7), (9, 7)], fill=SUN_SH)
for p in ((12, 5), (12, 8), (11, 10), (4, 10)):
    d.point([p], fill=SUN_SH)

# ground
d.rectangle([4, 23, 27, 27], fill=GRASS)
d.line([(4, 23), (27, 23)], fill=GRASS_HI)
d.rectangle([4, 26, 27, 27], fill=DIRT)
d.point([(6, 25), (11, 24), (21, 25), (25, 24), (8, 26), (24, 26)], fill=GRASS_SH)
d.point([(4, 27), (27, 27)], fill=GOLD_DEEP)

# ---- mascot: sprout ------------------------------------------------------
# stem
d.rectangle([15, 9, 16, 16], fill=K)
d.line([(15, 10), (15, 16)], fill=STEM)
d.line([(16, 11), (16, 16)], fill=LEAF_SH)
# left leaf (smaller, pointing left under the sun)
d.polygon([(15, 13), (12, 9), (8, 9), (4, 11), (7, 14), (13, 15)], fill=K)
d.polygon([(14, 12), (11, 10), (8, 10), (6, 11), (8, 13), (13, 14)], fill=LEAF)
d.line([(14, 13), (10, 11), (7, 11)], fill=LEAF_SH)
d.point([(8, 10), (9, 10), (10, 10)], fill=LEAF_HI)
# right leaf (bigger, waving up)
d.polygon([(16, 12), (18, 8), (22, 5), (26, 4), (26, 6), (24, 10), (19, 13), (17, 14)], fill=K)
d.polygon([(17, 12), (19, 9), (22, 6), (25, 5), (23, 9), (19, 12)], fill=LEAF)
d.point([(23, 6), (24, 6), (21, 7), (20, 8)], fill=LEAF_HI)
d.point([(22, 10), (20, 11)], fill=LEAF_SH)
d.line([(18, 11), (21, 8), (24, 6)], fill=LEAF_SH)

# ---- mascot: pot -----------------------------------------------------------
d.rectangle([8, 15, 23, 19], fill=K)                 # rim outline
d.polygon([(9, 19), (22, 19), (21, 28), (10, 28)], fill=K)  # body outline
d.rectangle([9, 16, 22, 18], fill=RIM)
d.line([(9, 16), (22, 16)], fill=RIM_HI)
d.line([(11, 16), (20, 16)], fill=SOIL)             # soil inside the rim
d.line([(15, 16), (16, 16)], fill=STEM)              # stem enters the soil
d.polygon([(10, 19), (21, 19), (20, 27), (11, 27)], fill=POT)
d.line([(10, 19), (21, 19)], fill=POT_SH)            # under-rim shadow
d.line([(11, 20), (11, 26)], fill=POT_HI)
d.line([(20, 20), (19, 27)], fill=POT_SH)
# face
d.rectangle([12, 21, 13, 22], fill=EYE)
d.rectangle([18, 21, 19, 22], fill=EYE)
d.point([(12, 21), (18, 21)], fill=SHINE)
d.point([(11, 24), (20, 24)], fill=BLUSH)
d.line([(14, 24), (15, 25), (16, 25), (17, 24)], fill=MOUTH)

big = img.resize((N * SCALE, N * SCALE), Image.NEAREST)
big.save(OUT)
prev = Image.new("RGBA", (N * SCALE * 2 + 60, N * SCALE + 40), c("ffffff"))
prev.paste(Image.new("RGBA", (N * SCALE + 30, N * SCALE + 40), c("0d1117")), (N * SCALE + 30, 0))
prev.alpha_composite(big, (15, 20))
prev.alpha_composite(big, (N * SCALE + 45, 20))
small = img.resize((N * 2, N * 2), Image.NEAREST)    # how it looks at the mod-list size
prev.alpha_composite(small, (N * SCALE - 50, N * SCALE - 50))
prev.save(OUT.replace(".png", "_preview.png"))
print(big.size)
