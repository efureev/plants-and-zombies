# Sprout Squad

NeoForge-форк мода [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) (joshxviii, MPL-2.0) для Minecraft 26.3.
Цели: порт с Fabric на NeoForge → свой mod id → свои персонажи.

Публичная документация — в `docs/<locale>/`: **`docs/en/` — основная**, `docs/ru/` — русская версия тех же файлов (README, INSTALL, FEATURES, WORKSHOP, STRUCTURES, UPSTREAM, `characters/`). Картинки общие — `docs/images/`. Любую правку публичной документации вноси в **обе** версии в одном коммите. Корневой `README.md` — английский, русский — `docs/ru/README.md`.

Рабочие документы — **только на русском и только локально**, не в гите (`/plan/` в `.gitignore`): ТЗ `plan/SPEC.md` и план развития `plan/ROADMAP.md`. Не добавляй их в коммиты и не ссылайся на них из публичных файлов и кода.

Всё, что попадает в git (документация, код, комментарии, коммиты), пишется нейтрально: без упоминаний семьи и авторов («сын», «папа» и т. п.); вместо этого — «автор идеи», «исходный рисунок».

**Полное ТЗ: `plan/SPEC.md`.** Перед началом любой задачи сверься с текущим этапом (M0–M10) и картой переноса (разделы 6–8).
При закрытии этапа обязательно отмечай его в таблице статуса этапов (`plan/SPEC.md` §6) и значком ✅ в заголовке этапа.
План развития контента (фазы Ф0–Ф12, каталог персонажей) — `plan/ROADMAP.md`. Новый арт рисуй по гайду по стилю из `docs/<locale>/WORKSHOP.md` (он же раздел 8.3 роадмапа).

## Ключевые решения
- Название: **Sprout Squad**, mod id: **`sproutsquad`**, артефакт: `sprout-squad`.
- Mod id `sproutsquad` (с этапа M8): ресурсы в `assets/sproutsquad` и `data/sproutsquad`, все id — `sproutsquad:…`. Менять id существующих блоков/предметов/сущностей нельзя — это ломает миры.
- Пакет оригинала `joshxviii.plantz` **не переименовывать никогда** (ради cherry-pick из upstream); префиксы `plantz$` у членов миксинов тоже остаются.
- После cherry-pick из оригинала прогоняй `python3 tools/upstream/rename_modid.py .` (и `tools/upstream/nbt_rename.py` для структур `.nbt`): у автора namespace по-прежнему `plantz`.
- Свой новый код — только в `dev.efureev.sproutsquad.*` со своими `DeferredRegister`; файлы оригинала правим минимально.
- Только NeoForge. Fabric не поддерживаем, мультилоадер не нужен.

## Стек
Minecraft 26.3 · NeoForge 26.3.x · ModDevGradle ≥ 2.0.147 · Kotlin for Forge (версия Kotlin-плагина = версия Kotlin в KFF) · Java 25.

## Команды
```bash
./gradlew build            # сборка + проверка
./gradlew runClient        # клиент для ручной проверки
./gradlew runServer        # выделенный сервер — обязательно проверять после изменений в общем коде
./gradlew runGameTestServer   # прогон загрузки мода и GameTest-ов (в т.ч. `sproutsquad:gallery`), сам завершается
```
JDK 25: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25`.

В игре: `/pz gallery [labels]` выставляет всех персонажей в ряд (замороженными), `/pz gallery clear` убирает — для скриншотов и проверки арта. Новые персонажи попадают в галерею сами (берутся из реестра сущностей).

`runClient` открывает окно игры. Запускай строго один экземпляр. При ошибке загрузки NeoForge показывает экран ошибки и **не завершает процесс**, поэтому следи за логом и закрывай клиент сам. Убивай только клиент: шаблон `net.neoforged.devlaunch.Main` совпадает и с сервером (у него `--nogui`), и с процессами в `.claude/worktrees`.

Любое ожидание запуска — со сторожем: если лог молчит дольше 45 секунд, это зависание, а не загрузка. Сними дамп потоков (`$JAVA_HOME/bin/jcmd <pid> Thread.print`), закрой процесс и разбирайся по дампу; не жди общий таймаут. Известный случай: на macOS ранний экран загрузки NeoForge виснет в `SDL_GL_SwapWindow`, когда окно не видно (перекрыто, другой рабочий стол, погашен экран). Для проверки запуска выключай его: `earlyWindowControl = false` в `run/config/fml.toml`, потом верни.

## Правила кода
- **Код оригинала** (`joshxviii.plantz`) регистрирует контент напрямую через `Registry.register` в Kotlin `object`. Это работает, потому что `PazMain.onInitialize()` вызывается из *последнего* `RegisterEvent` (см. `neoforge/PazNeoForge.kt`), когда все реестры ещё открыты. Не трогай `Paz*`-объекты до этого момента: ни в конструкторе `@Mod`, ни в статических полях миксинов.
- **Новый код** (`dev.efureev.sproutsquad`) — только `DeferredRegister` / `DeferredHolder`, без `.get()` в инициализаторах.
- Слой порта — пакет `joshxviii.plantz.neoforge` (точки входа `@Mod`, прослойки вместо Fabric API) и миксины в `joshxviii.plantz.mixin.neoforge`.
- Подписка на события из Kotlin — только с явным классом: `bus.addListener(SomeEvent::class.java) { }`. Вариант `addListener<SomeEvent> { }` компилируется, но NeoForge не может определить тип события по Kotlin-лямбде.
- Синхронизируемые данные на ванильных сущностях — через data attachments (`neoforge/PazSyncedData.kt`), а не `SynchedEntityData.defineId` в миксинах: NeoForge это запрещает (в dev — падение).
- Компоненты предметов обязаны реализовывать `equals`/`hashCode`: `data class`, `data object`, record или enum.
- Приватные поля Minecraft, которые нужны коду, открываются в `src/main/resources/META-INF/accesstransformer.cfg`.
- Общий код не импортирует клиентские классы (`net.minecraft.client.*`, рендер, экраны) — иначе падает выделенный сервер. Клиент — через `@EventBusSubscriber(value = Dist.CLIENT)` / клиентский `@Mod(dist = Dist.CLIENT)`.
- Новая логика на Kotlin; Java — только миксины. Стиль — как в оригинале.
- Миксины: `defaultRequire = 1`. Если в NeoForge есть подходящее событие — предпочитай его миксину.
- Точные имена событий/API сверяй с javadoc NeoForge 26.3, не по памяти — API между версиями менялось.
- Не удалять лицензионные уведомления MPL-2.0; изменённые файлы оригинала остаются под MPL-2.0.

## Контент
- Любой новый текст — минимум в `en_us.json` и `ru_ru.json`.
- Персонажи: по файлу на каждого в `docs/<locale>/characters/<id>.md` (растения, зомби, машины — и оригинала, и свои), оглавление `characters/README.md` собирает `python3 tools/docs/build_character_index.py`; картинки для документации (персонажи, их облики, предметы, эффекты, постройки) рисует и расставляет `python3 tools/docs/refresh.py` (нужны Pillow и numpy; `--check` — только проверка). Исходники Blockbench и эскизы — в `art/<name>/`, чек-лист — в `WORKSHOP.md`. Полное описание возможностей мода — `docs/<locale>/FEATURES.md`, краткое — в README.
- Как добавить растение (файлы, шаги, образец Potling / «Горшочек») — `docs/<locale>/WORKSHOP.md`. Тест `sproutsquad:squad_plants` проверяет чек-лист для всех своих растений.
- GameTest-ы регистрируются в `dev/efureev/sproutsquad/debug/SquadGameTests.kt`; у каждого своё окружение (id окружения регистрируется только один раз).
- После M8 запрещено менять id существующих блоков/предметов/сущностей без миграции — это ломает миры.

## Git
- Основная ветка `main`; работа в ветках `port/<этап>`, `feat/<персонаж>`, `fix/<что>`, слияние через PR.
- Conventional Commits: `port:`, `feat:`, `fix:`, `chore:`, `docs:`, `assets:`.
- `upstream` = joshxviii/plants-and-zombies (ветка `26.3`). Изменения из оригинала — только `git cherry-pick`, каждый записывать в `docs/<locale>/UPSTREAM.md`.
- Каждый завершённый этап коммить локально в его ветку `port/<этап>` (`feat/<персонаж>` и т.д.). Push, PR и merge в `main` — только по явной просьбе.

## Язык
Общение — на русском. Документация — на английском (основная, `docs/en/`) и русском (`docs/ru/`). Код, идентификаторы, комментарии в коде и сообщения коммитов — на английском; в комментариях ссылайся на `docs/en/…`.
