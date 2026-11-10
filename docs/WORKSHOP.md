# Мастерская персонажей

Как добавить в Sprout Squad своё растение. Образец — **Горшочек** (`sproutsquad:potling`), маскот мода: простой «производитель солнца», на котором проверен весь путь от регистрации до текстуры.

Процесс работы с сыном (идея → рисунок → модель → голос → код → испытание) описан в `docs/ROADMAP.md`, раздел 5. Требования к арту — раздел 8.3 там же. Здесь — техническая часть.

## Где что лежит

| Что | Файл |
|---|---|
| Регистрация сущности, цена в солнцах, атрибуты | `src/main/kotlin/dev/efureev/sproutsquad/registry/SquadEntities.kt` |
| Поведение растения | `src/main/kotlin/dev/efureev/sproutsquad/content/plant/<Name>.kt` |
| Модель и анимации | `src/main/kotlin/dev/efureev/sproutsquad/client/model/<Name>Model.kt` |
| Рендерер и слой модели | `src/main/kotlin/dev/efureev/sproutsquad/client/SproutSquadClient.kt` |
| Текстуры (основная и сонная) | `src/main/resources/assets/sproutsquad/textures/entity/plant/<id>/<id>.png`, `<id>_sleep.png` |
| Иконка на пакете семян (16×16) | `textures/item/plant_icon/<id>.png` + `models/item/plant_icon/<id>.json` |
| Имя | `assets/sproutsquad/lang/en_us.json` и `ru_ru.json`: `entity.sproutsquad.<id>` |
| Рецепт пакета семян | `data/sproutsquad/recipe/<id>_seed_packet.json` + `advancement/recipes/<id>_seed_packet.json` |
| Тег «растение» | `data/sproutsquad/tags/entity_type/squad_plants.json` |
| Исходники арта | `art/<id>/` (скрипт, Blockbench-проект, рисунок сына) |
| Палитра мода | `art/palette.png` (генерирует `art/draw_palette.py`) |

## Шаги

1. **Класс.** Скопировать `Potling.kt`, переименовать, задать цели ИИ. Готовые цели оригинала: `ProjectileAttackGoal` (стрелок, как Peashooter), `GenerateSunGoal` (солнце, как Sunflower), а также взрыв, ближний бой и т. д. в `joshxviii.plantz.ai.goal`. Ночной сон — `sleepsDuringNight()`.
2. **Регистрация.** Одна строка `plant("<id>", ::<Name>, width, height, sunCost, attributes)` в `SquadEntities`. Пакет семян в креативной вкладке и цена в солнцах появятся сами.
3. **Модель.** Наследник `SquadPlantModel`: геометрия в `createBodyLayer()`, анимации передаются в конструктор (`init`, `idle`, `action`, `sleep` — обязательный минимум из гайда). Модель из Blockbench (формат Modded Entity, экспорт Java, Mojang mappings) переводится в Kotlin один в один: те же `addBox`, `PartPose`, `Keyframe`.
4. **Рендерер.** Две строки в `SproutSquadClient`: слой модели и `PlantRenderer(...)`.
5. **Текстуры.** Основная и `_sleep` (закрытые глаза) — 64×64 по гайду 8.3; иконка 16×16 по размеру иконок оригинала. Рисунок сына — в `art/<id>/`.
6. **Данные.** Имя на en/ru, рецепт с компонентом `minecraft:entity_data`, строка в `squad_plants.json`.
7. **Проверка.** `./gradlew runGameTestServer` — тест `sproutsquad:squad_plants` проходит по чек-листу и называет, чего не хватает: класс, здоровье, цена, тег, рецепт, текстуры, иконка, имена. Галерея `/pz gallery` подхватит нового персонажа сама.
8. **В игре.** `./gradlew runClient`: посадить, посмотреть анимации днём и ночью, сравнить с соседями в галерее.
9. **Карточка.** `docs/CHARACTERS.md`: статус, характеристики, скриншот.

## Почему так

- Свой код регистрирует через `DeferredRegister`, а не прямым `Registry.register`, как оригинал: так не нужно ждать последнего `RegisterEvent` и можно спокойно трогать свои объекты.
- `SquadPlantModel` только запекает анимации в поля `PlantModel` оригинала (для этого они сделаны `protected`), а проигрывает их сам оригинал. Если автор изменит логику анимаций растений, наши растения получат её вместе с его растениями.
- Цена в солнцах задаётся в `SquadEntities` и при первом обращении записывается в конфиг сервера (`config/sproutsquad/server.json`), как у растений оригинала. Дальше действует значение из конфига: его можно подкрутить без пересборки, а новая цена в коде на уже созданный конфиг не влияет — поправь строку в `server.json` или удали её.

## Ещё не сделано

- Шаблон зомби (`zombie(...)` в `SquadEntities`) — появится вместе с первым своим зомби (Ф3).
- Свои звуки (`SquadSounds`, `sounds.json`) — вместе с первыми записями голоса сына.
- Шаблон проекта Blockbench с готовой развёрткой Горшочка.
