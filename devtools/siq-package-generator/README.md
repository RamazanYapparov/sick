# SIQ Package Generator

Отдельный dev-инструмент для создания тестовых `.siq` через официальную
библиотеку `SIPackages`. Он не включён в Gradle settings, не используется
приложением и не попадает в production-сборку.

## Первый запуск

Из корня репозитория:

```bash
./devtools/siq-package-generator/bootstrap.sh
./devtools/siq-package-generator/siq-tool generate \
  devtools/siq-package-generator/cases/basic.json
```

SDK устанавливается локально в `.dotnet/`, а NuGet-зависимости — в `.nuget/`.
Сгенерированные пакеты находятся в `artifacts/`. SDK, зависимости, медиа и
готовые пакеты игнорируются Git.

## Медиа и рецепты

Положите файлы в `media/`, обычно в подпапки `images/`, `audio/`, `video/` и
`html/`. В рецепте используйте относительный путь:

```json
{
  "type": "audio",
  "file": "audio/question.mp3",
  "waitForFinish": true
}
```

Примеры находятся в `cases/media.example.json` и
`cases/special-questions.example.json`, а допустимые поля — в
`recipe.schema.json`. `parameters` поддерживает типы `simple`, `content`,
`group` и `numberSet`, поэтому рецепт может описывать секретные вопросы,
варианты ответа и другие параметры SIQ. Для каждого тестового кейса создавайте
отдельный JSON в `cases/`. Стабильные рецепты можно коммитить; медиа и готовые
`.siq` остаются локальными.

## Команды

```bash
# Создать пакет; существующий файл не перезаписывается без --force
./devtools/siq-package-generator/siq-tool generate \
  devtools/siq-package-generator/cases/basic.json

# Использовать другой каталог медиа и путь результата
./devtools/siq-package-generator/siq-tool generate case.json \
  --media-root /path/to/media --output /tmp/case.siq --force

# Открыть пакет официальной библиотекой и проверить структуру
./devtools/siq-package-generator/siq-tool validate \
  devtools/siq-package-generator/artifacts/package.siq

# Показать доступные локальные медиа
./devtools/siq-package-generator/siq-tool list-media
```

Генератор отклоняет абсолютные и выходящие из `media/` пути, неизвестные поля
JSON, отсутствующие ответы и коллизии имён файлов внутри пакета.

## Codex skill

Project-specific skill хранится в
`skills/create-siq-test-package/` и учит Codex превращать описание тестового
сценария в минимальный рецепт, выбирать локальные медиа, генерировать пакет и
проверять его в приложении.

Локальная установка находится в
`~/.codex/skills/create-siq-test-package/`. В новой задаче skill можно вызвать
явно:

```text
$create-siq-test-package создай пакет для проверки завершения аудио
```
