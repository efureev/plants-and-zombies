#!/usr/bin/env python3
"""Textures for the Double Sunflower, a Sunflower with two stems and a flower on each.

Usage (needs Pillow), from the repository root:
    python3 art/double_sunflower/draw_double_sunflower.py [preview_dir]

Reads the Sunflower's textures (textures/entity/plant/sunflower/sunflower{,_sleep,_glow}.png) and writes
(64x64 unless noted):
    src/main/resources/assets/sproutsquad/textures/entity/plant/double_sunflower/double_sunflower.png
    .../double_sunflower_sleep.png   both heads with closed eyes
    .../double_sunflower_glow.png    the glow before the sun (drawn full-bright, faded in by the game)
    .../double_sunflower_baby_glow.png  the same for a baby: the game looks up <id>_baby_glow first and would
                                        otherwise fall back to the plain texture, lighting up the whole plant
    .../double_sunflower_baby_sleep.png the sleep sheet for a baby: <id>_baby_sleep, then <id>_baby, then the
                                        awake texture, so without it a sleeping or dying baby kept its eyes open
    src/main/resources/assets/sproutsquad/textures/item/plant_icon/double_sunflower.png  (16x16)
and, if preview_dir is given, double_sunflower_preview.png there (the three sheets, x8).

The tall head uses the Sunflower's own head block (UV 0,0). The short head is a copy of that block at
(0, 50) with its own face: a wink, a wide grin and blushing cheeks (DoubleSunflowerModel.createBodyLayer).
Everything else (petals, stems, leaves) is the Sunflower's layout. Style: docs/en/WORKSHOP.md.
"""
import os
import sys
from PIL import Image

ROOT = os.path.join("src", "main", "resources", "assets", "sproutsquad", "textures")
SUNFLOWER_DIR = os.path.join(ROOT, "entity", "plant", "sunflower")
ENTITY_DIR = os.path.join(ROOT, "entity", "plant", "double_sunflower")
ICON_PATH = os.path.join(ROOT, "item", "plant_icon", "double_sunflower.png")

HEAD_BLOCK = (0, 0, 28, 11)    # box UV of the 12x9x2 head: 2+12+2+12 wide, 2+9 tall
SECOND_HEAD = (0, 50)          # free space of the Sunflower's texture


def c(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# the Sunflower's own face colours
DARK, BROWN, ORANGE, ORANGE_SH, WHITE = c("3c3232"), c("713d0f"), c("d96c1c"), c("cb6215"), c("ffffff")
BLUSH = c("f05b2d")            # the Sunflower's tongue red, as cheeks

def draw_face(img, ox, oy, sleep, glow_of=None):
    """The short head's face, drawn on the head block whose top-left corner is (ox, oy)."""
    def p(x, y, col):
        img.putpixel((ox + x, oy + y), glow_of(col) if glow_of else col)

    if sleep:
        # asleep: the Sunflower's closed eyes and mouth as they are, only the cheeks tell the heads apart
        p(3, 7, BLUSH); p(12, 7, BLUSH)
        return
    # clear the copied Sunflower face back to the orange disc (its two-tone checker)
    for y in range(3, 10):
        for x in range(3, 13):
            p(x, y, ORANGE if (x + y) % 3 else ORANGE_SH)
    # the left eye is the Sunflower's (2x3 with a shine), the right one winks (an arc)
    for y in (3, 4, 5):
        for x in (4, 5):
            p(x, y, DARK)
    p(4, 3, WHITE)
    p(9, 5, BROWN); p(10, 4, DARK); p(11, 4, DARK); p(12, 5, BROWN)
    # wide grin with a tongue, blushing cheeks
    p(4, 7, BROWN); p(11, 7, BROWN)
    for x in range(5, 11):
        p(x, 8, DARK)
    p(6, 9, BROWN); p(9, 9, BROWN)
    p(7, 9, BLUSH); p(8, 9, BLUSH)
    p(3, 7, BLUSH); p(12, 7, BLUSH)


def glow_palette(base, glow):
    """Maps each colour of the Sunflower's head to its colour on the glow sheet (same pixels)."""
    mapping = {}
    for y in range(HEAD_BLOCK[1], HEAD_BLOCK[3]):
        for x in range(HEAD_BLOCK[0], HEAD_BLOCK[2]):
            b, g = base.getpixel((x, y)), glow.getpixel((x, y))
            if b[3] and g[3]:
                mapping.setdefault(b, g)

    def to_glow(col):
        if col in mapping:
            return mapping[col]
        r, g, b, a = col  # a colour the Sunflower has no glow for: brighten it
        return (min(255, r + 70), min(255, g + 70), min(255, b + 40), a)
    return to_glow


def sheet(name, sleep=False, glow=False):
    img = Image.open(os.path.join(SUNFLOWER_DIR, name)).convert("RGBA")
    img.paste(img.crop(HEAD_BLOCK), SECOND_HEAD)
    to_glow = None
    if glow:
        base = Image.open(os.path.join(SUNFLOWER_DIR, "sunflower.png")).convert("RGBA")
        to_glow = glow_palette(base, img)
    draw_face(img, SECOND_HEAD[0], SECOND_HEAD[1], sleep, to_glow)
    return img


# Two little Sunflower heads on their stems: the tall one behind on the right, the short one in front.
ICON = [
    "................",
    "................",
    ".........abab...",
    "........bcdcab..",
    "........accddb..",
    "...abab.bcdcda..",
    "..bcdcab.aeab...",
    "..accddb..e.....",
    "..bcdcda..e.....",
    "...aeab..e......",
    ".....e..e.......",
    "......ee........",
    ".......e........",
    "................",
    "................",
    "................",
]
ICON_COLORS = {"a": c("f6cf37"), "b": c("ecb427"), "c": c("cb6215"), "d": c("b1540c"), "e": c("7bbf2d")}


def icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ICON):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), ICON_COLORS[ch])
    return img


def main():
    os.makedirs(ENTITY_DIR, exist_ok=True)
    sheets = {
        "double_sunflower.png": sheet("sunflower.png"),
        "double_sunflower_sleep.png": sheet("sunflower_sleep.png", sleep=True),
        "double_sunflower_glow.png": sheet("sunflower_glow.png", glow=True),
    }
    for name, img in sheets.items():
        img.save(os.path.join(ENTITY_DIR, name))
    sheets["double_sunflower_glow.png"].save(os.path.join(ENTITY_DIR, "double_sunflower_baby_glow.png"))
    sheets["double_sunflower_sleep.png"].save(os.path.join(ENTITY_DIR, "double_sunflower_baby_sleep.png"))
    icon().save(ICON_PATH)
    if len(sys.argv) > 1:
        preview = Image.new("RGBA", (64 * 3 * 8 + 16, 64 * 8), (60, 40, 60, 255))
        for i, img in enumerate(sheets.values()):
            preview.alpha_composite(img.resize((512, 512), Image.NEAREST), (i * (512 + 8), 0))
        os.makedirs(sys.argv[1], exist_ok=True)
        preview.save(os.path.join(sys.argv[1], "double_sunflower_preview.png"))
    print("wrote", ENTITY_DIR, ICON_PATH)


if __name__ == "__main__":
    main()
