#!/usr/bin/env python3
"""Renders an item of the mod for the docs: its 3D JSON model (the one worn or held), or its
inventory icon scaled up as pixel art.

Usage (needs Pillow and numpy), from the repository root:
    python3 tools/docs/render_item.py <item> [--out docs/images/items/<item>.png] [--size 512]
                                             [--yaw 30] [--pitch 22]
    python3 tools/docs/render_item.py --all      # every item listed in ITEMS

3D models use the Minecraft block/item model format (elements from/to in 0..16, y up, optional
element rotation, faces with uv in 0..16 and a texture reference). Textures come from the mod's
assets or, for minecraft:* ids, from the game jar that ModDevGradle unpacks into
build/moddev/artifacts. Faces are drawn in an orthographic 3/4 view with simple shading.
"""
import argparse
import glob
import io
import json
import math
import os
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ASSETS = "src/main/resources/assets"
JAR = next(iter(sorted(glob.glob("build/moddev/artifacts/minecraft-patched-*-merged.jar"))), None)

# item -> how to draw it: ("model", [model ids], yaw) or ("icon", texture id)
ITEMS = {
    "cone": ("model", ["sproutsquad:block/cone_helmet"], 30),
    "bucket": ("model", ["sproutsquad:block/bucket_helmet"], 30),
    "football_helmet": ("model", ["sproutsquad:block/football_helmet"], 150),
    "party_hat": ("model", ["sproutsquad:block/party_hat"], 30),
    "plant_pot_helmet": ("model", ["sproutsquad:block/plant_pot_helmet"], 30),
    "coffin": ("model", ["sproutsquad:block/coffin"], 30),
    "ducky_tube": ("model", ["sproutsquad:block/ducky_tube"], 210),
    "roller_skates": ("model", ["sproutsquad:block/roller_skate_left", "sproutsquad:block/roller_skate_right"], 30),
    "newspaper": ("model", ["sproutsquad:item/newspaper/newspaper"], 30),
    # the blaster's icon is two layers: the body and the dye inside, tinted by the loaded dye
    "dye_blaster": ("icon", ["sproutsquad:item/dye_blaster_empty", ("sproutsquad:item/dye_blaster_liquid", (199, 78, 189))]),
    "baron_staff": ("icon", "sproutsquad:item/baron_staff"),
    "taco": ("icon", "sproutsquad:item/taco"),
    "door_shield": ("door_shield", None, 30),
}


# ---------------------------------------------------------------- resources

def split_id(resource, default_ns="minecraft"):
    ns, _, path = resource.partition(":") if ":" in resource else (default_ns, "", resource)
    return ns, path


def read_asset(ns, path):
    local = os.path.join(ASSETS, ns, path)
    if os.path.exists(local):
        return open(local, "rb").read()
    if JAR:
        with zipfile.ZipFile(JAR) as z:
            name = f"assets/{ns}/{path}"
            if name in z.namelist():
                return z.read(name)
    sys.exit(f"missing asset {ns}:{path}")


def load_texture(texture_id):
    ns, path = split_id(texture_id)
    return Image.open(io.BytesIO(read_asset(ns, f"textures/{path}.png"))).convert("RGBA")


def load_model(model_id):
    """A model with its parents merged (textures and elements)."""
    ns, path = split_id(model_id)
    model = json.loads(read_asset(ns, f"models/{path}.json"))
    if "parent" in model and "elements" not in model:
        parent = load_model(model["parent"])
        parent.get("textures", {}).update(model.get("textures", {}))
        model = {**parent, **model, "textures": parent.get("textures", {}), "elements": parent.get("elements", [])}
    return model


def resolve(textures, ref):
    seen = 0
    while ref.startswith("#") and seen < 8:
        ref = textures.get(ref[1:], "")
        seen += 1
    return ref


# ---------------------------------------------------------------- geometry

def rotation_matrix(axis, degrees):
    a = math.radians(degrees)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def face_corners(name, x0, y0, z0, x1, y1, z1):
    """Texture top-left, top-right and bottom-left corners of a face, seen from outside."""
    return {
        "north": ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        "south": ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        "west": ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        "east": ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        "up": ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        "down": ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[name]


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def model_faces(model, offset=(0, 0, 0)):
    textures = model.get("textures", {})
    images = {}
    faces = []
    for element in model.get("elements", []):
        (x0, y0, z0), (x1, y1, z1) = element["from"], element["to"]
        rot = element.get("rotation")
        matrix, origin = np.eye(3), np.zeros(3)
        if rot and rot.get("angle"):
            matrix = rotation_matrix(rot["axis"], rot["angle"])
            origin = np.array(rot.get("origin", [8, 8, 8]), dtype=float)
        flat = min(x1 - x0, y1 - y0, z1 - z0) == 0
        for name, face in element.get("faces", {}).items():
            ref = resolve(textures, face.get("texture", "#0"))
            if not ref:
                continue
            if ref not in images:
                images[ref] = load_texture(ref)
            tl, tr, bl = (np.array(p, dtype=float) for p in face_corners(name, x0, y0, z0, x1, y1, z1))
            br = tr + bl - tl
            corners = [tl, tr, br, bl]
            k = (face.get("rotation", 0) // 90) % 4
            tl, tr, bl = corners[k], corners[(k + 1) % 4], corners[(k + 3) % 4]
            pts = [matrix @ (p - origin) + origin + np.array(offset) for p in (tl, tr, bl)]
            normal = matrix @ np.array(NORMALS[name], dtype=float)
            uv = face.get("uv", [0, 0, 16, 16])
            faces.append((pts, images[ref], uv, normal, flat))
    return faces


# ---------------------------------------------------------------- drawing

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def draw(faces, out, size, yaw, pitch):
    view = rot_x(math.radians(pitch)) @ rot_y(math.radians(-yaw))
    light = np.array([-0.35, 1.0, 0.6])
    light /= np.linalg.norm(light)
    items = []
    for pts, image, uv, normal, flat in faces:
        cam = [view @ (p - 8) for p in pts]
        n = view @ normal
        if n[2] < 0:
            if not flat:
                continue
            normal = -normal
        o, s_end, t_end = cam
        a, b = s_end - o, t_end - o
        depth = float((o + a / 2 + b / 2)[2])
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(normal, light)))
        items.append((depth, o, a, b, image, uv, shade))
    items.sort(key=lambda f: f[0])
    xs = [p[0] for f in items for p in (f[1], f[1] + f[2], f[1] + f[3], f[1] + f[2] + f[3])]
    ys = [p[1] for f in items for p in (f[1], f[1] + f[2], f[1] + f[3], f[1] + f[2] + f[3])]
    min_x, max_x, min_y, max_y = min(xs), max(xs), min(ys), max(ys)
    supersample = 3
    px = size * supersample
    scale = px * 0.84 / max(max_x - min_x, max_y - min_y)

    def screen(p):
        return np.array([(p[0] - (min_x + max_x) / 2) * scale + px / 2, -(p[1] - (min_y + max_y) / 2) * scale + px / 2])

    canvas = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    ground = max(screen(f[1] + v)[1] for f in items for v in (np.zeros(3), f[2], f[3], f[2] + f[3]))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([px * 0.26, ground - px * 0.03, px * 0.74, ground + px * 0.03], fill=(0, 0, 0, 60))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(px * 0.012)))

    for depth, o, a, b, image, (u1, v1, u2, v2), shade in items:
        so = screen(o)
        sa, sb = screen(o + a) - so, screen(o + b) - so
        det = sa[0] * sb[1] - sa[1] * sb[0]
        if abs(det) < 1e-6:
            continue
        w, h = image.size
        # uv are in 0..16 over the whole texture; sample strictly inside the face's rect
        tu1, tv1, tu2, tv2 = (u1 / 16 * w, v1 / 16 * h, u2 / 16 * w, v2 / 16 * h)
        eps_u = 0.02 * (1 if tu2 >= tu1 else -1)
        eps_v = 0.02 * (1 if tv2 >= tv1 else -1)
        tu1, tu2, tv1, tv2 = tu1 + eps_u, tu2 - eps_u, tv1 + eps_v, tv2 - eps_v
        inv = np.array([[sb[1], -sb[0]], [-sa[1], sa[0]]]) / det
        a11, a12 = inv[0] * (tu2 - tu1)
        a21, a22 = inv[1] * (tv2 - tv1)
        c1 = tu1 - (a11 * so[0] + a12 * so[1])
        c2 = tv1 - (a21 * so[0] + a22 * so[1])
        warped = image.transform(canvas.size, Image.AFFINE, (a11, a12, c1, a21, a22, c2), resample=Image.NEAREST)
        mask = Image.new("L", canvas.size, 0)
        ImageDraw.Draw(mask).polygon([tuple(so), tuple(so + sa), tuple(so + sa + sb), tuple(so + sb)], fill=255)
        r, g, bl, al = warped.split()
        r, g, bl = (ch.point(lambda x: int(x * shade)) for ch in (r, g, bl))
        alpha = Image.composite(al, Image.new("L", canvas.size, 0), mask)
        canvas.alpha_composite(Image.merge("RGBA", (r, g, bl, alpha)))
    save(canvas.resize((size, size), Image.LANCZOS), out)


def draw_icon(layers, out, size):
    """An item/generated icon: one texture, or layers (texture id or (texture id, tint rgb))."""
    icon = None
    for layer in (layers if isinstance(layers, list) else [layers]):
        texture_id, tint = layer if isinstance(layer, tuple) else (layer, None)
        image = load_texture(texture_id)
        if tint:
            arr = np.asarray(image, dtype=np.float32)
            arr[..., :3] *= np.array(tint, dtype=np.float32) / 255.0
            image = Image.fromarray(arr.astype(np.uint8), "RGBA")
        icon = image if icon is None else Image.alpha_composite(icon, image)
    box = icon.getbbox() or (0, 0, *icon.size)
    icon = icon.crop(box)
    k = max(1, int(size * 0.8 / max(icon.size)))
    big = icon.resize((icon.width * k, icon.height * k), Image.NEAREST)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(big, ((size - big.width) // 2, (size - big.height) // 2))
    save(canvas, out)


def door_shield_model():
    """Approximation of the Door Shield: the game builds it from the door's own block model
    (DoorShieldItemModel); here an oak door, two blocks tall, on a thin board."""
    def face(u1, v1, u2, v2, tex):
        return {"uv": [u1, v1, u2, v2], "texture": tex}
    side = face(0, 0, 3, 16, "#bottom")
    return {
        "textures": {"top": "minecraft:block/oak_door_top", "bottom": "minecraft:block/oak_door_bottom"},
        "elements": [
            {"from": [0, 0, 6.5], "to": [16, 16, 9.5], "faces": {
                "north": face(16, 0, 0, 16, "#bottom"), "south": face(0, 0, 16, 16, "#bottom"),
                "west": side, "east": side, "down": face(0, 13, 16, 16, "#bottom")}},
            {"from": [0, 16, 6.5], "to": [16, 32, 9.5], "faces": {
                "north": face(16, 0, 0, 16, "#top"), "south": face(0, 0, 16, 16, "#top"),
                "west": face(0, 0, 3, 16, "#top"), "east": face(0, 0, 3, 16, "#top"), "up": face(0, 0, 16, 3, "#top")}},
        ],
    }


def save(image, out):
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    image.save(out, optimize=True)
    print(out, image.size)


def render(item, out, size, yaw=None, pitch=22.0):
    kind, what, *rest = ITEMS[item]
    yaw = yaw if yaw is not None else (rest[0] if rest else 30)
    if kind == "icon":
        draw_icon(what, out, size)
        return
    if kind == "door_shield":
        draw(model_faces(door_shield_model()), out, size, yaw, pitch)
        return
    faces = []
    for i, model_id in enumerate(what):  # several models side by side (a pair of roller skates)
        offset = ((i - (len(what) - 1) / 2) * 8, 0, 0)
        faces += model_faces(load_model(model_id), offset)
    draw(faces, out, size, yaw, pitch)


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("item", nargs="?")
    ap.add_argument("--all", action="store_true")
    ap.add_argument("--out")
    ap.add_argument("--size", type=int, default=512)
    ap.add_argument("--yaw", type=float)
    ap.add_argument("--pitch", type=float, default=22.0)
    args = ap.parse_args()
    for item in (ITEMS if args.all else [args.item]):
        if item not in ITEMS:
            sys.exit(f"unknown item {item}; known: {', '.join(ITEMS)}")
        render(item, args.out if (args.out and not args.all) else f"docs/images/items/{item}.png", args.size, args.yaw, args.pitch)


if __name__ == "__main__":
    main()
