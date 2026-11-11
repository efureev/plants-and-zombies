#!/usr/bin/env python3
"""Builds docs/<locale>/characters/README.md from the character cards next to it.

Usage, from the repository root:
    python3 tools/docs/build_character_index.py

Also adds thumbnails to the tables of docs/<locale>/FEATURES.md whose rows start with a
character link or with the name of an item pictured in docs/images/items
(tools/docs/render_item.py), and puts an item's picture above a paragraph that opens with its
name (once; safe to run again).

Every card `docs/<locale>/characters/<id>.md` starts with:
    # <Name>
    **<Kind> · <role>** · <origin>
    <one-paragraph summary>
    | | |  ...a table with a "Health" / "Здоровье" row...
Cards with a "## Card" / "## Карточка" section are the mod's own characters. A picture in
docs/images/characters/<id>.png (tools/docs/render_character.py) becomes a thumbnail in the index.
"""
import os
import re

LOCALES = {
    "en": {
        "title": "Characters",
        "intro": "Every plant, zombie and machine of the mod has its own card. How to add a new character, the checklist and the style guide are in [WORKSHOP](../WORKSHOP.md); what the mod offers overall is in [FEATURES](../FEATURES.md).",
        "groups": [("own", "Own characters (Sprout Squad)"), ("plant", "Plants"), ("zombie", "Zombies"), ("machine", "Machines"), ("other", "Other creatures")],
        "kinds": {"plant": "plant", "zombie": "zombie", "machine": "machine"},
        "columns": "| | Character | Role | Health | What it does |\n|---|---|---|---|---|",
        "own_columns": "| | Character | Role | Status | What it does |\n|---|---|---|---|---|",
        "health": "Health",
        "card": "Card",
        "status": "Status",
        "template_title": "Card template for a new own character",
        "template_intro": "Copy it to `docs/en/characters/<id>.md` and write the Russian card `docs/ru/characters/<id>.md` too; render its picture with `python3 tools/docs/render_character.py <id>`, then rebuild this index with `python3 tools/docs/build_character_index.py`. Statuses: `idea` → `model` → `in code` → `done`.",
        "template": """# <Name>

<p align="center"><img src="../../images/characters/<id>.png" alt="<Name>" width="320"></p>

**Plant · <role>** · Sprout Squad

One or two sentences: what it is for.

| | |
|---|---|
| Id | `sproutsquad:<id>` |
| Health | |
| Sun cost | |
| Size | |
| Attack | |
| At night | |
| How to get | recipe / loot / trade / seed mutation |

## Abilities

- …

## Card

| | |
|---|---|
| Idea | |
| Sketch | `art/<id>/sketch.png` |
| Status | idea |""",
    },
    "ru": {
        "title": "Персонажи",
        "intro": "У каждого растения, зомби и машины мода — своя карточка. Как добавить нового персонажа, чек-лист и гайд по стилю — в [WORKSHOP](../WORKSHOP.md); что позволяет мод в целом — в [FEATURES](../FEATURES.md).",
        "groups": [("own", "Свои персонажи (Sprout Squad)"), ("plant", "Растения"), ("zombie", "Зомби"), ("machine", "Машины"), ("other", "Другие существа")],
        "kinds": {"растение": "plant", "зомби": "zombie", "машина": "machine"},
        "columns": "| | Персонаж | Роль | Здоровье | Что делает |\n|---|---|---|---|---|",
        "own_columns": "| | Персонаж | Роль | Статус | Что делает |\n|---|---|---|---|---|",
        "health": "Здоровье",
        "card": "Карточка",
        "status": "Статус",
        "template_title": "Шаблон карточки нового своего персонажа",
        "template_intro": "Скопируйте в `docs/ru/characters/<id>.md` и напишите английскую карточку `docs/en/characters/<id>.md`; нарисуйте картинку командой `python3 tools/docs/render_character.py <id>`, затем пересоберите это оглавление командой `python3 tools/docs/build_character_index.py`. Статусы: `идея` → `модель` → `в коде` → `готов`.",
        "template": """# <Имя>

<p align="center"><img src="../../images/characters/<id>.png" alt="<Имя>" width="320"></p>

**Растение · <роль>** · Sprout Squad

Одно-два предложения: для чего он.

| | |
|---|---|
| Id | `sproutsquad:<id>` |
| Здоровье | |
| Цена в солнцах | |
| Размер | |
| Атака | |
| Ночью | |
| Как получить | рецепт / лут / торговля / мутация семян |

## Умения

- …

## Карточка

| | |
|---|---|
| Идея | |
| Эскиз | `art/<id>/sketch.png` |
| Статус | идея |""",
    },
}


def parse(path, cfg):
    lines = open(path, encoding="utf-8").read().split("\n")
    title = next(l[2:].strip() for l in lines if l.startswith("# "))
    role_line = next(l for l in lines if l.startswith("**"))
    m = re.match(r"\*\*(.+?)\*\*\s*(?:·\s*(.*))?", role_line)
    kind_role, origin = m.group(1), (m.group(2) or "").strip()
    kind_word, _, role = kind_role.partition("·")
    kind = cfg["kinds"].get(kind_word.strip().lower(), "other")
    after = lines[lines.index(role_line) + 1:]
    summary = []
    for l in after:
        if not l.strip():
            if summary:
                break
            continue
        if l.startswith("|") or l.startswith("#"):
            break
        summary.append(l.strip())

    def row(name):
        for l in lines:
            cells = [c.strip() for c in l.strip().strip("|").split("|")]
            if len(cells) >= 2 and cells[0] == name:
                return cells[1]
        return ""

    own = any(l.strip() == f"## {cfg['card']}" for l in lines)
    return {
        "id": os.path.basename(path)[:-3], "title": title, "role": role.strip() or kind_word.strip(),
        "kind": "own" if own else kind, "origin": origin, "summary": " ".join(summary),
        "health": row(cfg["health"]), "status": row(cfg["status"]) if own else "",
    }


def first_sentence(text, limit=140):
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)  # no nested links inside the table
    m = re.match(r"(.+?[.!?])(\s|$)", text)
    s = m.group(1) if m else text
    return s if len(s) <= limit else s[:limit - 1].rstrip() + "…"


def build(locale, cfg):
    folder = os.path.join("docs", locale, "characters")
    cards = [parse(os.path.join(folder, f), cfg) for f in sorted(os.listdir(folder))
             if f.endswith(".md") and f != "README.md"]
    out = [f"# {cfg['title']}", "", cfg["intro"], ""]
    for key, heading in cfg["groups"]:
        group = sorted((c for c in cards if c["kind"] == key), key=lambda c: c["title"].lower())
        if not group:
            continue
        out += [f"## {heading} ({len(group)})", "", cfg["own_columns"] if key == "own" else cfg["columns"]]
        for c in group:
            third = c["status"] if key == "own" else c["health"]
            image = os.path.join("docs", "images", "characters", c["id"] + ".png")
            thumb = f'<img src="../../images/characters/{c["id"]}.png" alt="" width="112">' if os.path.exists(image) else ""
            out.append(f"| {thumb} | [{c['title']}]({c['id']}.md) | {c['role']} | {third} | {first_sentence(c['summary'])} |")
        out.append("")
    out += [f"## {cfg['template_title']}", "", cfg["template_intro"], "", "```markdown", cfg["template"], "```", ""]
    with open(os.path.join(folder, "README.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    return len(cards)


THUMB = '<img src="../images/{folder}/{id}.png" alt="" width="112">'
LINK = re.compile(r"^\s*\[[^\]]+\]\(characters/([\w]+)\.md\)")
LANG = {"en": "en_us", "ru": "ru_ru"}
VANILLA_ITEMS = {"Bucket": "bucket", "Ведро": "bucket"}


def item_names(locale):
    """Item name (as the game shows it) -> item id, for items with a picture in docs/images/items."""
    import json
    lang = json.load(open(f"src/main/resources/assets/sproutsquad/lang/{LANG[locale]}.json", encoding="utf-8"))
    names = {}
    for key, name in lang.items():
        m = re.match(r"(?:item|block)\.sproutsquad\.(\w+)$", key)
        if m and os.path.exists(os.path.join("docs", "images", "items", m.group(1) + ".png")):
            names[name] = m.group(1)
    names.update({k: v for k, v in VANILLA_ITEMS.items() if os.path.exists(os.path.join("docs", "images", "items", v + ".png"))})
    return names


def thumbnails_in_features(locale):
    """Adds a thumbnail column to the FEATURES tables whose rows start with a character link or an
    item name that has a picture (idempotent); a short paragraph about the Taco gets one too."""
    path = os.path.join("docs", locale, "FEATURES.md")
    if not os.path.exists(path):
        return 0
    items = item_names(locale)

    def picture(cell):
        m = LINK.match(cell)
        if m and os.path.exists(os.path.join("docs", "images", "characters", m.group(1) + ".png")):
            return THUMB.format(folder="characters", id=m.group(1))
        if cell.strip() in items:
            return THUMB.format(folder="items", id=items[cell.strip()])
        return None

    lines = open(path, encoding="utf-8").read().split("\n")
    # paragraphs that open with an item's name ("Taco: food ...", "Тако — еда ...") get its picture above
    for k in range(len(lines) - 1, -1, -1):
        for name, item in items.items():
            if re.match(re.escape(name) + r"\s*[:—]", lines[k]) and f"items/{item}.png" not in "".join(lines[max(0, k - 2):k]):
                lines[k:k] = [THUMB.format(folder="items", id=item), ""]
    out, i, count = [], 0, 0
    while i < len(lines):
        if not lines[i].startswith("|"):
            out.append(lines[i]); i += 1
            continue
        j = i
        while j < len(lines) and lines[j].startswith("|"):
            j += 1
        table = lines[i:j]
        cells = [[c for c in row.strip().strip("|").split("|")] for row in table]
        has_thumbs = any(c and c[0].strip().startswith("<img") for c in cells[2:])
        if len(table) > 2 and not has_thumbs and any(picture(c[0]) for c in cells[2:] if c):
            table = ["| |" + table[0][1:], "|---" + table[1]] + [
                "| " + (picture(c[0]) or "") + " " + row for row, c in zip(table[2:], cells[2:])]
        count += sum(1 for row in table if "<img" in row)
        out += table
        i = j
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    return count


def main():
    for locale, cfg in LOCALES.items():
        print(locale, build(locale, cfg), "cards,", thumbnails_in_features(locale), "pictures in FEATURES tables")


if __name__ == "__main__":
    main()
