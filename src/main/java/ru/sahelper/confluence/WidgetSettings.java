package ru.sahelper.confluence;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Сайты helper-sa, к которым макрос может обращаться (защита от запросов сервера Confluence на произвольные адреса
 * внутренней сети). Задаёт администратор Confluence; по умолчанию — https://helper-sa.ru.
 */
@Named
public class WidgetSettings {

    static final String KEY = "ru.sahelper.confluence.allowedOrigins";
    static final String DEFAULT = "https://helper-sa.ru";

    private final PluginSettingsFactory settings;

    @Inject
    public WidgetSettings(@ComponentImport PluginSettingsFactory settings) {
        this.settings = settings;
    }

    public List<String> allowedOrigins() {
        Object value = settings.createGlobalSettings().get(KEY);
        return parse(value instanceof String ? (String) value : DEFAULT).origins;
    }

    public boolean allowed(ShareLink link) {
        return allowedOrigins().contains(link.origin());
    }

    /** Сохранить список (по адресу в строке); неверные строки — в ответе, список тогда не меняется. */
    public List<String> save(String text) {
        Parsed parsed = parse(text);
        if (parsed.invalid.isEmpty()) {
            settings.createGlobalSettings().put(KEY, String.join("\n", parsed.origins));
        }
        return parsed.invalid;
    }

    static final class Parsed {
        final List<String> origins;
        final List<String> invalid;

        Parsed(List<String> origins, List<String> invalid) {
            this.origins = origins;
            this.invalid = invalid;
        }
    }

    /** Строки — адреса сайтов (https://host[:port], путь не нужен); пустые строки пропускаются. */
    static Parsed parse(String text) {
        Set<String> origins = new LinkedHashSet<>();
        List<String> invalid = new ArrayList<>();
        for (String line : (text == null ? "" : text).split("[\\r\\n,]+")) {
            String value = line.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                URI uri = new URI(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
                String origin = ShareLink.origin(uri);
                boolean bare = (uri.getRawPath() == null || uri.getRawPath().isEmpty()) && uri.getRawQuery() == null && uri.getRawFragment() == null;
                if (origin == null || !bare) {
                    invalid.add(value);
                } else {
                    origins.add(origin);
                }
            } catch (URISyntaxException e) {
                invalid.add(value);
            }
        }
        return new Parsed(new ArrayList<>(origins), invalid);
    }
}
