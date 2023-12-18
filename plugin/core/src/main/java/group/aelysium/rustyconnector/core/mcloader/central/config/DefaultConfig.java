package group.aelysium.rustyconnector.core.mcloader.central.config;

import group.aelysium.rustyconnector.core.lib.lang.LangService;
import group.aelysium.rustyconnector.toolkit.core.config.IConfigService;
import group.aelysium.rustyconnector.toolkit.core.config.IYAML;
import group.aelysium.rustyconnector.toolkit.core.lang.LangFileMappings;
import group.aelysium.rustyconnector.core.lib.config.YAML;

import java.nio.file.Path;

public class DefaultConfig extends YAML {
    private String address;
    private String magicConfig;
    private boolean magicInterfaceResolver;

    public String address() {
        return address;
    }

    public String magicConfig() {
        return magicConfig;
    }
    public boolean magicInterfaceResolver() {
        return magicInterfaceResolver;
    }

    protected DefaultConfig(Path dataFolder, String target, String name, LangService lang) {
        super(dataFolder, target, name, lang, LangFileMappings.MCLOADER_CONFIG_TEMPLATE);
    }

    protected void register(int configVersion) throws IllegalStateException {
        try {
            this.processVersion(configVersion);
        } catch (Exception | UnsupportedClassVersionError e) {
            throw new IllegalStateException(e.getMessage());
        }

        this.magicConfig = IYAML.getValue(this.data,"magic-config",String.class);
        if(this.magicConfig.equals("")) throw new IllegalStateException("You must provide a magic config name name in order for RustyConnector to work! The config name must correspond to a config on your proxy.");

        this.address = IYAML.getValue(this.data,"address",String.class);

        this.magicInterfaceResolver = IYAML.getValue(this.data,"magic-interface-resolver",Boolean.class);
        if(this.address.equals("")) this.magicInterfaceResolver = true;
    }

    public static DefaultConfig construct(Path dataFolder, LangService lang, int pluginConfigVersion) {
        DefaultConfig config = new DefaultConfig(dataFolder, "config.yml", "config", lang);
        config.register(pluginConfigVersion);
        return config;
    }

    @Override
    public IConfigService.ConfigKey key() {
        return IConfigService.ConfigKey.singleton(DefaultConfig.class);
    }
}
