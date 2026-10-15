package ru.sahelper.confluence;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.xsrf.XsrfTokenAccessor;
import com.atlassian.sal.api.xsrf.XsrfTokenValidator;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Настройки плагина для системного администратора Confluence: адреса сайтов helper-sa, к которым можно обращаться. */
public class AdminServlet extends HttpServlet {

    private final UserManager users;
    private final LoginUriProvider login;
    private final XsrfTokenAccessor xsrfTokens;
    private final XsrfTokenValidator xsrfValidator;
    private final WidgetSettings settings;

    @Inject
    public AdminServlet(@ComponentImport UserManager users, @ComponentImport LoginUriProvider login,
                        @ComponentImport XsrfTokenAccessor xsrfTokens, @ComponentImport XsrfTokenValidator xsrfValidator,
                        WidgetSettings settings) {
        this.users = users;
        this.login = login;
        this.xsrfTokens = xsrfTokens;
        this.xsrfValidator = xsrfValidator;
        this.settings = settings;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (requireAdmin(request, response)) {
            render(request, response, String.join("\n", settings.allowedOrigins()), null, false);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        if (!xsrfValidator.validateFormEncodedToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "XSRF check failed");
            return;
        }
        String text = request.getParameter("origins");
        List<String> invalid = settings.save(text);
        render(request, response, text == null ? "" : text, invalid.isEmpty() ? null : invalid, invalid.isEmpty());
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserProfile user = users.getRemoteUser(request);
        if (user == null) {
            StringBuffer url = request.getRequestURL();
            if (request.getQueryString() != null) {
                url.append('?').append(request.getQueryString());
            }
            response.sendRedirect(login.getLoginUri(URI.create(url.toString())).toASCIIString());
            return false;
        }
        if (!users.isSystemAdmin(user.getUserKey())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String origins, List<String> invalid,
                        boolean saved) throws IOException {
        String token = xsrfTokens.getXsrfToken(request, response, true);
        response.setContentType("text/html;charset=UTF-8");
        StringBuilder html = new StringBuilder()
                .append("<html><head><title>Помощник системного аналитика</title>")
                .append("<meta name=\"decorator\" content=\"atl.admin\"/>")
                .append("<meta name=\"admin.active.section\" content=\"system.admin/configuration\"/>")
                .append("</head><body>")
                .append("<h2>Помощник системного аналитика</h2>")
                .append("<p>Макрос «Помощник системного аналитика» показывает документы только с этих сайтов helper-sa: ")
                .append("сервер Confluence обращается к ним за содержимым документа. По одному адресу в строке, ")
                .append("например <code>https://helper-sa.ru</code> (без пути).</p>");
        if (saved) {
            html.append(WidgetHtml.message("success", "Сохранено", "Список сайтов обновлён."));
        }
        if (invalid != null) {
            html.append(WidgetHtml.message("error", "Неверные адреса", String.join(", ", invalid)));
        }
        html.append("<form class=\"aui\" method=\"post\" action=\"").append(WidgetHtml.escape(request.getRequestURI())).append("\">")
                .append("<input type=\"hidden\" name=\"").append(WidgetHtml.escape(xsrfValidator.getXsrfParameterName()))
                .append("\" value=\"").append(WidgetHtml.escape(token)).append("\"/>")
                .append("<div class=\"field-group\"><label for=\"helper-sa-origins\">Разрешённые сайты</label>")
                .append("<textarea class=\"textarea long-field\" rows=\"5\" id=\"helper-sa-origins\" name=\"origins\">")
                .append(WidgetHtml.escape(origins)).append("</textarea></div>")
                .append("<div class=\"buttons-container\"><div class=\"buttons\">")
                .append("<input class=\"aui-button aui-button-primary\" type=\"submit\" value=\"Сохранить\"/>")
                .append("</div></div></form></body></html>");
        response.getWriter().write(html.toString());
    }
}
