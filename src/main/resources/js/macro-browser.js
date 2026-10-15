/*
 * Окно параметров макроса «Помощник системного аналитика»: при вставке ссылки на документ закрепляет его текущую
 * версию (параметр version) — страница Confluence не меняется вместе с документом. Версию перезакрепляют,
 * очистив поле «Версия документа» или заменив ссылку. Ссылке на версию номер не нужен.
 */
(function ($) {
    var MACRO = 'helper-sa';
    var openedUrl = null;

    // синхронно: параметры забираются из окна сразу после нажатия «Вставить»/«Сохранить»
    function resolve(url) {
        var result = null;
        $.ajax({
            url: AJS.contextPath() + '/plugins/servlet/helper-sa/resolve',
            data: { url: url },
            dataType: 'json',
            async: false,
            timeout: 15000,
            success: function (data) { result = data; }
        });
        return result;
    }

    function install() {
        if (!AJS.MacroBrowser || !AJS.MacroBrowser.setMacroJsOverride) {
            return;
        }
        AJS.MacroBrowser.setMacroJsOverride(MACRO, {
            beforeParamsSet: function (params) {
                openedUrl = params && params.url ? params.url : null;
                return params;
            },
            beforeParamsRetrieved: function (params) {
                var url = params && params.url ? $.trim(params.url) : '';
                if (!url) {
                    return params;
                }
                var urlChanged = url !== $.trim(openedUrl || '');
                if (params.version && !urlChanged) {
                    return params;
                }
                var info = resolve(url);
                if (info && info.link === 'document' && info.versionNumber) {
                    params.version = String(info.versionNumber);
                } else if (info && info.link === 'version') {
                    delete params.version;
                } else if (urlChanged) {
                    // документ не удалось проверить — версия от прежней ссылки к новой не относится
                    delete params.version;
                }
                return params;
            }
        });
    }

    AJS.toInit(install);
})(AJS.$);
