package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.CacheableMessage;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.util.ColorMapper;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketStatus;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.AuthenticationException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MagicLinkCore implements Particle {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final TimeoutCache<UUID, Packet> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(10, TimeUnit.SECONDS));
    private final Map<PacketIdentification, List<PacketListener<? extends Packet>>> listeners = new ConcurrentHashMap<>();
    protected final AESCryptor cryptor;
    protected final MessageCache cache;
    protected final Packet.Target self;

    protected MagicLinkCore(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self
    ) {
        this.cryptor = cryptor;
        this.cache = cache;
        this.self = self;
    }

    public void publish(Packet packet) {
        packetsAwaitingReply.put(packet.responseTarget().ownTarget(), packet);
    }

    public void on(PacketListener<? extends Packet> listener) {
        this.listeners.putIfAbsent(listener.target(), new ArrayList<>());
        this.listeners.get(listener.target()).add(listener);
    }

    public void close() throws Exception {
        this.listeners.clear();
        this.cache.close();
        this.packetsAwaitingReply.close();
    }

    /**
     * Handles all the MagicLink/RustyConnector internals of handling MagicLink packets.
     * @param rawMessage A AES-256 encrypted MagicLink packet.
     */
    public Packet handleMessage(String rawMessage) {
        CacheableMessage cachedMessage = null;
        String decryptedMessage;
        try {
            decryptedMessage = this.cryptor.decrypt(rawMessage);
            cachedMessage = this.cache.cacheMessage(decryptedMessage, PacketStatus.UNDEFINED);
        } catch (Exception e) {
            cachedMessage = this.cache.cacheMessage(rawMessage, PacketStatus.UNDEFINED);
            cachedMessage.sentenceMessage(PacketStatus.AUTH_DENIAL, "This message was encrypted using a different private key from what I have!");
            throw new RuntimeException();
        }

        Packet packet = Packet.parseReceived(decryptedMessage);

        if(this.cache.ignoredType(packet)) this.cache.removeMessage(cachedMessage.getSnowflake());

        if(!this.self.isNodeEquivalentToMe(packet.target())) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "This packet wasn't addressed to me.");
            return packet;
        }

        if(packet.replying()) {
            Packet reply = this.packetsAwaitingReply.get(packet.responseTarget().remoteTarget().orElse(null));

            if(reply == null) {
                cachedMessage.sentenceMessage(PacketStatus.TRASHED, "The packet that this is replying to doesn't exist.");
                return packet;
            }

            reply.replyListeners().forEach(l -> l.accept(packet));
            return packet;
        }

        List<PacketListener<? extends Packet>> listeners = this.listeners.get(packet.identification());
        if(listeners == null) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "No listeners exist to handle this packet.");
            return packet;
        }
        if(listeners.isEmpty()) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "No listeners exist to handle this packet.");
            return packet;
        }

        listeners.forEach(listener -> {
            try {
                listener.wrapAndExecute(packet);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return packet;
    }

    public interface Packets {
        interface Handshake {
            class Ping extends Packet.Wrapper {
                public String address() {
                    return this.parameters().get(Parameters.ADDRESS).getAsString();
                }
                public Optional<String> displayName() {
                    String displayName = this.parameters().get(Parameters.DISPLAY_NAME).getAsString();
                    if(displayName.isEmpty()) return Optional.empty();
                    return Optional.of(displayName);
                }
                public String magicConfigName() {
                    return this.parameters().get(Parameters.MAGIC_CONFIG_NAME).getAsString();
                }
                public Integer playerCount() {
                    return this.parameters().get(Parameters.PLAYER_COUNT).getAsInt();
                }
                public Optional<String> podName() {
                    String podName = this.parameters().get(Parameters.POD_NAME).getAsString();
                    if(podName.isEmpty()) return Optional.empty();
                    return Optional.of(podName);
                }

                public Ping(Packet packet) {
                    super(packet);
                }

                public interface Parameters {
                    String ADDRESS = "a";
                    String DISPLAY_NAME = "n";
                    String MAGIC_CONFIG_NAME = "c";
                    String PLAYER_COUNT = "pc";
                    String POD_NAME = "pn";
                }
            }

            class Failure extends Packet.Wrapper {
                public String reason() {
                    return this.parameters().get(Parameters.REASON).getAsString();
                }

                public Failure(Packet packet) {
                    super(packet);
                }

                public interface Parameters {
                    String REASON = "r";
                }
            }

            class Success extends Packet.Wrapper {
                public String message() {
                    return this.parameters().get(Parameters.MESSAGE).getAsString();
                }
                public NamedTextColor color() {
                    return ColorMapper.map(this.parameters().get(Parameters.COLOR).getAsString());
                }
                public Integer pingInterval() {
                    return this.parameters().get(Parameters.INTERVAL).getAsInt();
                }

                public Success(Packet packet) {
                    super(packet);
                }

                public interface Parameters {
                    String MESSAGE = "m";
                    String COLOR = "c";
                    String INTERVAL = "i";
                }
            }
        }

        class Disconnect extends Packet.Wrapper {
            public Disconnect(Packet packet) {
                super(packet);
            }
        }

        class StalePing extends Packet.Wrapper {
            public StalePing(Packet packet) {
                super(packet);
            }
        }

        class SendPlayer extends Packet.Wrapper {
            public String targetFamilyName() {
                return this.parameters().get(Parameters.TARGET_FAMILY_NAME).getAsString();
            }

            public UUID uuid() {
                return UUID.fromString(this.parameters().get(Parameters.PLAYER_UUID).getAsString());
            }

            public SendPlayer(Packet packet) {
                super(packet);
            }

            public interface Parameters {
                String TARGET_FAMILY_NAME = "f";
                String PLAYER_UUID = "p";
            }
        }
    }

    public static abstract class Server extends MagicLinkCore {
        protected String registrationConfiguration;
        protected final AtomicInteger delay = new AtomicInteger(5);
        protected final AtomicBoolean stopPinging = new AtomicBoolean(false);
        protected final String podName = System.getenv("POD_NAME");

        protected Server(
                @NotNull AESCryptor cryptor,
                @NotNull MessageCache cache,
                @NotNull Packet.@NotNull Target self,
                @NotNull String registrationConfiguration
        ) {
            super(cryptor, cache, self);
            this.registrationConfiguration = registrationConfiguration;
        }
    }

    public static abstract class Proxy extends MagicLinkCore {
        protected Map<String, ServerRegistrationConfiguration> registrationConfigurations;

        protected Proxy(
                @NotNull AESCryptor cryptor,
                @NotNull MessageCache cache,
                @NotNull Packet.@NotNull Target self,
                @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations
        ) {
            super(cryptor, cache, self);
            this.registrationConfigurations = registrationConfigurations;
        }

        /**
         * Fetches a Magic Link Server Config based on a name.
         * `name` is considered to be the name of the file found in `magic_configs` on the Proxy, minus the file extension.
         * @param name The name to look for.
         */
        public Optional<ServerRegistrationConfiguration> registrationConfig(String name) {
            ServerRegistrationConfiguration settings = this.registrationConfigurations.get(name);
            if(settings == null) return Optional.empty();
            return Optional.of(settings);
        }

        public record ServerRegistrationConfiguration(
                String family,
                int weight,
                int soft_cap,
                int hard_cap
        ) {
            public static ServerRegistrationConfiguration DEFAULT_CONFIGURATION = new ServerRegistrationConfiguration("lobby", 0, 20, 30);
        };
    }
}
