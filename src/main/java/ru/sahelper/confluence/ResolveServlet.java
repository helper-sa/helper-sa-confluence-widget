package ru.sahelper.confluence;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Текущая версия документа по ссылке — для окна параметров макроса ({@code macro-browser.js}): вставка ссылки
 * на документ закрепляет эту версию. Ответ — {@code {"link":"document|version","versionNumber":N}} или
 * {@code {"error":"..."}}. Только для вошедших пользователей и разрешённых сайтов.
 */
public class ResolveServlet extends HttpServlet {

    private final UserManager users;
    private final WidgetSettings settings;
    private final EmbedClient client;

    @Inject
    public ResolveServlet(@ComponentImport UserManager users, WidgetSettings settings, EmbedClient client) {
        this.users = users;
        this.settings = settings;
        this.client = client;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (users.getRemoteUser(request) == null) {
            write(response, HttpServletResponse.SC_UNAUTHORIZED, "{\"error\":\"unauthorized\"}");
            return;
        }
        Optional<ShareLink> link = ShareLink.parse(request.getParameter("url"));
        if (link.isEmpty()) {
            write(response, HttpServletResponse.SC_BAD_REQUEST, "{\"error\":\"invalid-link\"}");
            return;
        }
        if (!settings.allowed(link.get())) {
            write(response, HttpServletResponse.SC_FORBIDDEN, "{\"error\":\"origin-not-allowed\"}");
            return;
        }
        try {
            EmbedInfo info = client.fetchCurrent(link.get());
            write(response, HttpServletResponse.SC_OK, "{\"link\":\"" + (info.versionLink() ? "version" : "document")
                    + "\",\"versionNumber\":" + info.versionNumber() + "}");
        } catch (EmbedClient.EmbedException e) {
            boolean notFound = e.failure() == EmbedClient.Failure.NOT_FOUND;
            write(response, notFound ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_GATEWAY,
                    notFound ? "{\"error\":\"not-found\"}" : "{\"error\":\"unavailable\"}");
        }
    }

    private static void write(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.getWriter().write(json);
    }
}
