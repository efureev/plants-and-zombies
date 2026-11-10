#!/usr/bin/env python3
"""Rewrite `plantz:` namespaces inside NBT structure files (needs `pip install nbtlib`).

Usage: python3 tools/upstream/nbt_rename.py [--dry] FILE...
"""
import gzip
import sys

import nbtlib
from nbtlib.tag import Compound, List, String

OLD, NEW = "plantz:", "sproutsquad:"


def walk(tag, hits):
    if isinstance(tag, Compound):
        for k in list(tag.keys()):
            v = tag[k]
            if isinstance(v, String) and OLD in v:
                hits.append(str(v))
                tag[k] = String(str(v).replace(OLD, NEW))
            else:
                walk(v, hits)
    elif isinstance(tag, List):
        for i, v in enumerate(tag):
            if isinstance(v, String) and OLD in v:
                hits.append(str(v))
                tag[i] = String(str(v).replace(OLD, NEW))
            else:
                walk(v, hits)


dry = "--dry" in sys.argv
for path in [a for a in sys.argv[1:] if a != "--dry"]:
    with open(path, "rb") as f:
        gz = f.read(2) == b"\x1f\x8b"
    nbt = nbtlib.load(path, gzipped=gz)
    hits = []
    walk(nbt, hits)
    print(f"{path}: gzip={gz} replaced={len(hits)} {sorted(set(hits))[:6]}")
    if hits and not dry:
        nbt.save(path, gzipped=gz)
