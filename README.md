<p align="center">
  <img src="docs/images/logo.png" alt="Sprout Squad" width="600">
</p>

<p align="center">Мод для Minecraft 26.3 на <b>NeoForge</b>: растения защищают базу от зомби, а отряд пополняется собственными персонажами.</p>

> Неофициальный форк мода [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) от **joshxviii**, перенесённый с Fabric на NeoForge.
> Unofficial NeoForge fork of Plants & Zombies by joshxviii, licensed under MPL-2.0.

## Статус

Перенос на NeoForge и переименование в Sprout Squad выполнены, идёт проверка в игре. План работ и этапы — в [`docs/SPEC.md`](docs/SPEC.md).

| | |
|---|---|
| Minecraft | 26.3 |
| Загрузчик | NeoForge 26.3.x |
| Зависимости | Kotlin for Forge; опционально TerraBlender, Iris |
| Java | 25 |

## Сборка

```bash
./gradlew build        # jar появится в build/libs/ (sprout-squad-<версия>.jar)
./gradlew runClient    # запуск клиента для проверки
./gradlew runServer    # запуск выделенного сервера
```

Нужен JDK 25.

## Документация

- [`docs/SPEC.md`](docs/SPEC.md) — техническое задание и этапы
- [`docs/CHARACTERS.md`](docs/CHARACTERS.md) — собственные персонажи
- [`docs/UPSTREAM.md`](docs/UPSTREAM.md) — изменения, перенесённые из оригинала

## Лицензия и авторство

Код основан на [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) (© joshxviii) и распространяется под лицензией [Mozilla Public License 2.0](LICENSE). Файлы оригинала, изменённые в этом форке, остаются под MPL-2.0.

Plants vs. Zombies — товарный знак PopCap Games / Electronic Arts. Этот проект с ними не связан.
