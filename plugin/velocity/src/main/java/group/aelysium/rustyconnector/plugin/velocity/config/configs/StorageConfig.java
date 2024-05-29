package group.aelysium.rustyconnector.plugin.velocity.config.configs;

import group.aelysium.rustyconnector.common.config.YAML;
import group.aelysium.rustyconnector.common.lang.LangService;
import group.aelysium.rustyconnector.plugin.velocity.lib.remote_storage.Storage;
import group.aelysium.rustyconnector.toolkit.common.UserPass;
import group.aelysium.rustyconnector.toolkit.common.config.IConfigService;
import group.aelysium.rustyconnector.toolkit.common.config.IYAML;
import group.aelysium.rustyconnector.toolkit.common.lang.LangFileMappings;

import java.net.InetSocketAddress;
import java.nio.file.Path;

public class StorageConfig extends YAML {
    private Storage.Configuration configuration;
    public Storage.Configuration storageConfiguration() {
        return this.configuration;
    }



    protected StorageConfig(Path dataFolder, String target, String name, LangService lang) {
        super(dataFolder, target, name, lang, LangFileMappings.PROXY_CONNECTORS_TEMPLATE);
    }

    @SuppressWarnings("unchecked")
    protected void register() throws IllegalStateException {
        Storage.StorageType storageType = Storage.StorageType.valueOf(IYAML.getValue(this.data, "provider", String.class));
        switch (storageType) {
            case MYSQL -> {
                String host = IYAML.getValue(this.data, "available-providers.MYSQL.host", String.class);
                if (host.equals(""))
                    throw new IllegalStateException("Please configure your connector settings. `host` cannot be empty.");
                int port = IYAML.getValue(this.data, "available-providers.MYSQL.port", Integer.class);
                InetSocketAddress address = new InetSocketAddress(host, port);

                String user = IYAML.getValue(this.data, "available-providers.MYSQL.user", String.class);
                if (user.equals(""))
                    throw new IllegalStateException("Please configure your connector settings. `user` cannot be empty.");
                char[] password = IYAML.getValue(this.data, "available-providers.MYSQL.password", String.class).toCharArray();
                UserPass userPass = new UserPass(user, password);
                String database = IYAML.getValue(this.data, "available-providers.MYSQL.database", String.class);

                this.configuration = new Storage.Configuration.MySQL(address, userPass, database);
            }
            default -> throw new NullPointerException("No proper Storage System was defined!");
        }
    }

    public static StorageConfig construct(Path dataFolder, LangService lang) {
        StorageConfig config = new StorageConfig(dataFolder, "storage.yml", "storage", lang);
        config.register();
        return config;
    }

    @Override
    public IConfigService.ConfigKey key() {
        return IConfigService.ConfigKey.singleton(StorageConfig.class);
    }
}
