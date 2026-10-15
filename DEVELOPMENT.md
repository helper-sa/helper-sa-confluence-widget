# Разработка виджета

P2-плагин Confluence (Atlassian SDK / AMPS, Spring Scanner). Пользовательская инструкция — [README.md](README.md).

## Сборка и проверка

Нужны JDK 11/17 и Maven 3.9; зависимости — из публичного репозитория Atlassian (первая сборка долгая).

```bash
mvn package            # target/helper-sa-confluence-widget-<версия>.jar — файл для Releases
mvn test               # юнит-тесты: разбор ссылки и JSON, HTML виджета, настройки
mvn confluence:run     # локальный Confluence с плагином, http://localhost:1990/confluence
```

## Как работает

```
Окно параметров ── GET /plugins/servlet/helper-sa/resolve?url=… ──▶ Confluence ──▶ helper-sa   (текущая версия → параметр version)
Confluence     ── GET /api/v1/embed/<tool>/<token>[?version=N] ──▶ helper-sa   (название, код, автор, признак картинки; кеш 1 мин)
Браузер        ── GET /api/v1/embed/<tool>/<token>/image[?version=N] ──▶ helper-sa   (SVG)
```

- Параметры макроса: `url` (ссылка «Поделиться»), `width` (50–4000 px, по умолчанию 600),
  `version` (номер закреплённой версии для ссылки на документ).
- Закрепление: `js/macro-browser.js` при нажатии «Вставить»/«Сохранить» (`beforeParamsRetrieved`) синхронно
  запрашивает текущую версию через `ResolveServlet` и подставляет её в `version`, если ссылка новая или поле пустое.
  Без версии (макрос вставлен не через окно параметров) виджет показывает текущую версию с подсказкой.
- Версии в helper-sa хранятся ограниченно (по умолчанию 50 последних): удалённая версия — сообщение «Документ недоступен».
- Сервер Confluence обращается только к сайтам из списка администратора (защита от запросов к внутренней сети),
  без перенаправлений; используется прокси JVM (`-Dhttps.proxyHost=… -Dhttps.proxyPort=…`), если задан.

| Файл | Назначение |
|------|------------|
| `HelperSaMacro` | макрос: проверка ссылки и сайта, запрос, вывод виджета |
| `ResolveServlet`, `js/macro-browser.js` | закрепление текущей версии при вставке |
| `ShareLink` | разбор ссылки «Поделиться», адреса API и картинки |
| `EmbedClient` | запрос к helper-sa с кешем, без перенаправлений |
| `Json`, `EmbedInfo` | разбор ответа без внешних библиотек |
| `WidgetHtml` | HTML виджета и сообщений (с экранированием) |
| `WidgetSettings`, `AdminServlet` | разрешённые сайты (SAL PluginSettings), страница администратора с XSRF-защитой |

Контракт API — `docs/api/openapi.yaml` (тег `Embed`) и процесс `docs/uml/26-embed.md` в репозитории helper-sa.

## Совместимость

По умолчанию — Confluence Data Center 8.5 LTS (Platform 6: AMPS 8.17, Spring Scanner 2, Java 11).

| Confluence | `confluence.version` | `amps.version` | `spring.scanner.version` |
|------------|----------------------|----------------|--------------------------|
| 7.19 – 8.x | 8.5.x (по умолчанию) | 8.17.x | 2.2.x |
| 9.x / 10.x | версия продукта | 9.x | из шаблона `atlas-create-confluence-plugin` этой версии |

Для Confluence 10 также `javax.*` → `jakarta.*` в сервлетах (Jakarta EE).

## Скриншоты README

`docs/widget-*.png` — макет страницы с настоящим CSS виджета и SVG PlantUML, снятый Playwright.
При изменении вида виджета их стоит переснять.
