#!/usr/bin/env python3
"""Keeps the pictures of the docs up to date in one go: finds every picture FEATURES.md and the
character cards ask for (see tools/docs/pictures.py), renders the missing ones (all of them with
--force), puts them into the docs (build_character_index.py) and lists what of the mod still has
no picture anywhere in the docs.

Usage (needs Pillow and numpy), from the repository root:
    python3 tools/docs/refresh.py [--force]
    python3 tools/docs/refresh.py --check     # render nothing; fail if a picture is missing
"""
import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_character_index as index  # noqa: E402
import pictures as P  # noqa: E402


def wanted():
    """(kind, id) -> picture, for every picture the docs ask for, plus every character's own."""
    need = {}
    for locale in index.LOCALES:
        found = {P.character(cid, locale) for cid in P.cards(locale)}
        for doc in index.docs_with_pictures(locale):
            index.pictures_in(doc, locale, found, write=False)
        for p in found:
            need.setdefault((p.kind, p.id), p)
    return need


def render(picture):
    out = P.path(picture)
    if picture.kind == "characters":
        import render_character
        render_character.render(picture.base, out, look=picture.look)
    elif picture.kind == "items":
        import render_item
        render_item.render(picture.id, out)
    elif picture.kind == "structures":
        import render_structure
        render_structure.render(picture.base, out)
    else:
        import render_item
        render_item.render_effect(picture.id, out)


def report(need):
    shown = {key for key in need}
    items = sorted(i for i in P.item_names("en") if ("items", i) not in shown and not i.startswith("potion_"))
    looks = sorted(f"{cid}_{look}" for cid in P.cards("en") for look in P.looks(cid) if ("characters", f"{cid}_{look}") not in shown)
    structures = sorted(p.id for p in P.structures("en") if ("structures", p.id) not in shown)
    entities = sorted(k.split(".")[-1] for k in P.lang("en") if k.count(".") == 2 and k.startswith("entity.sproutsquad.")
                      and k.split(".")[-1] not in P.cards("en"))
    for title, names in (("items without a picture in the docs", items),
                         ("looks without a picture in the docs", looks),
                         ("structures without a picture in the docs", structures),
                         ("entities without a card (projectiles and the like)", entities)):
        print(f"{title} ({len(names)}): {' '.join(names) if names else '-'}")


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--force", action="store_true", help="render every picture again")
    ap.add_argument("--check", action="store_true", help="render nothing; fail if a picture is missing")
    args = ap.parse_args()
    need = wanted()
    missing = [p for p in need.values() if args.force or not P.exists(p)]
    if args.check:
        for p in missing:
            print("missing:", P.path(p), f"({p.name})")
        sys.exit(1 if missing else 0)
    failed = []
    for p in sorted(missing):
        try:
            render(p)
        except SystemExit as e:  # the renderers exit with a message when they cannot draw it
            failed.append(f"{P.path(p)}: {e}")
    index.main()
    print(f"rendered {len(missing) - len(failed)} of {len(missing)} pictures, {len(need)} in use")
    for line in failed:
        print("could not render", line)
    report(need)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
