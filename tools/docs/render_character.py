#!/usr/bin/env python3
"""Renders a character of the mod from its real model and texture, for the docs.

Usage (needs Pillow and numpy), from the repository root:
    python3 tools/docs/render_character.py <id> [--out docs/images/characters/<id>.png]
                                                [--yaw 30] [--pitch 22] [--size 512] [--pose idle]

<id> is the entity id without namespace (`sunflower`). The script finds the Blockbench-exported
model class whose LAYER_LOCATION is pazResource("<id>") (Java or Kotlin), the texture
textures/entity/<kind>/<id>/<id>.png (plus <id>_emissive.png, drawn full-bright), and the first
keyframe of the model's idle animation (the pose it stands in, game-like), then draws the boxes
in an orthographic 3/4 view with Minecraft's box UV layout and simple face shading.

Conventions mirrored from Minecraft: model space has y down and the root at y = 24; a part is
translate(offset) * rotationZYX(z, y, x) * scale; LivingEntityRenderer maps model (x, y, z) to
world (x, 24 - y, -z) in pixels, so the model's -z side (the face) looks at +z.
"""
import argparse
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SRC = "src/main"
NUM = r"-?\d+(?:\.\d+)?(?:[eE]-?\d+)?[fFdD]?"


def num(s):
    return float(s.strip().rstrip("fFdD"))


# ---------------------------------------------------------------- sources

def find_model(entity_id):
    pattern = re.compile(r'LAYER_LOCATION\s*=\s*(?:new\s+)?ModelLayerLocation\(\s*pazResource\("%s"\)' % re.escape(entity_id))
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.endswith((".java", ".kt")) and "Model" in f:
                path = os.path.join(root, f)
                text = open(path, encoding="utf-8").read()
                if pattern.search(text):
                    return path, text
    sys.exit(f"no model with LAYER_LOCATION pazResource(\"{entity_id}\")")


def find_animation(model_path, model_text):
    """The animation object the model bakes its idle animation from (e.g. SunflowerAnimation.idle)."""
    m = re.search(r"idleAnimation\s*=\s*(\w+)\.idle", model_text) or re.search(r"idle\s*=\s*(\w+)\.idle", model_text)
    if not m:
        return None
    name = m.group(1)
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.split(".")[0] == name and f.endswith((".java", ".kt")):
                return open(os.path.join(root, f), encoding="utf-8").read()
    # Kotlin: the animation object may live in the model file itself
    return model_text if re.search(r"object\s+%s" % name, model_text) else None


def find_texture(entity_id):
    for kind in ("plant", "zombie", "blueprint_machine", "gnome", ""):
        path = os.path.join(SRC, "resources/assets/sproutsquad/textures/entity", kind, entity_id, entity_id + ".png")
        if os.path.exists(path):
            return path
    sys.exit(f"no texture for {entity_id}")


# ---------------------------------------------------------------- model parsing

class Part:
    def __init__(self, name, parent):
        self.name, self.parent = name, parent
        self.offset = np.zeros(3)
        self.rotation = np.zeros(3)  # radians, x y z
        self.scale = np.ones(3)
        self.cubes = []              # (u, v, mirror, x, y, z, w, h, d, inflate)
        self.children = []


def parse_model(text):
    size = re.search(r"LayerDefinition\.create\(\s*\w+\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", text)
    tex_w, tex_h = (int(size.group(1)), int(size.group(2))) if size else (64, 64)
    root = Part("root", None)
    variables = {}
    for root_name in ("partdefinition", "root"):
        variables[root_name] = root
    # Kotlin: mesh.root / meshdefinition.getRoot()
    statement = re.compile(
        r"(?:(?:PartDefinition|val|var)\s+(\w+)\s*=\s*)?"
        r"([\w.()]+?)\.addOrReplaceChild\(\s*\"(\w+)\"\s*,\s*CubeListBuilder\.create\(\)(.*?)"
        r",\s*PartPose\.(ZERO|offset|offsetAndRotation)(?:\(([^()]*)\))?\s*\)",
        re.S)
    for m in statement.finditer(text):
        var, parent_expr, name, chain, pose_kind, pose_args = m.groups()
        parent_key = parent_expr.split(".")[0]
        parent = variables.get(parent_key) or (root if "root" in parent_expr.lower() else None)
        if parent is None:
            continue
        part = Part(name, parent)
        parent.children.append(part)
        if var:
            variables[var] = part
        if pose_args:
            values = [num(v) for v in pose_args.split(",")]
            part.offset = np.array(values[:3])
            if pose_kind == "offsetAndRotation":
                part.rotation = np.array(values[3:6])
        u = v = 0
        mirror = False
        for op in re.finditer(r"\.(texOffs|mirror|addBox)\(([^()]*(?:\([^()]*\))?[^()]*)\)", chain):
            kind, args = op.group(1), op.group(2)
            if kind == "texOffs":
                u, v = [int(num(a)) for a in args.split(",")[:2]]
            elif kind == "mirror":
                mirror = args.strip() not in ("false",)
            else:
                values = re.findall(NUM, re.sub(r"\"[^\"]*\"", "", args))
                inflate = 0.0
                deformation = re.search(r"CubeDeformation\(\s*(%s)" % NUM, args)
                if deformation:
                    inflate = num(deformation.group(1))
                box = [num(x) for x in values[:6]]
                part.cubes.append((u, v, mirror, *box, inflate))
    return root, (tex_w, tex_h)


def parse_pose(text, animation_name="idle"):
    """First keyframe per bone/channel of an AnimationDefinition (Java export or the Kotlin helpers)."""
    if text is None:
        return {}
    m = re.search(r"\b%s\b\s*(?::\s*AnimationDefinition)?\s*=\s*AnimationDefinition\.Builder(.*?)\.build\(\)" % animation_name, text, re.S)
    if not m:
        return {}
    body = m.group(1)
    pose = {}
    for ch in re.finditer(r"addAnimation\(\s*\"(\w+)\"\s*,\s*(?:new\s+)?AnimationChannel\(\s*(?:AnimationChannel\.Targets\.)?(ROTATION|POSITION|SCALE)\s*,(.*?)\)\s*\)", body, re.S):
        bone, target, keys = ch.groups()
        java = re.search(r"Keyframe\(\s*%s\s*,\s*KeyframeAnimations\.(degreeVec|posVec|scaleVec)\(\s*(%s)\s*,\s*(%s)\s*,\s*(%s)" % (NUM, NUM, NUM, NUM), keys)
        kotlin = re.search(r"(?:rot|scale)\(\s*%s\s*,\s*(%s)(?:\s*,\s*(%s)\s*,\s*(%s))?\s*\)" % (NUM, NUM, NUM, NUM), keys)
        if java:
            vec = [num(java.group(i)) for i in (2, 3, 4)]
        elif kotlin:
            vec = [num(kotlin.group(1))] * 3 if kotlin.group(2) is None else [num(kotlin.group(i)) for i in (1, 2, 3)]
        else:
            continue
        pose.setdefault(bone, {})[target] = vec
    return pose


def apply_pose(part, pose):
    p = pose.get(part.name, {})
    if "ROTATION" in p:
        part.rotation = part.rotation + np.radians(p["ROTATION"])
    if "POSITION" in p:
        x, y, z = p["POSITION"]
        part.offset = part.offset + np.array([x, -y, z])  # KeyframeAnimations.posVec flips y
    if "SCALE" in p:
        part.scale = part.scale * np.array(p["SCALE"])
    for c in part.children:
        apply_pose(c, pose)


# ---------------------------------------------------------------- geometry

def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_z(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def part_matrix(part):
    m = np.eye(4)
    m[:3, :3] = rot_z(part.rotation[2]) @ rot_y(part.rotation[1]) @ rot_x(part.rotation[0]) @ np.diag(part.scale)
    m[:3, 3] = part.offset
    return m


MODEL_TO_WORLD = np.diag([1.0, -1.0, -1.0, 1.0])
MODEL_TO_WORLD[1, 3] = 24.0


def collect_faces(part, parent_matrix, faces):
    matrix = parent_matrix @ part_matrix(part)
    for (u, v, mirror, x, y, z, w, h, d, g) in part.cubes:
        x0, y0, z0, x1, y1, z1 = x - g, y - g, z - g, x + w + g, y + h + g, z + d + g
        # (texture rect u, v, width, height), origin corner, s-edge, t-edge, local outward normal
        tw, th, td = w, h, d
        defs = {
            "north": ((u + td, v + td, tw, th), (x0, y0, z0), (x1, y0, z0), (x0, y1, z0), (0, 0, -1)),
            "south": ((u + 2 * td + tw, v + td, tw, th), (x1, y0, z1), (x0, y0, z1), (x1, y1, z1), (0, 0, 1)),
            "west": ((u, v + td, td, th), (x0, y0, z1), (x0, y0, z0), (x0, y1, z1), (-1, 0, 0)),
            "east": ((u + td + tw, v + td, td, th), (x1, y0, z0), (x1, y0, z1), (x1, y1, z0), (1, 0, 0)),
            "top": ((u + td, v, tw, td), (x0, y0, z1), (x1, y0, z1), (x0, y0, z0), (0, -1, 0)),
            "bottom": ((u + td + tw, v, tw, td), (x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (0, 1, 0)),
        }
        if mirror:  # Minecraft swaps the east/west textures and flips u
            defs["west"], defs["east"] = (defs["east"][0],) + defs["west"][1:], (defs["west"][0],) + defs["east"][1:]
        for name, (rect, o, s_end, t_end, normal) in defs.items():
            if rect[2] <= 0 or rect[3] <= 0:
                continue  # zero-area face of a flat plane
            pts = [np.array([*p, 1.0]) for p in (o, s_end, t_end)]
            world = [(MODEL_TO_WORLD @ matrix @ p)[:3] for p in pts]
            n = (MODEL_TO_WORLD[:3, :3] @ matrix[:3, :3] @ np.array(normal, dtype=float))
            n = n / (np.linalg.norm(n) or 1)
            flat = min(w, h, d) == 0
            faces.append((world, rect, n, mirror, flat))
    for c in part.children:
        collect_faces(c, matrix, faces)


# ---------------------------------------------------------------- rendering

def render(entity_id, out, yaw, pitch, size, pose_name):
    model_path, model_text = find_model(entity_id)
    root, (tex_w, tex_h) = parse_model(model_text)
    apply_pose(root, parse_pose(find_animation(model_path, model_text), pose_name))
    texture_path = find_texture(entity_id)
    textures = [(Image.open(texture_path).convert("RGBA"), False)]
    emissive = texture_path.replace(".png", "_emissive.png")
    if os.path.exists(emissive):
        textures.append((Image.open(emissive).convert("RGBA"), True))

    faces = []
    collect_faces(root, np.eye(4), faces)
    if not faces:
        sys.exit("the model has no boxes (could not parse it)")

    view = rot_x(math.radians(pitch)) @ rot_y(math.radians(-yaw))
    light = np.array([-0.35, 1.0, 0.6]); light /= np.linalg.norm(light)

    projected = []
    for world, rect, n, mirror, flat in faces:
        cam = [view @ p for p in world]
        cn = view @ n
        if cn[2] < 0:
            if not flat:
                continue  # back face of a solid box
            cn = -cn       # a flat plane is seen from either side
        o, s_end, t_end = cam
        a, b = s_end - o, t_end - o
        fourth = o + a + b
        depth = (o[2] + s_end[2] + t_end[2] + fourth[2]) / 4
        shade = 0.55 + 0.45 * max(0.0, float(np.dot(n if not flat else (n if np.dot(view @ n, [0, 0, 1]) >= 0 else -n), light)))
        projected.append((depth, o, a, b, rect, mirror, shade))
    projected.sort(key=lambda f: f[0])

    xs = [p[0] for f in projected for p in (f[1], f[1] + f[2], f[1] + f[3], f[1] + f[2] + f[3])]
    ys = [p[1] for f in projected for p in (f[1], f[1] + f[2], f[1] + f[3], f[1] + f[2] + f[3])]
    min_x, max_x, min_y, max_y = min(xs), max(xs), min(ys), max(ys)
    supersample = 3
    canvas_px = size * supersample
    margin = 0.08
    scale = canvas_px * (1 - 2 * margin) / max(max_x - min_x, max_y - min_y)

    def to_screen(p):
        return ((p[0] - (min_x + max_x) / 2) * scale + canvas_px / 2,
                (-(p[1] - (min_y + max_y) / 2)) * scale + canvas_px / 2)

    canvas = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    # soft ground shadow under the feet
    feet = [to_screen(f[1]) for f in projected]
    ground_y = max(p[1] for p in feet)
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([canvas_px * 0.28, ground_y - canvas_px * 0.035, canvas_px * 0.72, ground_y + canvas_px * 0.035], fill=(0, 0, 0, 70))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(canvas_px * 0.012)))

    for texture, glow in textures:
        tex = texture.resize((tex_w, tex_h), Image.NEAREST) if texture.size != (tex_w, tex_h) else texture
        for depth, o, a, b, (u, v, w, h), mirror, shade in projected:
            so, sa, sb = np.array(to_screen(o)), np.array(to_screen(o + a)) - np.array(to_screen(o)), np.array(to_screen(o + b)) - np.array(to_screen(o))
            det = sa[0] * sb[1] - sa[1] * sb[0]
            if abs(det) < 1e-6:
                continue
            # screen -> (s, t): solve [sa sb] [s t]^T = p - so ; then texel = (u + s*w, v + t*h)
            inv = np.array([[sb[1], -sb[0]], [-sa[1], sa[0]]]) / det
            # sample strictly inside the face's texture rect: the polygon edge would hit the neighbour texel
            eps = 0.02
            su = (w - 2 * eps) * (1 if not mirror else -1)
            u0 = u + eps if not mirror else u + w - eps
            a11, a12 = inv[0] * su
            a21, a22 = inv[1] * (h - 2 * eps)
            c1 = u0 - (a11 * so[0] + a12 * so[1])
            c2 = v + eps - (a21 * so[0] + a22 * so[1])
            warped = tex.transform(canvas.size, Image.AFFINE, (a11, a12, c1, a21, a22, c2), resample=Image.NEAREST)
            quad = [tuple(so), tuple(so + sa), tuple(so + sa + sb), tuple(so + sb)]
            mask = Image.new("L", canvas.size, 0)
            ImageDraw.Draw(mask).polygon(quad, fill=255)
            if not glow:
                r, g, bl, al = warped.split()
                r, g, bl = (ch.point(lambda x: int(x * shade)) for ch in (r, g, bl))
                warped = Image.merge("RGBA", (r, g, bl, al))
            alpha = Image.composite(warped.getchannel("A"), Image.new("L", canvas.size, 0), mask)
            warped.putalpha(alpha)
            canvas.alpha_composite(warped)

    result = canvas.resize((size, size), Image.LANCZOS)
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    result.save(out)
    print(out, result.size, len(projected), "faces")


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("id")
    ap.add_argument("--out")
    ap.add_argument("--yaw", type=float, default=30.0, help="turn to the viewer's right, degrees")
    ap.add_argument("--pitch", type=float, default=22.0, help="look down, degrees")
    ap.add_argument("--size", type=int, default=512)
    ap.add_argument("--pose", default="idle", help="animation whose first keyframe sets the pose ('none' for the bind pose)")
    args = ap.parse_args()
    out = args.out or f"docs/images/characters/{args.id}.png"
    render(args.id, out, args.yaw, args.pitch, args.size, args.pose if args.pose != "none" else "__none__")


if __name__ == "__main__":
    main()
