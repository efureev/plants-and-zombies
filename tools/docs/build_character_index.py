#!/usr/bin/env python3
"""Builds docs/<locale>/characters/README.md from the character cards next to it.

Usage, from the repository root:
    python3 tools/docs/build_character_index.py

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
            thumb = f'<img src="../../images/characters/{c["id"]}.png" alt="" width="56">' if os.path.exists(image) else ""
            out.append(f"| {thumb} | [{c['title']}]({c['id']}.md) | {c['role']} | {third} | {first_sentence(c['summary'])} |")
        out.append("")
    out += [f"## {cfg['template_title']}", "", cfg["template_intro"], "", "```markdown", cfg["template"], "```", ""]
    with open(os.path.join(folder, "README.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(out))
    return len(cards)


def main():
    for locale, cfg in LOCALES.items():
        print(locale, build(locale, cfg), "cards")


if __name__ == "__main__":
    main()
