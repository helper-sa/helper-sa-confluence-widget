package ru.sahelper.confluence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesEmbedAnswer() {
        EmbedInfo info = EmbedInfo.fromJson("{\"tool\":\"dbml\",\"link\":\"version\",\"name\":\"Схема \\\"БД\\\"\",\"source\":\"Table a {\\n  id int\\n}\\u0021\","
                + "\"authorName\":\"Анна\",\"updatedAt\":\"2026-10-15T09:30:00Z\",\"versionNumber\":3,\"image\":true,\"extra\":[1,2.5,null,{}]}");
        assertEquals("Схема \"БД\"", info.name());
        assertEquals("Table a {\n  id int\n}!", info.source());
        assertEquals(3, info.versionNumber());
        assertTrue(info.image());
        assertTrue(info.versionLink());
        assertEquals(2026, info.updatedAt().getYear());

        EmbedInfo minimal = EmbedInfo.fromJson("{\"name\":\"x\",\"source\":\"\",\"versionNumber\":null}");
        assertNull(minimal.versionNumber());
        assertFalse(minimal.image());
        assertFalse(minimal.versionLink());
    }

    @Test
    void parsesValuesAndRejectsGarbage() {
        assertEquals(List.of(1L, -2.5e1, true), Json.parse(" [1, -2.5e1, true] "));
        assertEquals(Map.of(), Json.parse("{}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\":1"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("<html>"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} x"));
    }
}
