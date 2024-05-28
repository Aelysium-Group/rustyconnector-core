package group.aelysium.rustyconnector.toolkit.common.lang;

import group.aelysium.rustyconnector.toolkit.common.serviceable.interfaces.Service;

import java.io.File;

public interface ILangService<TLanguageResolver extends ILanguageResolver> extends Service {
    String code();

    boolean isInline();

    TLanguageResolver resolver();

    File get(LangFileMappings.Mapping fileName);
}
