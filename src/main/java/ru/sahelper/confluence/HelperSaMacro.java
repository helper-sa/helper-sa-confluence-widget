package ru.sahelper.confluence;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Макрос «Помощник системного аналитика»: по ссылке «Поделиться» (документ или версия) показывает картинку
 * (миниатюра заданной ширины, щелчок — полный размер), ссылку на источник и аккордеон с исходным кодом.
 * Ссылка на документ показывает версию, закреплённую при вставке (параметр {@code version}, его заполняет
 * {@code macro-browser.js}): документ меняется — страница Confluence нет.
 * Сведения запрашиваются сервером Confluence ({@link EmbedClient}), картинку браузер загружает из helper-sa.
 */
public class HelperSaMacro implements Macro {

    private static final Logger log = LoggerFactory.getLogger(HelperSaMacro.class);
    private static final String RESOURCES = "ru.sahelper.helper-sa-confluence-widget:widget-resources";

    private final XhtmlContent xhtml;
    private final PageBuilderService pageBuilder;
    private final WidgetSettings settings;
    private final EmbedClient client;

    @Inject
    public HelperSaMacro(@ComponentImport XhtmlContent xhtml, @ComponentImport PageBuilderService pageBuilder,
                         WidgetSettings settings, EmbedClient client) {
        this.xhtml = xhtml;
        this.pageBuilder = pageBuilder;
        this.settings = settings;
        this.client = client;
    }

    @Override
    public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException {
        pageBuilder.assembler().resources().requireWebResource(RESOURCES);
        String url = parameters.get("url");
        Optional<ShareLink> parsed = ShareLink.parse(url);
        if (parsed.isEmpty()) {
            return WidgetHtml.message("warning", "Неверная ссылка",
                    "Укажите ссылку «Поделиться» на документ или версию вида https://helper-sa.ru/shared/<инструмент>/<токен>.");
        }
        ShareLink link = parsed.get();
        if (!settings.allowed(link)) {
            return WidgetHtml.message("warning", "Сайт не разрешён",
                    "Адрес " + link.origin() + " не входит в список разрешённых. Администратор Confluence может добавить его "
                            + "в настройках «Помощник системного аналитика».");
        }
        // версия, закреплённая при вставке ссылки на документ (заполняет окно параметров макроса)
        Integer pinned = WidgetHtml.version(parameters.get("version"));
        EmbedInfo info;
        try {
            info = client.fetch(link, pinned);
        } catch (EmbedClient.EmbedException e) {
            if (e.failure() == EmbedClient.Failure.NOT_FOUND) {
                return WidgetHtml.message("error", "Документ недоступен", pinned == null
                        ? "Ссылка отозвана или не существует: " + link.url()
                        : "Ссылка отозвана или версия " + pinned + " удалена из истории документа: " + link.url());
            }
            log.warn("helper-sa request failed: {}", e.getMessage());
            return WidgetHtml.message("error", "helper-sa недоступен", "Не удалось получить документ, попробуйте обновить страницу позже.");
        }
        return WidgetHtml.widget(link, info, info.versionLink() ? null : pinned, WidgetHtml.width(parameters.get("width")),
                code(info, link, context), ZoneId.systemDefault());
    }

    /** Исходный код — макросом «Код» Confluence (подсветка, номера строк); не вышло — простым блоком. */
    private String code(EmbedInfo info, ShareLink link, ConversionContext context) {
        try {
            return xhtml.convertStorageToView(WidgetHtml.codeStorage(link.tool(), info.source()), context);
        } catch (Exception e) {
            log.debug("Code macro rendering failed, falling back to <pre>", e);
            return WidgetHtml.plainCode(info.source());
        }
    }

    @Override
    public BodyType getBodyType() {
        return BodyType.NONE;
    }

    @Override
    public OutputType getOutputType() {
        return OutputType.BLOCK;
    }
}
