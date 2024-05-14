package group.aelysium.rustyconnector.plugin.velocity.lib.magic_link;

import group.aelysium.rustyconnector.core.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.core.common.messenger.MessengerConnection;
import group.aelysium.rustyconnector.core.common.messenger.MessengerConnector;
import group.aelysium.rustyconnector.core.common.messenger.implementors.redis.RedisConnector;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnection;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnector;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ClockService;
import group.aelysium.rustyconnector.toolkit.velocity.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MagicLinkService extends IMagicLink {
    protected final ClockService clock;
    protected IMessengerConnector connector;
    protected Map<String, MagicLinkMCLoaderSettings> settingsMap;

    protected MagicLinkService(IMessengerConnector connector, Map<String, MagicLinkMCLoaderSettings> magicLinkMCLoaderSettingsMap) {
        this.clock = new ClockService(2);
        this.connector = connector;
        this.settingsMap = magicLinkMCLoaderSettingsMap;
    }

    private void heartbeat() {
        this.clock.scheduleDelayed(() -> {
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

            this.heartbeat();
        }, LiquidTimestamp.from(3, TimeUnit.SECONDS));
    }

    protected void startHeartbeat() {
        this.heartbeat();
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
        return this.connector.connection();
    }

    /**
     * Connect to the remote resource.
     * @return A {@link MessengerConnection}.
     * @throws ConnectException If there was an issue connecting to the remote resource.
     */
    public IMessengerConnection connect() throws ConnectException {
        return this.connector.connect();
    }

    @Override
    public void close() throws Exception {
        this.clock.kill();
        this.connector.kill();
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

        public void redis(RedisConnector.Settings settings) {
            this.redis = settings;
        }

        private IMessengerConnector connector() throws IllegalArgumentException {
            if(this.cryptor == null) throw new IllegalArgumentException("You must provide an AESCryptor!");

            if(this.redis != null) return new RedisConnector(this.cryptor, this.redis);

            throw new IllegalArgumentException("You must define a valid provider for Magic Link!");
        }

        @Override
        public @NotNull MagicLinkService ignite() throws Exception {
            IMessengerConnector connector = this.connector();

            MagicLinkService service = new MagicLinkService(connector, this.magicConfigs);
            service.startHeartbeat();
            return service;
        }

    }
}
