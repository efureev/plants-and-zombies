#!/usr/bin/env python3
"""Textures for the Poppy Rattle, a poppy seed head that grows random seed packets.

Usage (needs Pillow), from the repository root:
    python3 art/poppy_rattle/draw_poppy_rattle.py [preview_dir]

Writes (64x64 unless noted):
    src/main/resources/assets/sproutsquad/textures/entity/plant/poppy_rattle/poppy_rattle.png
    .../poppy_rattle_sleep.png            closed eyes (used while dying: it never sleeps)
    .../poppy_rattle_night.png            glowing eyes at night
    .../poppy_rattle_night_emissive.png   only the eyes, drawn full-bright by the game
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/poppy_rattle.png  (16x16)
and, if preview_dir is given, poppy_rattle_preview.png there (the UV sheets, x8).

The UV layout follows PoppyRattleModel.createBodyLayer() (box UV). Style: the style guide in
docs/en/WORKSHOP.md — light from above, no black outlines, eyes #111f0a with a 1px shine.
"""
import math
import os
import sys
from PIL import Image

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
ENTITY_DIR = os.path.join(ROOT, "entity", "plant", "poppy_rattle")
ICON_PATH = os.path.join(ROOT, "item", "plant_icon", "poppy_rattle.png")


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


CLEAR = (0, 0, 0, 0)
# seed head: mushroom-stem greens of the palette, one own shadow, a Peashooter green for the deepest row
POD_DEEP, POD_SH, POD, POD_LI, POD_HI = c("4a7e26"), c("6e9663"), c("9cb989"), c("afca9d"), c("c7e0b6")
# crown lid: the palette's soil browns
LID_DEEP, LID, LID_LI = c("3f2a1b"), c("5a3d28"), c("70503a")
# petals: own reds, the blush as highlight
RED_SH, RED, RED_HI = c("9b2220"), c("d0342c"), c("e88d7a")
# stem and leaves: Peashooter greens
LEAF_DEEP, LEAF_SH, LEAF, LEAF_HI, LEAF_TIP = c("3f6d1f"), c("4a7e26"), c("79b930"), c("98e244"), c("bdf77c")
EYE, SHINE, BLUSH, MOUTH = c("111f0a"), c("ffffff"), c("e88d7a"), c("4a2219")
GLOW = c("f6cf37")  # night eyes (the sun palette)
# seed packet paper; the "?" is the petal red
PAPER_SH, PAPER, PAPER_HI = c("d8c9a0"), c("f2e8c8"), c("fffbe9")

FACE_X, FACE_Y = 8, 8          # front face of the 8x7x8 body
EYES = (1, 5)                  # eye columns on the face, 2x2 at rows 2-3


def fill(img, x, y, w, h, col):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            img.putpixel((xx, yy), col)


def rows(img, x, y, pattern, pal):
    """Paint a small pixel pattern: each string is a row, each char a palette key ('.' = skip)."""
    for dy, line in enumerate(pattern):
        for dx, ch in enumerate(line):
            if ch != ".":
                img.putpixel((x + dx, y + dy), pal[ch])


def pod_side(img, x, y, w, h, ridges=True):
    """A wall of the seed head: lighter top row, darker bottom row, faint vertical ridges."""
    fill(img, x, y, w, h, POD)
    fill(img, x, y, w, 1, POD_LI)
    fill(img, x, y + h - 1, w, 1, POD_SH)
    if ridges:
        for rx in range(1, w, 3):
            fill(img, x + rx, y + 1, 1, h - 2, POD_SH if rx % 2 else POD_LI)


def draw_body(eyes="open"):
    """eyes: "open" (day), "glow" (night), "closed" (dying)."""
    img = Image.new("RGBA", (64, 64), CLEAR)

    # ---- body 8x7x8 at (0,0): sides on row 8, the face on the front ----
    fill(img, 8, 0, 8, 8, POD_LI)
    fill(img, 16, 0, 8, 8, POD_SH)
    for x in (0, 16, 24):
        pod_side(img, x, 8, 8, 7)
    pod_side(img, FACE_X, FACE_Y, 8, 7, ridges=False)
    img.putpixel((FACE_X, FACE_Y + 1), POD_HI)               # shine on the "forehead"
    for ex in EYES:
        if eyes == "closed":
            fill(img, FACE_X + ex, FACE_Y + 3, 2, 1, EYE)
        else:
            fill(img, FACE_X + ex, FACE_Y + 2, 2, 2, GLOW if eyes == "glow" else EYE)
            img.putpixel((FACE_X + ex, FACE_Y + 2), SHINE)
    img.putpixel((FACE_X, FACE_Y + 4), BLUSH)
    img.putpixel((FACE_X + 7, FACE_Y + 4), BLUSH)
    fill(img, FACE_X + 3, FACE_Y + 5, 2, 1, MOUTH)

    # ---- base 6x1x6 at (32,0) ----
    fill(img, 38, 0, 6, 6, POD_SH)
    fill(img, 44, 0, 6, 6, POD_DEEP)
    fill(img, 32, 6, 24, 1, POD_SH)

    # ---- shoulder 6x1x6 at (0,40): narrows under the crown ----
    fill(img, 6, 40, 6, 6, POD_LI)
    fill(img, 12, 40, 6, 6, POD_SH)
    for i in range(24):
        img.putpixel((i, 46), POD_HI if i % 3 == 0 else POD_LI)

    # ---- neck 4x1x4 at (32,8): the ring of pores the seeds fall out of ----
    fill(img, 36, 8, 4, 4, POD_SH)
    fill(img, 40, 8, 4, 4, POD_SH)
    for i in range(16):
        img.putpixel((32 + i, 12), LID_DEEP if i % 2 == 0 else POD)

    # ---- lid 7x1x7 at (0,16): a round crown with rays, the stigma disc of a poppy ----
    for yy in range(7):
        for xx in range(7):
            dx, dy = xx - 3, yy - 3
            r = math.hypot(dx, dy)
            if r > 3.9:
                continue                                       # round corners
            ray = (math.degrees(math.atan2(dy, dx)) + 360) % 45
            ray = ray < 9 or ray > 36
            col = LID_LI if (ray and r > 1.0) else LID
            if r > 3.1 and not ray:
                col = LID_DEEP
            img.putpixel((7 + xx, 16 + yy), col)
    fill(img, 14, 16, 7, 7, LID_DEEP)                          # underside
    for i in range(28):
        img.putpixel((i, 23), LID_LI if i % 3 == 1 else LID_DEEP)

    # ---- nub 2x1x2 at (48,8) ----
    fill(img, 50, 8, 2, 2, LID_LI)
    fill(img, 52, 8, 2, 2, LID_DEEP)
    fill(img, 48, 10, 8, 1, LID)

    # ---- stem 2x3x2 at (56,8) ----
    fill(img, 58, 8, 2, 2, LEAF)
    fill(img, 60, 8, 2, 2, LEAF_SH)
    for i, col in enumerate((LEAF, LEAF_HI, LEAF_HI, LEAF, LEAF, LEAF_SH, LEAF_SH, LEAF)):
        fill(img, 56 + i, 10, 1, 3, col)

    # ---- seed packet 4x6x1 at (40,16): paper with a green band and a red "?" ----
    fill(img, 41, 16, 4, 1, PAPER_HI)
    fill(img, 45, 16, 4, 1, PAPER_SH)
    fill(img, 40, 17, 1, 6, PAPER_SH)
    fill(img, 45, 17, 1, 6, PAPER_SH)
    rows(img, 41, 17, ["BBBB",
                       "pIIp",
                       "ppIp",
                       "pIhp",
                       "phhp",
                       "pIhp"], {"h": PAPER_HI, "p": PAPER, "I": RED, "B": LEAF})
    fill(img, 46, 17, 4, 6, PAPER_SH)

    # ---- petal plane 5x0x5 at (0,27): row 0 at the head, row 4 the tip; a black blotch at the base ----
    pal = {"r": RED, "R": RED_HI, "s": RED_SH, "d": LID_DEEP}
    rows(img, 5, 27, ["..d..",
                      ".rdr.",
                      "rrrRr",
                      "rRRRr",
                      ".rrr."], pal)
    rows(img, 10, 27, ["..d..",
                       ".sds.",
                       "ssrss",
                       "srrrs",
                       ".sss."], pal)

    # ---- leaves 7x0x4: serrated poppy leaves; the right one's tip at u=6, the left one's at u=0 ----
    lp = {"d": LEAF_DEEP, "s": LEAF_SH, "l": LEAF, "h": LEAF_HI, "t": LEAF_TIP}
    rows(img, 4, 33, [".l.l.h.", "slllhht", "sllhht.", ".s.l..."], lp)
    rows(img, 11, 33, [".d.s.s.", "dsssssl", "dssssl.", ".d.s..."], lp)
    rows(img, 26, 33, [".h.l.l.", "thhllls", ".thhlls", "...l.s."], lp)
    rows(img, 33, 33, [".s.s.d.", "lsssssd", ".lssssd", "...s.d."], lp)
    return img


def draw_emissive(night):
    """Only the night eyes: the layer the game draws full-bright."""
    img = Image.new("RGBA", (64, 64), CLEAR)
    for ex in EYES:
        for dx in range(2):
            for dy in range(2):
                p = (FACE_X + ex + dx, FACE_Y + 2 + dy)
                img.putpixel(p, night.getpixel(p))
    return img


def draw_icon():
    """16x16 overlay for the seed packet: the seed head with its crown and red petals."""
    img = Image.new("RGBA", (16, 16), CLEAR)
    pal = {"D": LID_DEEP, "L": LID, "l": LID_LI, "n": POD_SH, "h": POD_HI, "a": POD_LI, "p": POD,
           "s": POD_SH, "e": EYE, "w": SHINE, "r": RED, "R": RED_HI, "q": RED_SH, "g": LEAF, "G": LEAF_SH}
    icon = ["...lLl...",
            ".DLlLlLD.",
            "...nnn...",
            "..aaaaa..",
            ".hppppps.",
            ".pwepwes.",
            ".peepees.",
            ".ppppppss",
            "rRpp.ppsq",
            "rrrssssqq",
            ".rq.g.qq.",
            "....G....",
            ]
    rows(img, 3, 2, icon, pal)
    img.putpixel((7, 10), MOUTH)
    return img


def main():
    os.makedirs(ENTITY_DIR, exist_ok=True)
    day, asleep, night = draw_body("open"), draw_body("closed"), draw_body("glow")
    day.save(os.path.join(ENTITY_DIR, "poppy_rattle.png"))
    asleep.save(os.path.join(ENTITY_DIR, "poppy_rattle_sleep.png"))
    night.save(os.path.join(ENTITY_DIR, "poppy_rattle_night.png"))
    draw_emissive(night).save(os.path.join(ENTITY_DIR, "poppy_rattle_night_emissive.png"))
    icon = draw_icon()
    icon.save(ICON_PATH)

    if len(sys.argv) > 1:
        k = 8
        sheet = Image.new("RGBA", (64 * k * 3 + 16 * k + 16, 64 * k), c("3c3c46"))
        for i, im in enumerate((day, night, asleep)):
            sheet.alpha_composite(im.resize((64 * k, 64 * k), Image.NEAREST), (i * 64 * k, 0))
        sheet.alpha_composite(icon.resize((16 * k, 16 * k), Image.NEAREST), (192 * k + 8, 8))
        sheet.save(os.path.join(sys.argv[1], "poppy_rattle_preview.png"))


if __name__ == "__main__":
    main()
