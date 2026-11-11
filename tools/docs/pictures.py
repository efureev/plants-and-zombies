"""What pictures the docs show and where they come from: the characters of the mod (and their
looks: texture variants such as the Snowcoat or a name-tag look), its items and its effects,
matched by the names the docs use for them.

Shared by build_character_index.py (puts existing pictures into the docs) and refresh.py
(renders the pictures the docs need). Pictures live in docs/images/<kind>/<id>.png.

How a doc asks for pictures:
- a table row: the first cell names a character (a link to its card or its title), an item or an
  effect as the game calls it; when the first cell is a character, the second cell may name its
  looks instead ("Browncoat | Snowcoat", "Super Brainz | Toxic, Electro", "Sunflower | `Vampire`");
  a looks table in a card names them as "Snowcoat (`snowcoat`)";
- a line <!-- pictures: id id ... --> (or <!-- pictures 64: ... --> for a size): a row of pictures
  after it. An id is an item, a character, a look (<character>_<look>) or an effect; it may be a
  pattern (*_balloon, marigold_*) and may say its kind (item:, character:, effect:).
"""
import fnmatch
import functools
import json
import os
import re
from collections import namedtuple

ASSETS = "src/main/resources/assets/sproutsquad"
IMAGES = "docs/images"
LANG_FILE = {"en": "en_us", "ru": "ru_ru"}

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
DYE_NAMES = {
    "en": {d: d.replace("_", " ") for d in DYES},
    "ru": dict(zip(DYES, ["белые", "оранжевые", "сиреневые", "голубые", "жёлтые", "лаймовые", "розовые",
                          "серые", "светло-серые", "бирюзовые", "фиолетовые", "синие", "коричневые",
                          "зелёные", "красные", "чёрные"])),
}
# texture suffixes that are a state or a separate part, not a look of the character
NOT_A_LOOK = {"sleep", "damage", "glow", "baby", "angry", "butter", "barrel", "imp"}
# names the docs use for things that have no name of their own in the mod's lang files
VANILLA_ITEMS = {"bucket": {"en": "Bucket", "ru": "Ведро"}}
EFFECT_ALIASES = {"painted": {"en": "Painted", "ru": "Покрашен"}}
LOOK_TITLES = {"zombat_baron": {"en": "Baron's bat", "ru": "Зомбимышь Барона"}}

Picture = namedtuple("Picture", "kind id name base look", defaults=(None, None))


def path(picture):
    return os.path.join(IMAGES, picture.kind, picture.id + ".png")


def exists(picture):
    return os.path.exists(path(picture))


# ---------------------------------------------------------------- what the mod has

@functools.lru_cache(None)
def lang(locale):
    return json.load(open(f"{ASSETS}/lang/{LANG_FILE[locale]}.json", encoding="utf-8"))


@functools.lru_cache(None)
def cards(locale):
    """Character id -> its title, from docs/<locale>/characters/<id>.md."""
    folder = f"docs/{locale}/characters"
    titles = {}
    for f in sorted(os.listdir(folder)):
        if f.endswith(".md") and f != "README.md":
            first = open(os.path.join(folder, f), encoding="utf-8").readline()
            titles[f[:-3]] = first.lstrip("# ").strip()
    return titles


@functools.lru_cache(None)
def looks(entity_id):
    """Texture variants of a character that change its look: <id>_<look>.png next to <id>.png."""
    found = []
    for root, _, files in os.walk(f"{ASSETS}/textures/entity"):
        if os.path.basename(root) != entity_id or entity_id + ".png" not in files:
            continue
        for f in sorted(files):
            if not f.startswith(entity_id + "_") or not f.endswith(".png") or f.endswith("_emissive.png"):
                continue
            look = f[len(entity_id) + 1:-4]
            if not NOT_A_LOOK & set(look.split("_")):
                found.append(look)
    return found


@functools.lru_cache(None)
def look_names(locale):
    """Character id -> {look name in the card: look}, from card rows like "| Snowcoat (`snowcoat`) |"."""
    names = {}
    for cid in cards(locale):
        text = open(f"docs/{locale}/characters/{cid}.md", encoding="utf-8").read()
        # the first cell may be a thumbnail column already
        for name, look in re.findall(r"^\|(?:[^|]*<img[^|]*\||\s*\|)?\s*([^|`<]+?)\s*\(`(\w+)`\)\s*\|", text, re.M):
            if look in looks(cid):
                names.setdefault(cid, {})[name] = look
    return names


@functools.lru_cache(None)
def item_names(locale):
    """Item id -> its name in the game; potion_<name> are the mod's potions."""
    names = {}
    items = {f[:-5] for f in os.listdir(f"{ASSETS}/items") if f.endswith(".json") and "spawn_egg" not in f}
    for key, name in lang(locale).items():
        m = re.match(r"(?:item|block)\.sproutsquad\.(\w+)$", key)
        if m and m.group(1) in items:
            names[m.group(1)] = name
        m = re.match(r"item\.minecraft\.potion\.effect\.(\w+)$", key)
        if m:
            names["potion_" + m.group(1)] = name
    names.update({item: n[locale] for item, n in VANILLA_ITEMS.items()})
    if "seed_packet" in names:  # its picture shows a Peashooter's packet (render_item.py)
        names["seed_packet"] = lang(locale)["item.sproutsquad.seed_packet.entity"] % lang(locale)["entity.sproutsquad.peashooter"]
    return names


@functools.lru_cache(None)
def effect_names(locale):
    names = {}
    for key, name in lang(locale).items():
        m = re.match(r"effect\.sproutsquad\.(\w+)$", key)
        if m:
            names[m.group(1)] = name
    names.update({effect: n[locale] for effect, n in EFFECT_ALIASES.items()})
    return names


# ---------------------------------------------------------------- pictures by name

def character(cid, locale, look=None):
    title = cards(locale)[cid]
    if not look:
        return Picture("characters", cid, title, cid)
    named = {v: k for k, v in look_names(locale).get(cid, {}).items()}
    if look in named:
        name = named[look]
    elif f"{cid}_{look}" in LOOK_TITLES:
        name = LOOK_TITLES[f"{cid}_{look}"][locale]
    elif look in DYES:
        name = f"{title} ({DYE_NAMES[locale][look]})"
    else:  # a name-tag look
        name = f"{title} «{look.upper() if len(look) <= 2 else look.capitalize()}»"
    return Picture("characters", f"{cid}_{look}", name, cid, look)


LINK = re.compile(r"\s*\*{0,2}\[[^\]]+\]\((?:characters/)?(\w+)\.md\)")


def norm(name):
    """Names compare loosely: "Wall-nut" and "Wall Nut", «Флаг мозгоф» and Флаг мозгоф are one."""
    return re.sub(r"[\W_]", "", name.lower().replace("ё", "е"))


def clean(text):
    text = re.sub(r"`[^`]*`", "", text)
    text = re.sub(r"\([^)]*\)", "", text)
    return text.replace("**", "").strip().strip("«»\"'").strip()


def resolve(cell, locale):
    """The picture a table cell names, or None."""
    m = LINK.match(cell)
    if m and m.group(1) in cards(locale):
        return character(m.group(1), locale)
    name = norm(clean(cell))
    if not name:
        return None
    for iid, n in item_names(locale).items():
        if norm(n) == name:
            return Picture("items", iid, n)
    for eid, n in effect_names(locale).items():
        if norm(n) == name:
            return Picture("effects", eid, n)
    for cid, title in cards(locale).items():
        if norm(title) == name:
            return character(cid, locale)
    for cid, named in look_names(locale).items():
        for look_name, look in named.items():
            if norm(look_name) == name:
                return character(cid, locale, look)
    return None


def resolve_look(cid, part, locale):
    word = part.strip().strip("`«»\"' ")
    if word.lower().replace(" ", "_") in looks(cid):
        return character(cid, locale, word.lower().replace(" ", "_"))
    for name, look in look_names(locale).get(cid, {}).items():
        if word and (name == word or re.match(re.escape(word) + r"[ -]", name) or name.endswith(" " + word)):
            return character(cid, locale, look)
    return None


def resolve_row(cells, locale):
    """Pictures for a table row: its first cell, or the looks its second cell names."""
    first = resolve(cells[0], locale)
    if first and first.kind == "characters" and not first.look and len(cells) > 1:
        parts = [p for p in re.split(r",\s*", cells[1].strip()) if p]
        found = [resolve_look(first.id, p, locale) for p in parts]
        if parts and all(found):
            return found
    return [first] if first else []


def everything(locale, kind=None):
    pool = []
    if kind in (None, "item"):
        pool += [Picture("items", i, n) for i, n in item_names(locale).items()]
    if kind in (None, "character"):
        for cid in cards(locale):
            pool.append(character(cid, locale))
            pool += [character(cid, locale, look) for look in looks(cid)]
    if kind in (None, "effect"):
        pool += [Picture("effects", e, n) for e, n in effect_names(locale).items()]
    return pool


def dye_order(picture):
    for dye in sorted(DYES, key=len, reverse=True):
        if re.search(r"(^|_)%s(_|$)" % dye, picture.id):
            return (DYES.index(dye), picture.id)
    return (len(DYES), picture.id)


def expand(tokens, locale):
    """Pictures for the ids of a <!-- pictures: ... --> line, patterns sorted in dye order."""
    out = []
    for token in tokens:
        kind, _, pattern = token.rpartition(":")
        matched = [p for p in everything(locale, kind or None) if fnmatch.fnmatchcase(p.id, pattern)]
        if any(c in pattern for c in "*?["):
            out += sorted(matched, key=dye_order)
        else:
            out += matched[:1]
    return out
