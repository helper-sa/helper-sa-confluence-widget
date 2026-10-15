package ru.sahelper.confluence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class WidgetHtmlTest {

    private static final ShareLink LINK = ShareLink.parse("https://helper-sa.ru/shared/bpmn/tok").orElseThrow();
    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    @Test
    void widgetHasThumbnailLinkAndEscapedSource() {
        EmbedInfo info = new EmbedInfo("bpmn", false, "<Процесс>", "<definitions/>", "Анна", OffsetDateTime.parse("2026-10-15T09:30:00Z"),
                2, true);
        String html = WidgetHtml.widget(LINK, info, 2, 320, WidgetHtml.plainCode(info.source()), MSK);

        assertTrue(html.contains("href=\"https://helper-sa.ru/shared/bpmn/tok\""));
        assertTrue(html.contains("&lt;Процесс&gt;"));
        assertFalse(html.contains("<Процесс>"));
        assertTrue(html.contains("<img src=\"https://helper-sa.ru/api/v1/embed/bpmn/tok/image?version=2\""));
        assertFalse(html.contains("helper-sa-widget__unpinned"));
        assertTrue(html.contains("width: 320px"));
        assertTrue(html.contains("<details class=\"helper-sa-widget__source\"><summary>Исходный код</summary><pre"));
        assertTrue(html.contains("&lt;definitions/&gt;"));
        assertTrue(html.contains("BPMN · версия 2 · Анна · 15.10.2026 12:30"));
    }

    @Test
    void missingImageIsExplainedForImageTools() {
        EmbedInfo info = new EmbedInfo("bpmn", true, "P", "x", "", null, 1, false);
        assertTrue(WidgetHtml.widget(LINK, info, null, 600, "", MSK).contains("Картинка ещё не сформирована"));
        ShareLink yaml = ShareLink.parse("https://helper-sa.ru/shared/yaml/tok").orElseThrow();
        assertFalse(WidgetHtml.widget(yaml, info, null, 600, "", MSK).contains("helper-sa-widget__no-image"));
    }

    @Test
    void documentLinkWithoutPinnedVersionShowsCurrentWithHint() {
        EmbedInfo info = new EmbedInfo("bpmn", false, "P", "x", "", OffsetDateTime.parse("2026-10-15T09:30:00Z"), 4, true);
        String html = WidgetHtml.widget(LINK, info, null, 600, "", MSK);
        assertTrue(html.contains("Версия не закреплена"));
        assertTrue(html.contains("/image?v="));
        EmbedInfo version = new EmbedInfo("bpmn", true, "P", "x", "", null, 4, true);
        assertFalse(WidgetHtml.widget(LINK, version, null, 600, "", MSK).contains("Версия не закреплена"));
    }

    @Test
    void widthAndLanguageAndCodeStorage() {
        assertEquals(600, WidgetHtml.width(null));
        assertEquals(600, WidgetHtml.width("abc"));
        assertEquals(600, WidgetHtml.width("10"));
        assertEquals(250, WidgetHtml.width(" 250 "));
        assertEquals(3, WidgetHtml.version(" 3 "));
        assertEquals(null, WidgetHtml.version(""));
        assertEquals(null, WidgetHtml.version("0"));
        assertEquals("yaml", WidgetHtml.language("openapi", "openapi: 3.1.0"));
        assertEquals("js", WidgetHtml.language("openapi", " {\"openapi\":\"3.1.0\"}"));
        assertEquals("text", WidgetHtml.language("plantuml", "@startuml"));
        assertTrue(WidgetHtml.codeStorage("xml", "a]]>b").contains("<![CDATA[a]]]]><![CDATA[>b]]>"));
    }

    @Test
    void allowedOriginsAreNormalized() {
        WidgetSettings.Parsed parsed = WidgetSettings.parse("https://Helper-SA.ru/\n\nhttp://localhost:8080, https://x.ru/path\nnot a url");
        assertEquals(java.util.List.of("https://helper-sa.ru", "http://localhost:8080"), parsed.origins);
        assertEquals(java.util.List.of("https://x.ru/path", "not a url"), parsed.invalid);
    }
}
