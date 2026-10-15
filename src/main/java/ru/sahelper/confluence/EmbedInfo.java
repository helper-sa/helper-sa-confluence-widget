package ru.sahelper.confluence;

import java.time.OffsetDateTime;
import java.util.Map;

/** Документ или версия по ссылке — ответ {@code GET /api/v1/embed/<tool>/<token>} helper-sa. */
public final class EmbedInfo {

    private final String tool;
    private final boolean versionLink;
    private final String name;
    private final String source;
    private final String authorName;
    private final OffsetDateTime updatedAt;
    private final Integer versionNumber;
    private final boolean image;

    public EmbedInfo(String tool, boolean versionLink, String name, String source, String authorName, OffsetDateTime updatedAt,
                     Integer versionNumber, boolean image) {
        this.tool = tool;
        this.versionLink = versionLink;
        this.name = name;
        this.source = source;
        this.authorName = authorName;
        this.updatedAt = updatedAt;
        this.versionNumber = versionNumber;
        this.image = image;
    }

    static EmbedInfo fromJson(String json) {
        Object parsed = Json.parse(json);
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("JSON object expected");
        }
        Map<?, ?> m = (Map<?, ?>) parsed;
        Object updated = m.get("updatedAt");
        Object version = m.get("versionNumber");
        return new EmbedInfo(text(m.get("tool")), "version".equals(m.get("link")), text(m.get("name")), text(m.get("source")), text(m.get("authorName")),
                updated instanceof String ? parseDate((String) updated) : null,
                version instanceof Number ? ((Number) version).intValue() : null,
                Boolean.TRUE.equals(m.get("image")));
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    private static OffsetDateTime parseDate(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public String tool() {
        return tool;
    }

    /** Ссылка на версию (иначе — на документ: версию можно закрепить). */
    public boolean versionLink() {
        return versionLink;
    }

    public String name() {
        return name;
    }

    public String source() {
        return source;
    }

    public String authorName() {
        return authorName;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }

    public Integer versionNumber() {
        return versionNumber;
    }

    public boolean image() {
        return image;
    }
}
