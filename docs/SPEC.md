# ТЗ: NeoForge-форк Plants & Zombies

| | |
|---|---|
| Репозиторий | https://github.com/efureev/plants-and-zombies |
| Оригинал (upstream) | https://github.com/joshxviii/plants-and-zombies (ветка `26.3`, коммит `977cfb46`, мод v1.5.2) |
| Основная ветка форка | `main` |
| Целевая платформа | Minecraft 26.3 + NeoForge 26.3.x, Java 25, Kotlin |
| Версия документа | 1.0 — 2026-11-10 |

---

## 1. Цели

1. **Перенести мод Plants & Zombies с Fabric на NeoForge** с сохранением всего функционала оригинала (растения, зомби, рейды, генерация мира, измерение, почта, машина времени, звуки, локализации).
2. **Сделать форк самостоятельным проектом**, который со временем уходит от оригинала: свой mod id, своё название, своя версия.
3. **Добавлять собственных персонажей** (растения, зомби и др.), придуманных вместе с сыном, по понятному и повторяемому процессу.
4. **Сохранить возможность забирать изменения из оригинала** (новые персонажи, исправления) выборочно, через cherry-pick, пока это оправдано.

### Не-цели

- Поддержка Fabric (Fabric-сборка удаляется полностью, мультилоадер не нужен).
- Поддержка версий Minecraft, отличных от 26.3, на первом этапе.
- Публикация на Modrinth/CurseForge — не обязательна; если появится — см. раздел 13.
- Совместимость сохранений с оригинальным модом (после смены mod id миры несовместимы — это осознанное решение).

---

## 2. Исходное состояние (результат анализа оригинала)

| Параметр | Значение |
|---|---|
| Загрузчик | Fabric Loader 0.20.1, Fabric API 0.165.2+26.3, Loom 1.17.21 |
| Язык | Kotlin 2.4.21 (305 файлов `.kt`) + Java (152 файла `.java`, в т.ч. миксины) |
| Mod id / пакет | `plantz` / `joshxviii.plantz` |
| Source sets | раздельные `main` и `client` (`splitEnvironmentSourceSets`) |
| Контент | 37 классов растений (`entity/plant`), ~29 классов зомби (`entity/zombie`), гномы, блюпринт-машины, снаряды, блоки, предметы, эффекты, зачарования, рейды/боссы, своё измерение и структуры, торговля, рецепты, достижения |
| Модели | ванильные Java/Kotlin-модели (`PazModels.kt`), **GeckoLib не используется** |
| Опциональные зависимости | TerraBlender (worldgen), Iris (шейдеры, через `isModLoaded`), ModMenu (экран конфига), ассеты для dynamic lights |
| Миксины | 13 серверных/общих + 8 клиентских (+ пустой `ExampleClientMixin`) |
| Access widener | нет |
| Fabric API | ~40 файлов зависят от `net.fabricmc.*` (полный список — раздел 7) |
| Прямые `Registry.register(...)` | 43 вызова (в NeoForge так нельзя — нужны `DeferredRegister`) |
| Конфиг | собственный на Gson (`PazConfig.kt`), путь берётся из `FabricLoader` |
| Локализации | 21 язык, включая `ru_ru` и `uk_ua` |
| Ресурсы | `data/c/tags` (общие теги — совместимы с NeoForge), Fabric resource conditions не используются |
| CI | GitHub Actions `build.yml` (Java 25, `./gradlew build`), `check-lang.yml` |
| Лицензия | `LICENSE` — MPL-2.0; в `fabric.mod.json` указано `CC0-1.0` (расхождение; ориентируемся на MPL-2.0) |

---

## 3. Целевой стек

| Компонент | Версия / выбор |
|---|---|
| Minecraft | 26.3 |
| NeoForge | последняя `26.3.0.x` (на момент старта — beta; зафиксировать в `gradle.properties`) |
| Gradle-плагин | ModDevGradle (MDG) **≥ 2.0.147** (более старые не настраивают NeoForge 26.3) |
| Kotlin | Kotlin for Forge (KFF) под NeoForge 26.3; версия Kotlin-плагина Gradle — **та же, что в KFF** |
| Java | 25 (toolchain) |
| TerraBlender | NeoForge-сборка под 26.3 (`compileOnly` + опциональная зависимость) |
| Iris | NeoForge-сборка, `compileOnly`, только проверка наличия |
| IDE | IntelliJ IDEA + плагин Minecraft Development (необязательно) |
| Модели | Blockbench (экспорт Java-моделей под Mojang mappings) |

Актуальные версии перед началом работ сверить на maven.neoforged.net, странице KFF и Modrinth (TerraBlender, Iris).

---

## 4. Решения, принимаемые до начала работ

| # | Решение | Рекомендация | Когда нужно |
|---|---|---|---|
| D1 | Название мода | придумать вместе с сыном | до M8 |
| D2 | **Mod id** (`[a-z0-9_]`, 2–64 символа) | короткий и уникальный, например `pazplus`, `gardenwar` | до M8, **до первого «настоящего» мира** |
| D3 | Корневой пакет для своего кода | `dev.efureev.<modid>` | до M9 |
| D4 | Лицензия своих ассетов (модели, текстуры, звуки персонажей сына) | MPL-2.0 для кода; для ассетов — MPL-2.0 или CC BY 4.0 / All Rights Reserved на усмотрение | до первой публикации |
| D5 | Схема версий | `<mc>-<major>.<minor>.<patch>`, напр. `26.3-0.1.0` | M1 |

Пока D2 не принято, во всей работе используется оригинальный id `plantz`. Смена id — отдельный этап (M8).

---

## 5. Репозиторий и процесс

### 5.1. Ветки

- `main` — основная ветка форка. Создаётся из `26.3` оригинала:
  ```bash
  git clone https://github.com/efureev/plants-and-zombies.git
  cd plants-and-zombies
  git checkout -b main origin/26.3
  git push -u origin main
  # в настройках GitHub: Default branch → main; ветку 26.3 можно удалить из форка
  git remote add upstream https://github.com/joshxviii/plants-and-zombies.git
  ```
- Рабочие ветки: `port/<этап>` для порта (`port/build`, `port/registries`, …), `feat/<персонаж>` для нового контента, `fix/<что>` для исправлений. Слияние в `main` через PR (даже в одиночку — ради истории и CI).
- Теги релизов: `v26.3-0.1.0` и т.д.

### 5.2. Синхронизация с оригиналом

- Точка форка фиксируется тегом `upstream-base-977cfb46`.
- Раз в 2–4 недели: `git fetch upstream`, просмотр новых коммитов `git log upstream/26.3 ^upstream-base-…`.
- Изменения переносятся **только через `git cherry-pick`** (полный merge невозможен после порта и смены id). Коммиты, затрагивающие Fabric API, адаптируются вручную.
- Перенесённые коммиты записываются в `docs/UPSTREAM.md` (хэш upstream → хэш в форке / «пропущен, причина»).
- Если сопровождение синхронизации становится дороже пользы — она прекращается, это нормально.

### 5.3. Коммиты

Conventional Commits: `feat:`, `fix:`, `port:`, `chore:`, `docs:`, `assets:`.

---

## 6. Этапы работ (milestones)

Каждый этап заканчивается сборкой `./gradlew build` без ошибок и проверкой по критериям приёмки.

### M0. Подготовка репозитория
- Ветка `main`, `upstream`, тег точки форка (5.1–5.2).
- В `README.md` — раздел «Это форк», ссылка на оригинал и автора, лицензия.
- Добавить `docs/SPEC.md` (этот документ), `docs/UPSTREAM.md`, `docs/CHARACTERS.md` (реестр своих персонажей, раздел 9).

**Приёмка:** `main` — ветка по умолчанию на GitHub, README обновлён.

### M1. Сборка под NeoForge
- Удалить Loom, `fabric.mod.json`, зависимости Fabric API / Fabric Kotlin / ModMenu, репозиторий Fabric в `settings.gradle`.
- Подключить MDG, NeoForge 26.3, KFF, Kotlin JVM-плагин (версия из KFF), toolchain Java 25.
- Создать `src/main/resources/META-INF/neoforge.mods.toml`: modId, version, displayName, authors (оригинальный автор + форк), license `MPL-2.0`, зависимости `neoforge`, `minecraft` `[26.3,26.4)`, `kotlinforforge`, опциональные `terrablender`, `iris`; секция `[[mixins]]` для обоих конфигов.
- **Source sets:** объединить `client` в `main` (MDG работает с одним source set). Клиентский код остаётся в отдельных пакетах `…plantz.client` / прежние пути `src/client/*` переезжают в `src/main/*`, правило — общий код никогда не импортирует клиентские классы (см. риск R4).
- Run-конфигурации: `client`, `server`, `gameTestServer`, `data` (если будет datagen).
- Обновить `.github/workflows/build.yml`: пути `src/main/**`, `neoforge.mods.toml`; кэш Gradle.
- Временный режим: всё Fabric-зависимое закомментировано / заглушено `TODO(port)`, чтобы проект собрался.

**Приёмка:** `./gradlew build` проходит; `./gradlew runClient` запускает игру с модом в списке модов (контент может ещё не работать).

### M2. Регистрация контента
- Все 43 прямых `Registry.register` / `registerForHolder` → `DeferredRegister` (блоки, предметы, сущности, блок-энтити, эффекты, зелья, меню, звуки, частицы, креативные вкладки, критерии, POI, сериализаторы данных и т.д.).
- Kotlin `object`-регистры (`PazBlocks`, `PazItems`, `PazEntities`, …) переписываются на `DeferredHolder`/`DeferredItem`/`DeferredBlock`; регистры подписываются на mod event bus в конструкторе главного класса `@Mod`.
- `FabricDefaultAttributeRegistry` → `EntityAttributeCreationEvent`.
- `FabricBlockEntityTypeBuilder` → конструктор `BlockEntityType` через `DeferredRegister`.
- `FabricEntityDataRegistry` (14 мест) → регистрация `EntityDataSerializer` в `NeoForgeRegistries.ENTITY_DATA_SERIALIZERS`.
- `FabricParticleTypes` → `SimpleParticleType` / собственный `ParticleType`.
- `DynamicRegistries` (свой datapack-реестр, напр. `seed_mutation`) → `DataPackRegistryEvent.NewRegistry`.
- Attachment `hypnotized_goal` (использует **внутренний** `AttachmentRegistryImpl` Fabric) → NeoForge `AttachmentType` через `NeoForgeRegistries.ATTACHMENT_TYPES`; продумать, нужна ли сериализация (Goal не сериализуется — сделать несохраняемым).
- `DefaultItemComponentEvents` → `ModifyDefaultComponentsEvent`.
- Порядок инициализации: убедиться, что ленивые `object` Kotlin не обращаются к реестрам до их заполнения.

**Приёмка:** в креативе есть все вкладки и предметы; все сущности спавнятся яйцами/командой `/summon`; в логе нет ошибок регистрации.

### M3. Логика, события, сеть, меню, конфиг
- Точки входа: `ModInitializer`/`ClientModInitializer` → класс `@Mod` + клиентский `@Mod(dist = Dist.CLIENT)` или `@EventBusSubscriber(Dist.CLIENT)`.
- События — по таблице раздела 7.
- Сеть: payload-классы (`CustomPacketPayload` + `StreamCodec`) сохраняются как есть; регистрация → `RegisterPayloadHandlersEvent` (`playToServer`/`playToClient`), отправка → `PacketDistributor` / `connection.send`; `PlayerLookup.level` → `level.players()`.
- Меню с доп. данными: `ExtendedMenuType`/`ExtendedMenuProvider` → `IMenuTypeExtension.create(...)` + `player.openMenu(provider, buf -> …)` (письмо, почтовый ящик, ящик сбора, машина времени).
- Команды: `CommandRegistrationCallback` → `RegisterCommandsEvent`.
- Конфиг: оставить свой Gson-конфиг, путь — `FMLPaths.CONFIGDIR`, сторона — `FMLEnvironment.dist`. Экран конфига вместо ModMenu — `IConfigScreenFactory` (если в оригинале был экран; иначе пропустить). Переход на `ModConfigSpec` — не требуется.
- Синхронизация серверного конфига с клиентом (если есть в оригинале) — сохранить через свой payload.

**Приёмка:** почта (письмо → ящик → ящик сбора) работает в одиночной игре и на выделенном сервере; команды мода выполняются; конфиг создаётся и читается из `config/`.

### M4. Мир: генерация, спавн, лут, измерение
- `BiomeModifications` (спавн зомби/гномов по биомам) → **biome modifiers** в `data/<modid>/neoforge/biome_modifier/*.json` (`neoforge:add_spawns`, `neoforge:add_features`) либо программно через `BiomeModifier`.
- Правила спавна (`SpawnPlacementsInvoker`) → `RegisterSpawnPlacementsEvent`; миксин удалить.
- `LootTableEvents` → `LootTableLoadEvent` или **global loot modifiers** (`data/<modid>/loot_modifiers/`), предпочтительно GLM.
- TerraBlender: NeoForge-версия; проверка наличия через `ModList.get().isLoaded("terrablender")`; `OverworldBiomeBuilderMixin` оставить только как fallback без TerraBlender (уточнить по коду).
- Проверить своё измерение (`data/plantz/dimension`, `dimension_type`), структуры, торговлю жителей (`villager_trade`), джукбокс-песни, зачарования, damage types — это датапак-данные, переносятся без изменений.

**Приёмка:** в новом мире зомби и гномы спавнятся в ожидаемых биомах, структуры генерируются, лут из сундуков/мобов содержит предметы мода, измерение доступно.

### M5. Клиент: рендер, модели, интерфейс
- Слои моделей: `ModelLayerRegistry` → `EntityRenderersEvent.RegisterLayerDefinitions`; рендереры → `EntityRenderersEvent.RegisterRenderers`.
- `RenderStateDataKey` (доп. данные в render state) → механизм NeoForge для render state (`RegisterRenderStateModifiersEvent` / `setRenderData` с `ContextKey`) — уточнить по актуальному API 26.3.
- `FabricLayerRenderState` в `DoorShieldItemModel` и `WrapperBakedItemModel` в `raid/BossType.kt` → эквиваленты NeoForge для кастомных моделей предметов (`RegisterSpecialModelRendererEvent` / `RegisterItemModelsEvent`) — **исследовательская задача**, при отсутствии аналога — переписать на ванильные special item models.
- **`BossType.kt` лежит в общем коде, но импортирует клиентский класс** — вынести клиентскую часть, иначе падение на выделенном сервере.
- HUD: `HudElementRegistry` → `RegisterGuiLayersEvent` (порядок относительно ванильных слоёв через `VanillaGuiLayers`).
- Частицы: `ParticleProviderRegistry` → `RegisterParticleProvidersEvent`; `ParticleGroupRegistry` → уточнить аналог.
- Экраны меню: `MenuScreens` регистрация → `RegisterMenuScreensEvent`.
- Тултипы: `ItemTooltipCallback` → `ItemTooltipEvent`; `ItemComponentTooltipProviderRegistry` → ванильный `TooltipProvider` у компонента / `ItemTooltipEvent`.
- Ресурсы клиента: `ResourceLoader` (reload listeners) → `AddClientReloadListenersEvent`.
- Клиентский тик: `ClientTickEvents` → `ClientTickEvent.Post`.
- Шейдеры/пайплайны (`PazRenderPipelines`) → регистрация через `RegisterRenderPipelinesEvent`; проверка Iris через `ModList`.

**Приёмка:** все растения, зомби, снаряды, блоки и предметы отображаются корректно (нет «фиолетово-чёрных» текстур и невидимых сущностей); HUD-элементы и экраны работают; звуки воспроизводятся.

### M6. Миксины
- Пройти по каждому миксину (раздел 8): заменить событием NeoForge, где возможно; остальные проверить, что цели совпадают в 26.3 (маппинги Mojang и там, и там — переносится как есть).
- Удалить `ExampleClientMixin`.
- `defaultRequire = 1` оставить — несработавший миксин должен ронять запуск, а не молча отключать фичу.

**Приёмка:** запуск без ошибок миксинов; функции, зависящие от миксинов (снос голов, отражение снарядов, ИИ жителей, рейды, оверлей босса, музыка рейда), работают.

### M7. Совместимость и полировка
- Iris (NeoForge) — запуск с шейдерами без падений.
- Dynamic lights: найти NeoForge-мод, который читает `assets/*/dynamiclights` (LambDynamicLights или аналог); если нет — оставить ассеты, функция просто не активна.
- JEI/EMI — рецепты мода видны (бонус, не блокер).
- Проверка на выделенном сервере (`runServer`) + клиент по сети — обязательна.

### M8. Ребрендинг (смена mod id и названия)
- Выполняется после M7, **до начала игры в «настоящем» мире**.
- `plantz` → `<modid>` (D2): `assets/plantz` → `assets/<modid>`, `data/plantz` → `data/<modid>`, все `ResourceLocation`/`Identifier` (функция `pazResource`), ключи локализации, `neoforge.mods.toml`, имена миксин-конфигов, теги, ссылки внутри JSON (рецепты, лут-таблицы, advancements, worldgen, `customPlantEntityLimits` в конфиге).
- Пакет `joshxviii.plantz` **не переименовывается** — это облегчает cherry-pick из оригинала; свой код живёт в своём пакете (D3).
- Название, иконка, описание, авторы (оригинальный автор остаётся в credits).
- Скрипт-проверка: `grep -r "plantz:" src/` не должен находить ничего, кроме намеренных мест.

**Приёмка:** мир создаётся и загружается заново, весь контент на месте; в F3/`/summon` отображается новый id.

### M9. Свои персонажи
- Инфраструктура: пакет `<D3>`, базовые хелперы, шаблон персонажа, `docs/CHARACTERS.md`.
- Первый персонаж сына — как эталон процесса (раздел 9).
- Далее — по одному персонажу на `feat/<имя>`-ветку.

### M10. Релиз 0.1
- Тег `v26.3-0.1.0`, GitHub Release с собранным `.jar`.
- `CHANGELOG.md` — отдельный раздел форка поверх истории оригинала.

---

## 7. Карта переноса Fabric → NeoForge (по файлам)

| Файл | Fabric API | Замена в NeoForge |
|---|---|---|
| `PazMain.kt` | `ModInitializer`, `ServerTickEvents`, `ServerLifecycleEvents`, `ServerBlockEntityEvents`, `ServerPlayerEvents`, `ServerPlayNetworking` | класс `@Mod`; `ServerTickEvent`; `ServerStartingEvent`/`ServerStoppedEvent`; `ChunkEvent`/`BlockEntity#onLoad`; `PlayerEvent.Clone`/`PlayerRespawnEvent`; `PacketDistributor` |
| `PazEntities.kt` | `FabricDefaultAttributeRegistry`, `ServerEntityEvents`, прочее | `EntityAttributeCreationEvent`; `EntityJoinLevelEvent`; `DeferredRegister` |
| `PazBlocks.kt` | `FabricBlockEntityTypeBuilder` и др. | `DeferredRegister.Blocks`, `BlockEntityType` |
| `PazItems.kt` | `DefaultItemComponentEvents`, `ItemComponentTooltipProviderRegistry` | `ModifyDefaultComponentsEvent`; ванильный `TooltipProvider` |
| `PazEffects.kt` | `AttachmentType`, `AttachmentRegistryImpl` (internal!) | `NeoForgeRegistries.ATTACHMENT_TYPES` |
| `PazDataSerializers.kt` | `FabricEntityDataRegistry` | `NeoForgeRegistries.ENTITY_DATA_SERIALIZERS` |
| `PazMenus.kt`, `inventory/AbstractMailboxMenu.kt` | `ExtendedMenuType` | `IMenuTypeExtension.create` |
| `block/entity/*Entity.kt`, `item/LetterItem.kt` | `ExtendedMenuProvider` | `MenuProvider` + `player.openMenu(provider, bufWriter)` |
| `PazNetwork.kt`, `networking/*Payload.kt`, `block/Mail*Block.kt` | `PayloadTypeRegistry`, `ServerPlayNetworking`, `PacketContext` | `RegisterPayloadHandlersEvent`, `IPayloadContext`, `PacketDistributor` |
| `PazLootTables.kt` | `LootTableEvents` | `LootTableLoadEvent` / global loot modifiers |
| `PazSpawnPlacements.kt` | `BiomeModifications`, `BiomeSelectionContext` | biome modifiers (JSON) + `RegisterSpawnPlacementsEvent` |
| `PazRegistries.kt` | `DynamicRegistries` | `DataPackRegistryEvent.NewRegistry` |
| `PazServerParticles.kt` | `FabricParticleTypes` | `SimpleParticleType` через `DeferredRegister` |
| `PazWorldGen.kt`, `mixin/OverworldBiomeBuilderMixin.java` | `FabricLoader.isModLoaded` | `ModList.get().isLoaded` (в миксине — `LoadingModList`) |
| `PazConfig.kt` | `FabricLoader` (config dir), `EnvType` | `FMLPaths.CONFIGDIR`, `FMLEnvironment.dist` |
| `Utils.kt` | `UseBlockCallback` | `PlayerInteractEvent.RightClickBlock` |
| `command/PazCommands.kt` | `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| `entity/zombie/Imp.kt` | `PlayerLookup.level` | `serverLevel.players()` |
| `raid/BossType.kt` | `WrapperBakedItemModel` (клиентский класс в общем коде!) | вынести в клиент; аналог модели предмета — исследовать |
| `client/PazClient.kt` | `ClientModInitializer`, `ItemTooltipCallback`, `ResourceLoader` | клиентский `@Mod(dist = CLIENT)`; `ItemTooltipEvent`; `AddClientReloadListenersEvent` |
| `client/PazClientNetwork.kt`, `gui/*Screen.kt` | `ClientPlayNetworking`, `ClientTickEvents` | `PacketDistributor.sendToServer` / `ClientPacketDistributor`; `ClientTickEvent` |
| `client/PazModels.kt` | `ModelLayerRegistry`, `RenderStateDataKey` | `EntityRenderersEvent.RegisterLayerDefinitions`; render state data NeoForge |
| `client/DoorShieldItemModel.kt` | `FabricLayerRenderState` | исследовать (special model renderer) |
| `client/PazScreens.kt` | `HudElementRegistry`, `VanillaHudElements` | `RegisterGuiLayersEvent`, `VanillaGuiLayers`; `RegisterMenuScreensEvent` |
| `client/PazParticles.kt` | `ParticleProviderRegistry`, `ParticleGroupRegistry` | `RegisterParticleProvidersEvent`; группы — исследовать |
| `client/PazRenderPipelines.kt` | `FabricLoader` (Iris) | `ModList`; `RegisterRenderPipelinesEvent` |
| `client/PazModMenu.kt` | ModMenu API | удалить; `IConfigScreenFactory` при необходимости |

Точные имена событий сверять с javadoc NeoForge 26.3 — API между версиями меняется.

---

## 8. Миксины

| Миксин | Сторона | Действие |
|---|---|---|
| `BlocksAttacksBreakChanceMixin` | общая | проверить цель, оставить |
| `EntityProjectileDeflectionMixin` | общая | проверить; возможно `ProjectileImpactEvent` |
| `LivingEntityAccessor`, `MobAccessor` | общая | оставить (accessor) или заменить на access transformer |
| `LivingEntityMixin` | общая | разобрать по инжектам; часть — `LivingIncomingDamageEvent`/`LivingDeathEvent` и т.п. |
| `LivingEntityProtectsHeadMixin` | общая | проверить, оставить |
| `OverworldBiomeBuilderMixin` | общая | `FabricLoader` → `LoadingModList`; оставить |
| `PlayerListMixin` | общая | проверить; возможно `PlayerLoggedInEvent` |
| `ServerLevelMixin` | общая | проверить; возможно `LevelTickEvent` |
| `ServerPlayerMixin` | общая | проверить |
| `SpawnPlacementsInvoker` | общая | **удалить** → `RegisterSpawnPlacementsEvent` |
| `VillagerHostilesSensorMixin` | общая | оставить |
| `ZombieMixin` | общая | проверить, оставить |
| `AvatarRendererMixin`, `HumanoidModelMixin`, `LivingEntityRendererMixin` | клиент | проверить; часть — `RenderLivingEvent` / render layers |
| `BossHealthOverlayMixin` | клиент | возможно `CustomizeGuiOverlayEvent.BossEventProgress` |
| `HudMixin` | клиент | возможно `RegisterGuiLayersEvent` / `RenderGuiLayerEvent` |
| `NowPlayingToastMixin`, `SoundManagerMixin` | клиент | проверить, оставить (музыка рейда) |
| `RangeSelectItemModelPropertiesMixin` | клиент | возможно `RegisterRangeSelectItemModelPropertyEvent` |
| `ExampleClientMixin` | клиент | **удалить** |

Правило: миксин оставляем, если события нет или оно неудобно. Переписывание на события — желательно, но не блокер для M6.

---

## 9. Свои персонажи

### 9.1. Где живёт код и ресурсы
- Код: пакет `<D3>.content.plant.<Name>`, `<D3>.content.zombie.<Name>`; регистрация — в своём `<D3>.registry.*` (отдельные `DeferredRegister`), не в файлах оригинала. Так cherry-pick из upstream не конфликтует со своим контентом.
- Ресурсы: `assets/<modid>/textures/entity/custom/<name>.png`, модели в коде, звуки `assets/<modid>/sounds/custom/<name>/`, лут `data/<modid>/loot_table/entities/<name>.json`.
- Исходники Blockbench (`.bbmodel`) — `art/<name>/` в корне репозитория (не попадают в jar), туда же рисунок-эскиз сына.

### 9.2. Карточка персонажа (`docs/CHARACTERS.md`)
Для каждого: имя (рус./англ.), id, тип (растение / зомби / другое), автор идеи, эскиз, что умеет (атака, защита, особое), стоимость в солнцах (для растений), здоровье, урон, скорость, где появляется (для зомби), как получить семя, звуки, статус.

### 9.3. Чек-лист реализации персонажа
1. Карточка заполнена и согласована «с заказчиком».
2. Модель и текстура в Blockbench, экспорт Java-модели (Mojang mappings).
3. Класс сущности: наследник `Plant` (или базового класса зомби), поведение/цели ИИ, атрибуты.
4. Регистрация: `EntityType`, атрибуты, предмет-семя / яйцо призыва, рендерер, слой модели.
5. Звуки (опционально) + `sounds.json`.
6. Локализация: `en_us` и `ru_ru` — **обязательно**.
7. Получение в игре: рецепт/лут/торговля/мутация семян (`seed_mutation`), запись в креативную вкладку.
8. Если это зомби — спавн: biome modifier и/или участие в рейдах.
9. Проверка: одиночная игра + выделенный сервер, без ошибок в логе.
10. Скриншот/GIF в `docs/characters/<name>.png`, статус «готов» в карточке.

### 9.4. Баланс
Новые персонажи балансируются относительно существующих аналогов оригинала (здоровье/урон/стоимость в пределах ±30% от ближайшего аналога), если «заказчик» явно не хочет иначе.

---

## 10. Нефункциональные требования

- **Выделенный сервер:** мод обязан запускаться на `runServer` без клиентских классов в общем коде.
- **Стабильность:** запуск без ошибок и предупреждений мода в логе; `defaultRequire = 1` для миксинов.
- **Производительность:** не хуже оригинала — 50+ растений и рейд на 30 зомби без заметного падения TPS на средней машине.
- **Сохранения:** после M8 изменения, ломающие миры (смена id сущностей/блоков/предметов), запрещены без миграции (`MissingMappingsEvent` или datafixer) и записи в CHANGELOG.
- **Локализация:** весь новый текст — минимум `en_us` и `ru_ru`; существующие переводы оригинала не удалять. `check-lang.yml` сохранить и адаптировать.
- **Код:** Kotlin для новой логики; Java — только миксины; стиль как в оригинале.

---

## 11. Тестирование

### 11.1. Автоматическое
- CI: `./gradlew build` на каждый push/PR в `main`.
- По возможности GameTest (`runGameTestServer`) для ключевых механик: посадка семени → рост → атака растения; почтовая система.

### 11.2. Ручной чек-лист (перед каждым релизом)
- [ ] Новый мир создаётся, мод в списке модов, креативные вкладки на месте.
- [ ] Каждое растение: посадка, рост, атака/способность, сломанная модель/текстура — нет.
- [ ] Каждый зомби: `/summon`, естественный спавн, поведение, лут.
- [ ] Рейд: запуск, боссы, полоса босса, музыка.
- [ ] Почта: письмо → ящик → ящик сбора.
- [ ] Машина времени, блюпринт-машины, горшки, вагонетка с горшком.
- [ ] Измерение и структуры мода.
- [ ] Торговля жителей, зачарования, эффекты, зелья.
- [ ] Конфиг: изменение значения влияет на игру.
- [ ] Выделенный сервер + подключение клиента.
- [ ] Iris с шейдерами — без падения.
- [ ] Русская локализация отображается.

---

## 12. Риски

| # | Риск | Влияние | Митигация |
|---|---|---|---|
| R1 | NeoForge 26.3 в статусе beta, API меняется | ломающиеся обновления | зафиксировать версию; обновлять осознанно |
| R2 | Нет прямого аналога Fabric-API для кастомных моделей предметов (`FabricLayerRenderState`, `WrapperBakedItemModel`), групп частиц | задержка M5 | ванильные special item models; упростить эффект |
| R3 | Порядок инициализации Kotlin `object` + `DeferredRegister` | падения при старте | только `DeferredHolder`, никаких `.get()` в инициализаторах |
| R4 | Клиентские классы в общем коде (минимум `BossType.kt`) | падение сервера | проверка через `runServer` в CI/вручную |
| R5 | Версия Kotlin-плагина ≠ Kotlin в KFF | ошибки компиляции/рантайма | брать версию из KFF |
| R6 | Upstream активно развивается (коммиты почти ежедневно) | cherry-pick дорожает | регулярность, `docs/UPSTREAM.md`, право прекратить синхронизацию |
| R7 | Бренд Plants vs. Zombies принадлежит PopCap/EA | риск при публичном распространении | см. раздел 13 |
| R8 | TerraBlender/Iris под NeoForge 26.3 отстают | нет совместимости | зависимости опциональные, мод работает без них |

---

## 13. Лицензия и атрибуция

- Код оригинала — **MPL-2.0**. Изменённые файлы оригинала остаются под MPL-2.0, их исходники открыты (репозиторий публичный — условие выполнено). Лицензионные уведомления не удалять.
- Новые файлы (свой контент) — по решению D4; рекомендуется MPL-2.0 для единообразия.
- В `README.md` и `neoforge.mods.toml` (credits): «Based on Plants & Zombies by joshxviii, licensed under MPL-2.0» со ссылкой.
- Расхождение `CC0-1.0` в `fabric.mod.json` оригинала не используется как основание: ориентир — файл `LICENSE`.
- При публикации на площадках: собственное название (D1), явная пометка «неофициальный форк», без использования логотипа и названия оригинального мода как своего. Бренд Plants vs. Zombies — собственность PopCap/EA; риск такой же, как у оригинала.
- Документ не является юридической консультацией.

---

## 14. Порядок и оценка

| Этап | Зависит от | Оценка (вечера) |
|---|---|---|
| M0 Подготовка репозитория | — | 0.5 |
| M1 Сборка | M0 | 1–2 |
| M2 Регистрация | M1 | 2–3 |
| M3 Логика, сеть, меню, конфиг | M2 | 2–3 |
| M4 Мир | M2 | 1–2 |
| M5 Клиент | M2 | 3–4 |
| M6 Миксины | M1 | 1–2 |
| M7 Совместимость | M3–M6 | 1 |
| M8 Ребрендинг | M7, D1, D2 | 1 |
| M9 Первый свой персонаж | M8 | 1–2 |
| M10 Релиз 0.1 | M9 | 0.5 |

Итого до играбельной версии со своим id (M0–M8): **~13–20 вечеров**. Дальше — по 1–2 вечера на персонажа.

### Определение готовности версии 0.1
Все пункты ручного чек-листа (11.2) пройдены, свой mod id, хотя бы один персонаж сына в игре, сборка в GitHub Release.
