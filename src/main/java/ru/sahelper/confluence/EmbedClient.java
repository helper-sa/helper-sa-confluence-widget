package ru.sahelper.confluence;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Named;

/**
 * Запрос сведений о документе в helper-sa с сервера Confluence. Ответы кешируются на минуту: страница с виджетом
 * не обращается к helper-sa при каждом просмотре. Перенаправления не выполняются (адрес проверен по списку
 * разрешённых сайтов, перенаправление увело бы запрос на другой адрес).
 */
@Named
public class EmbedClient {

    static final Duration TTL = Duration.ofMinutes(1);
    private static final int MAX_CACHED = 500;
    private static final int MAX_BODY = 8 * 1024 * 1024;

    /** Почему документ не получен. */
    public enum Failure { NOT_FOUND, UNAVAILABLE }

    public static final class EmbedException extends Exception {
        private final Failure failure;

        EmbedException(Failure failure, String message, Throwable cause) {
            super(message, cause);
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }

    private static final class Cached {
        final long expiresAt;
        final EmbedInfo info;

        Cached(long expiresAt, EmbedInfo info) {
            this.expiresAt = expiresAt;
            this.info = info;
        }
    }

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final HttpClient http;

    public EmbedClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER);
        // прокси JVM Confluence (-Dhttps.proxyHost …), если задан
        ProxySelector proxy = ProxySelector.getDefault();
        if (proxy != null) {
            builder.proxy(proxy);
        }
        this.http = builder.build();
    }

    /** Документ (version — номер закреплённой версии, null — текущая) с кешем на минуту. */
    public EmbedInfo fetch(ShareLink link, Integer version) throws EmbedException {
        String url = link.apiUrl(version);
        long now = System.currentTimeMillis();
        Cached cached = cache.get(url);
        if (cached != null && cached.expiresAt > now) {
            return cached.info;
        }
        EmbedInfo info = load(url);
        if (cache.size() >= MAX_CACHED) {
            cache.clear();
        }
        cache.put(url, new Cached(now + TTL.toMillis(), info));
        return info;
    }

    /** Текущее состояние документа без кеша — номер версии, которую закрепит вставка макроса. */
    public EmbedInfo fetchCurrent(ShareLink link) throws EmbedException {
        return load(link.apiUrl(null));
    }

    private EmbedInfo load(String url) throws EmbedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "helper-sa-confluence-widget")
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new EmbedException(Failure.UNAVAILABLE, "helper-sa unavailable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbedException(Failure.UNAVAILABLE, "Interrupted", e);
        }
        if (response.statusCode() == 404) {
            throw new EmbedException(Failure.NOT_FOUND, "Shared document not found", null);
        }
        if (response.statusCode() != 200 || response.body().length > MAX_BODY) {
            throw new EmbedException(Failure.UNAVAILABLE, "helper-sa answered " + response.statusCode(), null);
        }
        try {
            return EmbedInfo.fromJson(new String(response.body(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new EmbedException(Failure.UNAVAILABLE, "Unexpected helper-sa answer", e);
        }
    }
}
