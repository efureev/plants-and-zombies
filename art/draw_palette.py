#!/usr/bin/env python3
"""Sprout Squad palette (style guide in docs/en/WORKSHOP.md) as art/palette.png.

Usage (needs Pillow), from the repository root:
    python3 art/draw_palette.py

One row per material, 8x8 swatches, darkest tone on the left. Aseprite, Blockbench and most
pixel editors can load the PNG as a palette. Draw new characters with these colours plus at
most three of their own.
"""
from PIL import Image

SWATCH = 8


def ramp(a, b, steps):
    """Tones between two endpoints (zombie materials are given as ranges in the guide)."""
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return ["%02x%02x%02x" % tuple(round(ca[k] + (cb[k] - ca[k]) * t / (steps - 1)) for k in range(3))
            for t in range(steps)]


ROWS = [
    # plants: Peashooter greens
    ["2d5213", "3f6d1f", "4a7e26", "79b930", "84c738", "98e244", "a7ec58", "bdf77c"],
    # sun: orange shadows, yellow body and light
    ["cb6215", "d96c1c", "ecb427", "f6cf37", "f7e64d"],
    # mushroom stems
    ["9cb989", "afca9d", "c7e0b6"],
    # terracotta pot (mascot) and soil
    ["6e3528", "8c4838", "b4654f", "c97758", "cf7c60", "e3967a", "3f2a1b", "5a3d28", "70503a"],
    # plant faces: eyes, shine, blush, mouth
    ["111f0a", "ffffff", "e88d7a", "4a2219"],
    # zombies: skin, jacket, trousers (Browncoat)
    ramp("2f5434", "567157", 4) + ramp("2f2110", "47351e", 4),
    ramp("2e2f4e", "3a3c63", 3),
]


def main():
    width = max(len(r) for r in ROWS) * SWATCH
    img = Image.new("RGBA", (width, len(ROWS) * SWATCH), (0, 0, 0, 0))
    for y, row in enumerate(ROWS):
        for x, hexcol in enumerate(row):
            col = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
            for dy in range(SWATCH):
                for dx in range(SWATCH):
                    img.putpixel((x * SWATCH + dx, y * SWATCH + dy), col)
    img.save("art/palette.png")
    print("art/palette.png", img.size, sum(len(r) for r in ROWS), "colours")


if __name__ == "__main__":
    main()
