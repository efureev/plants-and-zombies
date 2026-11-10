#!/usr/bin/env python3
"""Rename the mod id plantz -> sproutsquad in text resources and in code string literals.

Never touches: the Java/Kotlin package `joshxviii.plantz` (and mixin config "package"),
mixin member prefixes `plantz$`, or resource paths such as `plantz_flag`.
Usage: python3 tools/upstream/rename_modid.py . [--dry]

Run it after cherry-picking a commit from the original mod: new upstream lines still use the
`plantz` namespace. Files that land in assets/plantz or data/plantz must be moved to
assets/sproutsquad / data/sproutsquad (git usually does this itself via rename detection).
Lines containing "joshxviii" are skipped in resources (mixin "package"), check them by hand.
"""
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1])
DRY = "--dry" in sys.argv
OLD, NEW = "plantz", "sproutsquad"

TEXT_EXT = {".json", ".mcmeta", ".fsh", ".vsh", ".glsl", ".txt", ".toml"}


def rename_token(s: str) -> str:
    """Rename namespace/key uses of `plantz` inside one string (already known not to be a package)."""
    s = re.sub(r"(?<![A-Za-z0-9_.])plantz:", NEW + ":", s)          # plantz:foo   (ids, NBT keys)
    s = re.sub(r"\.plantz\.", "." + NEW + ".", s)                    # item.plantz.x (lang keys)
    s = re.sub(r"(?<![A-Za-z0-9_.$])plantz\.(?=[a-z])", NEW + ".", s)  # plantz.subtitles.x, plantz.mixins.json
    s = re.sub(r"(?<![A-Za-z0-9_./])plantz/", NEW + "/", s)          # plantz/wave_type/...
    s = re.sub(r"\.plantz$", "." + NEW, s)                           # trailing key segment
    if s == OLD:
        s = NEW
    return s


def process_resource(path: Path) -> int:
    text = path.read_text(encoding="utf-8")
    out_lines, n = [], 0
    for line in text.split("\n"):
        if "joshxviii" in line:                       # mixin config "package": keep
            out_lines.append(line)
            continue
        # rename inside every JSON/TOML string literal on the line, plus bare GLSL imports
        new = re.sub(r'"((?:[^"\\]|\\.)*)"', lambda m: '"' + rename_token(m.group(1)) + '"', line)
        new = re.sub(r"<plantz:", "<" + NEW + ":", new)
        if new != line:
            n += 1
        out_lines.append(new)
    if n and not DRY:
        path.write_text("\n".join(out_lines), encoding="utf-8")
    return n


STRING_LIT = re.compile(r'"((?:[^"\\\n]|\\.)*)"')


def process_code(path: Path) -> int:
    text = path.read_text(encoding="utf-8")
    n = 0

    def repl(m):
        nonlocal n
        body = m.group(1)
        if "joshxviii" in body:
            return m.group(0)
        new = rename_token(body)
        if new != body:
            n += 1
        return '"' + new + '"'

    new_text = STRING_LIT.sub(repl, text)
    if n and not DRY:
        path.write_text(new_text, encoding="utf-8")
    return n


total = {}
for p in sorted((ROOT / "src/main/resources").rglob("*")) + sorted((ROOT / "src/main/templates").rglob("*")):
    if p.is_file() and p.suffix in TEXT_EXT:
        k = process_resource(p)
        if k:
            total[str(p.relative_to(ROOT))] = k
for p in sorted((ROOT / "src/main").rglob("*")):
    if p.is_file() and p.suffix in {".kt", ".java"}:
        k = process_code(p)
        if k:
            total[str(p.relative_to(ROOT))] = k

print(f"files changed: {len(total)}, lines/literals: {sum(total.values())}")
for k, v in sorted(total.items(), key=lambda t: -t[1])[:15]:
    print(f"  {v:5} {k}")
