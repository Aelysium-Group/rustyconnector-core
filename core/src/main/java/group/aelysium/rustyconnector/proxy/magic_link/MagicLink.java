package group.aelysium.rustyconnector.proxy.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MagicLink extends MagicLinkCore {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    protected Map<String, MagicLinkServerSettings> settingsMap;

    protected MagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull Map<String, MagicLinkServerSettings> magicLinkServerSettingsMap
    ) {
        super(cryptor, cache, self);
        this.settingsMap = magicLinkServerSettingsMap;
        this.heartbeat();
    }

    private void heartbeat() {
        this.executor.schedule(() -> {
            try {
                RC.P.Families().dump().forEach(f -> {
                    try {
                        Family family = f.orElseThrow();
                        family.servers().forEach(server -> {
                            server.decreaseTimeout(3);

                            try {
                                if (server.stale()) family.deleteServer(server);
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

    /**
     * Fetches a Magic Link Server Config based on a name.
     * `name` is considered to be the name of the file found in `magic_configs` on the Proxy, minus the file extension.
     * @param name The name to look for.
     */
    public Optional<MagicLinkServerSettings> magicConfig(String name) {
        MagicLinkServerSettings settings = this.settingsMap.get(name);
        if(settings == null) return Optional.empty();
        return Optional.of(settings);
    }

    @Override
    public void close() throws Exception {
        super.close();
        this.executor.shutdownNow();
    }

    public record MagicLinkServerSettings(
            String family,
            int weight,
            int soft_cap,
            int hard_cap
    ) {
        public static MagicLinkServerSettings DEFAULT_CONFIGURATION = new MagicLinkServerSettings("lobby", 0, 20, 30);
    };

    public static class Tinder extends Particle.Tinder<MagicLink> {
        private final AESCryptor cryptor;
        private final Packet.Target self;
        private final MessageCache cache;
        private final Map<String, MagicLinkServerSettings> magicConfigs;
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull Packet.Target self,
                @NotNull MessageCache cache,
                @NotNull Map<String, MagicLinkServerSettings> magicConfigs
                ) {
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.magicConfigs = magicConfigs;
        }

        @Override
        public @NotNull MagicLink ignite() throws Exception {
            return new MagicLink(
                    this.cryptor,
                    this.cache,
                    this.self,
                    this.magicConfigs
            );
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID proxyUUID) {
            return new Tinder(
                    AESCryptor.DEFAULT_CRYPTOR,
                    Packet.Target.proxy(proxyUUID),
                    new MessageCache(50),
                    new HashMap<>()
            );
        }
    }
}
