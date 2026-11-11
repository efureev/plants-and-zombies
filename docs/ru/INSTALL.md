# Установка Sprout Squad

Как поставить мод в игру, играть вместе на сервере, обновить и удалить его. English version: [docs/en/INSTALL.md](../en/INSTALL.md).

- [Что понадобится](#что-понадобится)
- [Официальный лаунчер Minecraft](#официальный-лаунчер-minecraft)
- [Prism Launcher](#prism-launcher)
- [Сервер для игры вместе](#сервер-для-игры-вместе)
- [Обновление и удаление](#обновление-и-удаление)
- [Если что-то не так](#если-что-то-не-так)

## Что понадобится

| Что | Версия | Где взять |
|---|---|---|
| Minecraft: Java Edition | 26.3 | лицензионная копия и [лаунчер Minecraft](https://www.minecraft.net/download) |
| NeoForge | 26.3.0.102 или новее (26.3.x) | [neoforged.net](https://neoforged.net/) |
| Kotlin for Forge | 6.5.0 или новее, сборка для NeoForge 26.3 | [Modrinth](https://modrinth.com/mod/kotlin-for-forge) или [CurseForge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) |
| Sprout Squad | последний выпуск | [GitHub Releases](https://github.com/efureev/plants-and-zombies/releases): `sprout-squad-<версия>.jar` |
| TerraBlender | по желанию | [Modrinth](https://modrinth.com/mod/terrablender) |

- **Kotlin for Forge обязателен**: мод написан на Kotlin, и без этой библиотеки игра останавливается на экране «не хватает зависимости».
- **TerraBlender — по желанию.** Биом «Кладбище» появляется и без него; с ним Кладбище делит обычный мир с другими модами на биомы, которые используют TerraBlender.
- Мод нужен **и на клиенте, и на сервере**: игрок без мода не зайдёт на сервер с модом, и наоборот.
- Sprout Squad уже содержит Plants & Zombies: оригинальный мод рядом с ним не ставьте (он сделан для Fabric, и они будут конфликтовать).

## Официальный лаунчер Minecraft

1. В лаунчере один раз запустите Minecraft **26.3** без модов и закройте: лаунчер скачает игру.
2. Скачайте **установщик NeoForge** для 26.3 с [neoforged.net](https://neoforged.net/) и запустите его (двойным щелчком или `java -jar neoforge-<версия>-installer.jar`). Выберите **Install client** и нажмите OK.
3. В лаунчере появится установка **NeoForge**. Запустите её один раз и закройте игру: так появится папка `mods`.
4. Откройте папку игры и в ней папку `mods`:
   - Windows: `%APPDATA%\.minecraft\mods` (вставьте в адресную строку Проводника);
   - macOS: `~/Library/Application Support/minecraft/mods` (в Finder: Переход → Переход к папке);
   - Linux: `~/.minecraft/mods`.
5. Положите в `mods`: `sprout-squad-<версия>.jar`, jar Kotlin for Forge и, если хотите, TerraBlender.
6. Запустите установку **NeoForge**. В главном меню в разделе **Моды** будут Sprout Squad и Kotlin for Forge.

## Prism Launcher

[Prism Launcher](https://prismlauncher.org/) держит каждую сборку с модами в своём экземпляре, отдельно от обычной игры.

1. **Добавить экземпляр** → Minecraft **26.3**, загрузчик модов **NeoForge** (26.3.0.102 или новее) → OK.
2. **Изменить** экземпляр → **Моды** → **Добавить файл** → выберите `sprout-squad-<версия>.jar`.
3. В том же окне **Скачать моды** → найдите **Kotlin for Forge** (и TerraBlender, если хотите) → просмотрите и подтвердите.
4. **Запустить**.

## Сервер для игры вместе

Дома проще всего в меню паузы одиночного мира нажать **Открыть для сети**: остальные в той же сети заходят через **Сетевую игру**. Моды у всех должны быть одинаковые.

Выделенный сервер:

1. Установите **Java 25** (она нужна Minecraft 26.3), например [Eclipse Temurin](https://adoptium.net/).
2. Запустите тот же установщик NeoForge и выберите **Install server** в пустую папку (или `java -jar neoforge-<версия>-installer.jar --installServer`).
3. Запустите сервер один раз через `run.bat` (Windows) или `./run.sh` (macOS, Linux). Он остановится и попросит принять EULA: поставьте `eula=true` в файле `eula.txt`.
4. Положите в папку `mods` сервера тот же `sprout-squad-<версия>.jar` и Kotlin for Forge (и TerraBlender, если он есть у игроков).
5. Запустите сервер снова. Игроки подключаются через **Сетевая игра → Добавить сервер**; версии модов у них должны совпадать с серверными.

Настройки сервера — в `config/sproutsquad/server.json` (описаны в [FEATURES](FEATURES.md#настройки-сервера-configsproutsquadserverjson)); машина времени и Гномий космос работают только с включённым `enableExperimentalFeatures`.

## Обновление и удаление

- **Обновление:** сделайте резервную копию миров (папка `saves`), удалите старый `sprout-squad-*.jar` из `mods` и положите новый. В папке должен остаться один jar Sprout Squad. Сервер и всех игроков обновляйте вместе.
- **Удаление:** удалите jar. Мир, в который играли с модом, потеряет его растения, зомби, блоки и предметы; игра предупредит об этом при открытии мира. Если может захотеться вернуться, сохраните резервную копию.

## Если что-то не так

| Что видно | Что делать |
|---|---|
| Экран о недостающей зависимости `kotlinforforge` | Поставьте Kotlin for Forge 6.5.0 или новее, сборку для NeoForge 26.3 |
| Экран о том, что моду нужна другая версия Minecraft или NeoForge | Мод — для Minecraft 26.3 и NeoForge 26.3.0.102 или новее: проверьте версию установки в лаунчере |
| «Повторяющиеся моды» или два Sprout Squad | Оставьте в `mods` один `sprout-squad-*.jar`, остальные удалите |
| Не пускает на сервер: моды не совпадают | Поставьте те же версии Sprout Squad и Kotlin for Forge, что и на сервере |
| Игра вылетает | Подробности — в `logs/latest.log` и `crash-reports/` в папке игры. Откройте [issue](https://github.com/efureev/plants-and-zombies/issues) и приложите лог |

Что мод умеет, когда он уже в игре: [FEATURES](FEATURES.md).
