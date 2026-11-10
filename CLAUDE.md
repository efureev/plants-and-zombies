# Sprout Squad

NeoForge-форк мода [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) (joshxviii, MPL-2.0) для Minecraft 26.3.
Цели: порт с Fabric на NeoForge → свой mod id → свои персонажи (придумываем вместе с сыном).

**Полное ТЗ: [`docs/SPEC.md`](docs/SPEC.md).** Перед началом любой задачи сверься с текущим этапом (M0–M10) и картой переноса (разделы 6–8).

## Ключевые решения
- Название: **Sprout Squad**, mod id: **`sproutsquad`**, артефакт: `sprout-squad`.
- **До этапа M8 mod id остаётся `plantz`.** Не переименовывай `assets/plantz`, `data/plantz` и ресурсные id раньше M8.
- Пакет оригинала `joshxviii.plantz` **не переименовывать никогда** (ради cherry-pick из upstream).
- Свой новый код — только в `dev.efureev.sproutsquad.*` со своими `DeferredRegister`; файлы оригинала правим минимально.
- Только NeoForge. Fabric не поддерживаем, мультилоадер не нужен.

## Стек
Minecraft 26.3 · NeoForge 26.3.x · ModDevGradle ≥ 2.0.147 · Kotlin for Forge (версия Kotlin-плагина = версия Kotlin в KFF) · Java 25.

## Команды
```bash
./gradlew build            # сборка + проверка
./gradlew runClient        # клиент для ручной проверки
./gradlew runServer        # выделенный сервер — обязательно проверять после изменений в общем коде
./gradlew runGameTestServer
```

## Правила кода
- Регистрация только через `DeferredRegister` / `DeferredHolder`. Никаких `Registry.register(...)` и никаких `.get()` у holder'ов в инициализаторах Kotlin `object`.
- Общий код не импортирует клиентские классы (`net.minecraft.client.*`, рендер, экраны) — иначе падает выделенный сервер. Клиент — через `@EventBusSubscriber(value = Dist.CLIENT)` / клиентский `@Mod(dist = Dist.CLIENT)`.
- Новая логика на Kotlin; Java — только миксины. Стиль — как в оригинале.
- Миксины: `defaultRequire = 1`. Если в NeoForge есть подходящее событие — предпочитай его миксину.
- Точные имена событий/API сверяй с javadoc NeoForge 26.3, не по памяти — API между версиями менялось.
- Не удалять лицензионные уведомления MPL-2.0; изменённые файлы оригинала остаются под MPL-2.0.

## Контент
- Любой новый текст — минимум в `en_us.json` и `ru_ru.json`.
- Свои персонажи: реестр и карточки в `docs/CHARACTERS.md`, исходники Blockbench и эскизы — в `art/<name>/`, чек-лист — `docs/SPEC.md` §9.3.
- После M8 запрещено менять id существующих блоков/предметов/сущностей без миграции — это ломает миры.

## Git
- Основная ветка `main`; работа в ветках `port/<этап>`, `feat/<персонаж>`, `fix/<что>`, слияние через PR.
- Conventional Commits: `port:`, `feat:`, `fix:`, `chore:`, `docs:`, `assets:`.
- `upstream` = joshxviii/plants-and-zombies (ветка `26.3`). Изменения из оригинала — только `git cherry-pick`, каждый записывать в `docs/UPSTREAM.md`.
- Не коммить и не пушить без явной просьбы.

## Язык
Документация проекта и общение — на русском. Код, идентификаторы, комментарии в коде и сообщения коммитов — на английском.
