package group.aelysium.rustyconnector.proxy.magic_link;

import group.aelysium.rustyconnector.toolkit.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.messenger.MessengerConnection;
import group.aelysium.rustyconnector.common.messenger.MessengerConnector;
import group.aelysium.rustyconnector.toolkit.RustyConnector;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.magic_link.IMagicLink;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MagicLink extends MagicLinkCore implements IMagicLink.Proxy {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected IMessengerConnector connector;
    protected Map<String, MagicLinkMCLoaderSettings> settingsMap;

    protected MagicLink(IMessengerConnector connector, Map<String, MagicLinkMCLoaderSettings> magicLinkMCLoaderSettingsMap) {
        this.connector = connector;
        this.settingsMap = magicLinkMCLoaderSettingsMap;
    }

    private void heartbeat() {
        this.executor.schedule(() -> {
            try {
                // Unregister any stale servers
                // The removing feature of server#unregister is valid because serverService.servers() creates a new list which isn't bound to the underlying list.
                RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().dump().forEach(ff -> {
                    try {
                        ff.orElseThrow().connector().mcloaders().forEach(s -> {
                            s.decreaseTimeout(3);

                            try {
                                if (s.stale()) s.unregister(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception ignore) {}

            this.heartbeat();
        }, 3, TimeUnit.SECONDS);
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
        this.connector.connection().orElseThrow().close();
        this.executor.shutdownNow();
    }

    public static class Tinder extends Particle.Tinder<IMagicLink.Proxy> {
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
        public @NotNull IMagicLink.Proxy ignite() throws Exception {
            IMessengerConnector connector = this.connector();

            MagicLink service = new MagicLink(connector, this.magicConfigs);
            service.startHeartbeat();
            return service;
        }

    }
}
