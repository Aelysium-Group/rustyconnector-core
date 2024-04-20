package group.aelysium.rustyconnector.plugin.velocity.lib.magic_link;

import group.aelysium.rustyconnector.core.lib.crypt.AESCryptor;
import group.aelysium.rustyconnector.core.lib.messenger.MessengerConnection;
import group.aelysium.rustyconnector.core.lib.messenger.MessengerConnector;
import group.aelysium.rustyconnector.core.lib.messenger.implementors.redis.RedisConnector;
import group.aelysium.rustyconnector.toolkit.core.UserPass;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnection;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnector;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ClockService;
import group.aelysium.rustyconnector.toolkit.velocity.magic_link.IMagicLink;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MagicLinkService extends IMagicLink {
    protected final ClockService clock;
    protected IMessengerConnector connector;
    protected Map<String, MagicLinkMCLoaderSettings> settingsMap;

    protected MagicLinkService(IMessengerConnector connector, Map<String, MagicLinkMCLoaderSettings> magicLinkMCLoaderSettingsMap) {
        this.clock = new ClockService(2);
        this.connector = connector;
        this.settingsMap = magicLinkMCLoaderSettingsMap;
    }

    protected void startHeartbeat() {
        this.clock.scheduleRecurring(() -> {
            try {
                // Unregister any stale servers
                // The removing feature of server#unregister is valid because serverService.servers() creates a new list which isn't bound to the underlying list.
                serverService.servers().forEach(server -> {
                    server.decreaseTimeout(3);

                    try {
                        if (server.stale()) server.unregister(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception ignore) {}
        }, 3, 5); // Period of `3` lets us not loop over the servers as many times with a small hit to how quickly stale servers will be unregistered.
    }

    public Optional<MagicLinkMCLoaderSettings> magicConfig(String name) {
        MagicLinkMCLoaderSettings settings = this.settingsMap.get(name);
        if(settings == null) return Optional.empty();
        return Optional.of(settings);
    }

    /**
     * Get the {@link MessengerConnection} created from this {@link MessengerConnector}.
     * @return An {@link Optional} possibly containing a {@link MessengerConnection}.
     */
    public Optional<IMessengerConnection> connection() {
        return this.redisConnector.connection();
    }

    /**
     * Connect to the remote resource.
     * @return A {@link MessengerConnection}.
     * @throws ConnectException If there was an issue connecting to the remote resource.
     */
    public IMessengerConnection connect() throws ConnectException {
        return this.redisConnector.connect();
    }

    @Override
    public void close() throws Exception {
        this.redisConnector.kill();
    }

    public enum MessengerType {
        REDIS
    }

    public static class Tinder extends Particle.Tinder<MagicLinkService> {
        private Map<String, MagicLinkMCLoaderSettings> magicConfigs = new ConcurrentHashMap<>();
        private AESCryptor cryptor;
        private RedisConnector.Settings redis;

        public Tinder() {}

        public void storeConfig(String key, MagicLinkMCLoaderSettings config) {
            this.magicConfigs.put(key, config);
        }

        public void cryptor(AESCryptor cryptor) {
            this.cryptor = cryptor;
        }

        private void validate() throws IllegalArgumentException {
            if(this.cryptor == null) throw new IllegalArgumentException("You must provide an AESCryptor!");
            boolean hasAValidService = false;
            if(this.redis != null) hasAValidService = true;

            if(!hasAValidService) throw new IllegalArgumentException("You must define a valid provider for Magic Link/!");
        }

        @Override
        public @NotNull MagicLinkService ignite() throws Exception {
            this.validate();

            MagicLinkService service = new MagicLinkService();
            service.startHeartbeat();
            return service;
        }

    }

    public static abstract class Configuration {
        protected final AESCryptor cryptor;
        protected final MessengerType type;

        protected Configuration(AESCryptor cryptor, MessengerType type) {
            this.cryptor = cryptor;
            this.type = type;
        }

        public MessengerType type() {
            return this.type;
        }
        public abstract MessengerConnector connector();

        public static class Redis extends Configuration {
            private final InetSocketAddress address;
            private final UserPass userPass;
            private final String channel;
            private final String protocol;

            public Redis(AESCryptor cryptor, InetSocketAddress address, UserPass userPass, String protocol, String channel) {
                super(cryptor, MessengerType.REDIS);
                this.address = address;
                this.userPass = userPass;
                this.channel = channel;
                this.protocol = protocol;
            }

            public RedisConnector connector() {
                return new RedisConnector(cryptor, this.address, this.userPass, protocol, this.channel);
            }
        }
    }
}
