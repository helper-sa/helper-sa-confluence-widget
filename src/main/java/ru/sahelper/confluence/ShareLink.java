package ru.sahelper.confluence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ссылка «Поделиться» helper-sa: {@code https://<сайт>/shared/<инструмент>/<токен>} — на документ (всегда текущее
 * содержимое) или на версию (именно она). Ссылки на проект ({@code /shared/project/...}) не поддерживаются.
 */
public final class ShareLink {

    private static final Pattern PATH = Pattern.compile("^/shared/([a-z][a-z0-9-]{0,31})/([A-Za-z0-9_-]{1,64})/?$");

    private final String origin;
    private final String tool;
    private final String token;
    private final String url;

    private ShareLink(String origin, String tool, String token) {
        this.origin = origin;
        this.tool = tool;
        this.token = token;
        this.url = origin + "/shared/" + tool + "/" + token;
    }

    /** Разбор ссылки; не ссылка helper-sa на документ или версию — пусто. */
    public static Optional<ShareLink> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(value.trim());
            String origin = origin(uri);
            Matcher m = PATH.matcher(uri.getRawPath() == null ? "" : uri.getRawPath());
            if (origin == null || !m.matches() || "project".equals(m.group(1)) || "projects".equals(m.group(1))) {
                return Optional.empty();
            }
            return Optional.of(new ShareLink(origin, m.group(1), m.group(2)));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    /** {@code https://host[:port]} в нижнем регистре; не http(s) или без хоста — null. */
    public static String origin(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http") || uri.getHost() == null || uri.getRawUserInfo() != null) {
            return null;
        }
        boolean defaultPort = uri.getPort() == -1 || scheme.equals("https") && uri.getPort() == 443 || scheme.equals("http") && uri.getPort() == 80;
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + uri.getPort());
    }

    public String origin() {
        return origin;
    }

    public String tool() {
        return tool;
    }

    public String token() {
        return token;
    }

    /** Нормализованная ссылка на просмотр в helper-sa. */
    public String url() {
        return url;
    }

    /** Сведения о документе; version — номер версии (для ссылки на документ), null — текущая. */
    public String apiUrl(Integer version) {
        return base() + query(version);
    }

    public String imageUrl(Integer version) {
        return base() + "/image" + query(version);
    }

    private String base() {
        return origin + "/api/v1/embed/" + tool + "/" + token;
    }

    private static String query(Integer version) {
        return version == null ? "" : "?version=" + version;
    }
}
