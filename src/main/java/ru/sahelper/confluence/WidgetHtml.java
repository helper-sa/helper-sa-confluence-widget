package ru.sahelper.confluence;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/** HTML виджета и сообщений макроса (всё, что пришло извне, экранируется). */
public final class WidgetHtml {

    public static final int DEFAULT_WIDTH = 600;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Инструменты с картинкой: PlantUML рисует сервер helper-sa, BPMN и DBML — браузер автора. */
    private static final Set<String> IMAGE_TOOLS = Set.of("plantuml", "bpmn", "dbml");

    private static final Map<String, String> TOOL_NAMES = Map.ofEntries(
            Map.entry("dbml", "DBML"), Map.entry("plantuml", "PlantUML"), Map.entry("openapi", "OpenAPI"),
            Map.entry("asyncapi", "AsyncAPI"), Map.entry("bpmn", "BPMN"), Map.entry("xsd", "XSD"), Map.entry("wsdl", "WSDL"),
            Map.entry("xml", "XML"), Map.entry("jsonschema", "JSON Schema"), Map.entry("yaml", "YAML"), Map.entry("json", "JSON"),
            Map.entry("markdown", "Markdown"), Map.entry("asciidoc", "AsciiDoc"));

    /** Язык подсветки макроса «Код» Confluence. */
    private static final Map<String, String> LANGUAGES = Map.ofEntries(
            Map.entry("dbml", "sql"), Map.entry("bpmn", "xml"), Map.entry("xsd", "xml"), Map.entry("wsdl", "xml"),
            Map.entry("xml", "xml"), Map.entry("jsonschema", "js"), Map.entry("json", "js"), Map.entry("yaml", "yaml"));

    private WidgetHtml() {
    }

    /** Ширина миниатюры из параметра макроса: число 50–4000, иначе — по умолчанию. */
    public static int width(String value) {
        try {
            int w = Integer.parseInt(value == null ? "" : value.trim());
            return w >= 50 && w <= 4000 ? w : DEFAULT_WIDTH;
        } catch (NumberFormatException e) {
            return DEFAULT_WIDTH;
        }
    }

    /** Номер закреплённой версии из параметра макроса; пусто или не число — null (текущая). */
    public static Integer version(String value) {
        try {
            int v = Integer.parseInt(value == null ? "" : value.trim());
            return v >= 1 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Язык подсветки исходного кода; OpenAPI/AsyncAPI — YAML или JSON по содержимому. */
    public static String language(String tool, String source) {
        if ("openapi".equals(tool) || "asyncapi".equals(tool)) {
            return source.trim().startsWith("{") ? "js" : "yaml";
        }
        return LANGUAGES.getOrDefault(tool, "text");
    }

    /** Исходный код как разметка хранения макроса «Код» (CDATA с разбиением «]]>»). */
    public static String codeStorage(String tool, String source) {
        return "<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">" + language(tool, source)
                + "</ac:parameter><ac:parameter ac:name=\"linenumbers\">true</ac:parameter><ac:plain-text-body><![CDATA["
                + source.replace("]]>", "]]]]><![CDATA[>") + "]]></ac:plain-text-body></ac:structured-macro>";
    }

    /** Исходный код без макроса «Код» (если его не удалось отрисовать). */
    public static String plainCode(String source) {
        return "<pre class=\"helper-sa-widget__code\">" + escape(source) + "</pre>";
    }

    /**
     * Виджет: заголовок со ссылкой на источник, картинка (миниатюра заданной ширины; щелчок — полный размер)
     * и аккордеон с исходным кодом (codeHtml — уже отрисованный блок кода).
     */
    public static String widget(ShareLink link, EmbedInfo info, Integer pinned, int width, String codeHtml, ZoneId zone) {
        StringBuilder html = new StringBuilder("<div class=\"helper-sa-widget\">");
        html.append("<div class=\"helper-sa-widget__header\">")
                .append("<a class=\"helper-sa-widget__title\" href=\"").append(escape(link.url()))
                .append("\" target=\"_blank\" rel=\"noopener noreferrer\">").append(escape(info.name())).append("</a>")
                .append("<span class=\"helper-sa-widget__meta\">").append(escape(meta(link, info, zone))).append("</span>")
                .append("</div>");
        if (!info.versionLink() && pinned == null) {
            html.append("<div class=\"helper-sa-widget__unpinned\">Версия не закреплена — показана текущая. ")
                    .append("Откройте параметры макроса и сохраните их, чтобы закрепить версию ")
                    .append(info.versionNumber() == null ? "" : String.valueOf(info.versionNumber())).append(".</div>");
        }
        if (info.image()) {
            String image = link.imageUrl(pinned);
            if (pinned == null && info.updatedAt() != null) {
                // без закреплённой версии: после правки документа браузер не покажет старую картинку из кеша
                image += "?v=" + info.updatedAt().toInstant().toEpochMilli();
            }
            html.append("<div class=\"helper-sa-widget__image\"><a href=\"").append(escape(image))
                    .append("\" target=\"_blank\" rel=\"noopener noreferrer\" title=\"Открыть в полном размере\">")
                    .append("<img src=\"").append(escape(image)).append("\" alt=\"").append(escape(info.name()))
                    .append("\" style=\"width: ").append(width).append("px; max-width: 100%; height: auto;\"/></a></div>");
        } else if (IMAGE_TOOLS.contains(link.tool())) {
            html.append("<div class=\"helper-sa-widget__no-image\">")
                    .append("plantuml".equals(link.tool())
                            ? "Картинку не удалось построить: проверьте синтаксис диаграммы в helper-sa."
                            : "Картинка ещё не сформирована: откройте документ в helper-sa — она построится автоматически.")
                    .append("</div>");
        }
        if (!info.source().isEmpty()) {
            html.append("<details class=\"helper-sa-widget__source\"><summary>Исходный код</summary>")
                    .append(codeHtml).append("</details>");
        }
        return html.append("</div>").toString();
    }

    static String meta(ShareLink link, EmbedInfo info, ZoneId zone) {
        StringBuilder meta = new StringBuilder(TOOL_NAMES.getOrDefault(link.tool(), link.tool()));
        if (info.versionNumber() != null) {
            meta.append(" · версия ").append(info.versionNumber());
        }
        if (!info.authorName().isEmpty()) {
            meta.append(" · ").append(info.authorName());
        }
        if (info.updatedAt() != null) {
            meta.append(" · ").append(DATE.format(info.updatedAt().atZoneSameInstant(zone)));
        }
        return meta.toString();
    }

    /** Сообщение вместо виджета (неверная ссылка, сайт не разрешён, документ недоступен). */
    public static String message(String kind, String title, String text) {
        return "<div class=\"aui-message aui-message-" + kind + " helper-sa-widget__message\"><p class=\"title\"><strong>"
                + escape(title) + "</strong></p><p>" + escape(text) + "</p></div>";
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
}
