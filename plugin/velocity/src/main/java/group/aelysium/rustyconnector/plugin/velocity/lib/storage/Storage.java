package group.aelysium.rustyconnector.plugin.velocity.lib.storage;

import group.aelysium.rustyconnector.plugin.velocity.lib.storage.reactors.MySQLReactor;
import group.aelysium.rustyconnector.plugin.velocity.lib.storage.reactors.StorageReactor;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IStorage;
import group.aelysium.rustyconnector.toolkit.core.UserPass;

import java.net.InetSocketAddress;
import java.sql.SQLException;

public class Storage implements IStorage {
    protected final RemoteStorage remoteStorage;
    protected final Configuration config;
    protected Storage(Configuration config) throws SQLException {
        this.config = config;
        this.remoteStorage = new RemoteStorage(this.config.reactor());
    }

    public RemoteStorage database() {
        return this.remoteStorage;
    }

    @Override
    public void kill() {
        this.remoteStorage.kill();
    }

    public static Storage create(Configuration configuration) throws SQLException {
        return new Storage(configuration);
    }

    public enum StorageType {
        SQLITE,
        MYSQL
    }

    public static abstract class Configuration {
        protected final StorageType type;

        protected Configuration(StorageType type) {
            this.type = type;
        }

        public StorageType type() {
            return this.type;
        }
        public abstract StorageReactor reactor();

        public static class MySQL extends Configuration {
            private final MySQLReactor.Core.Settings settings;

            public MySQL(InetSocketAddress address, UserPass userPass, String database) {
                super(StorageType.MYSQL);
                this.settings = new MySQLReactor.Core.Settings(address, userPass, database);
            }

            public StorageReactor reactor() {
                return new MySQLReactor(settings);
            }

            public InetSocketAddress address() {
                return this.settings.address();
            }
            public UserPass userPass() {
                return this.settings.userPass();
            }
            public String database() {
                return this.settings.database();
            }
        }
    }
}
