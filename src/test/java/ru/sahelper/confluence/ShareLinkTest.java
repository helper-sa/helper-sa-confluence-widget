package ru.sahelper.confluence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShareLinkTest {

    @Test
    void parsesDocumentAndVersionLinks() {
        ShareLink link = ShareLink.parse(" https://Helper-SA.ru:443/shared/plantuml/AbC_12-x ").orElseThrow();
        assertEquals("https://helper-sa.ru", link.origin());
        assertEquals("plantuml", link.tool());
        assertEquals("AbC_12-x", link.token());
        assertEquals("https://helper-sa.ru/shared/plantuml/AbC_12-x", link.url());
        assertEquals("https://helper-sa.ru/api/v1/embed/plantuml/AbC_12-x/image", link.imageUrl(null));
        assertEquals("https://helper-sa.ru/api/v1/embed/plantuml/AbC_12-x?version=3", link.apiUrl(3));
        assertEquals("https://helper-sa.ru/api/v1/embed/plantuml/AbC_12-x/image?version=3", link.imageUrl(3));
        assertEquals("http://localhost:8080", ShareLink.parse("http://localhost:8080/shared/bpmn/t?x=1").orElseThrow().origin());
    }

    @Test
    void rejectsOtherLinks() {
        for (String value : new String[] {null, "", "helper-sa.ru/shared/bpmn/t", "ftp://helper-sa.ru/shared/bpmn/t",
                "https://helper-sa.ru/tools/bpmn/123", "https://helper-sa.ru/shared/project/tok",
                "https://helper-sa.ru/shared/openapi/tok.2f0c", "https://user@helper-sa.ru/shared/bpmn/t",
                "https://helper-sa.ru/shared/bpmn/t/../../x"}) {
            assertTrue(ShareLink.parse(value).isEmpty(), String.valueOf(value));
        }
    }
}
