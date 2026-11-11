<p align="center">
  <img src="../images/logo.png" alt="Sprout Squad" width="600">
</p>

<p align="center">Мод для Minecraft 26.3 на <b>NeoForge</b>: растения защищают базу от зомби, а отряд пополняется собственными персонажами.</p>

<p align="center"><a href="../../README.md">English</a> · <b>Русский</b></p>

> Неофициальный форк мода [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) от **joshxviii**, перенесённый с Fabric на NeoForge.
> Unofficial NeoForge fork of Plants & Zombies by joshxviii, licensed under MPL-2.0.

## Статус

Перенос на NeoForge и переименование в Sprout Squad выполнены, идёт проверка в игре.

Что форк добавляет к оригиналу:

- **Починка того, что нельзя было получить или использовать**: боевые машины из чертежей воюют за игрока, газонокосилка работает как последний рубеж, Squash атакует, зелья варятся, у рейдов и мобов есть лут, зомби-козёл появляется в горах, за полёт на шариках больше не кикает ([полный список](UPSTREAM.md)).
- **Собственные персонажи**, первый — растение-маскот Горшочек ([персонажи](CHARACTERS.md)).

| | |
|---|---|
| Minecraft | 26.3 |
| Загрузчик | NeoForge 26.3.x |
| Зависимости | Kotlin for Forge; опционально TerraBlender, Iris |
| Java | 25 |

## Сборка

```bash
./gradlew build              # jar появится в build/libs/ (sprout-squad-<версия>.jar)
./gradlew runClient          # запуск клиента для проверки
./gradlew runServer          # запуск выделенного сервера
./gradlew runGameTestServer  # загрузка мода и его GameTest-ы, потом выход
```

Нужен JDK 25.

Отладка (оператор сервера, уровень 2):

```text
/pz gallery           # выставить всех растений, зомби и их варианты в ряд перед собой (с подписями)
/pz gallery false     # то же без подписей — для скриншотов
/pz gallery clear     # убрать галерею (подставки остаются)
```

## Документация

Основная версия документации — английская ([English](../../README.md#documentation)); здесь — русская.

- [CHARACTERS](CHARACTERS.md) — собственные персонажи
- [WORKSHOP](WORKSHOP.md) — как добавить персонажа, гайд по стилю
- [UPSTREAM](UPSTREAM.md) — изменения из оригинала и исправленные в нём ошибки

## Лицензия и авторство

Код основан на [Plants & Zombies](https://github.com/joshxviii/plants-and-zombies) (© joshxviii) и распространяется под лицензией [Mozilla Public License 2.0](../../LICENSE). Файлы оригинала, изменённые в этом форке, остаются под MPL-2.0.

Plants vs. Zombies — товарный знак PopCap Games / Electronic Arts. Этот проект с ними не связан.
