# Синхронизация с оригиналом

Оригинал: https://github.com/joshxviii/plants-and-zombies (remote `upstream`, ветка `26.3`).

Точка форка: `977cfb46` (тег `upstream-base-977cfb46`, мод v1.5.2).

Изменения из оригинала переносятся только через `git cherry-pick` (см. `docs/SPEC.md` §5.2).

## Просмотр новых коммитов

```bash
git fetch upstream
git log --oneline upstream-base-977cfb46..upstream/26.3
```

## Журнал

| Дата | Коммит upstream | Коммит в форке | Что | Примечание |
|---|---|---|---|---|
| — | — | — | — | пока ничего не переносилось |

## Известные проблемы в данных оригинала

Обнаружены при запуске порта; к переносу на NeoForge не относятся, есть и в Fabric-версии.

| Что | Где | Проявление |
|---|---|---|
| Достижения ссылаются на несуществующего родителя `plantz:robo_zombie` | `data/plantz/advancement/bigger_three.json`, `biggest_three.json` | `Couldn't load advancements: [plantz:bigger_three, plantz:biggest_three]` |
| Рецепт тако требует теги `c:crops/tomato`, `c:crops/onion`, `c:foods/leafy_green`, пустые без других модов | `data/plantz/recipe/taco.json` | `Recipe plantz:taco can't be placed due to empty ingredients` |
| Нет переводов субтитров для ~55 звуков | `assets/plantz/sounds.json`, `lang/*.json` | В dev-клиенте: `Missing subtitle translation` |
| Звук `music/raid/low.ogg` указан у события `music.gnome_space`, файла нет | `assets/plantz/sounds.json` | `File plantz:sounds/music/raid/low.ogg does not exist` |
