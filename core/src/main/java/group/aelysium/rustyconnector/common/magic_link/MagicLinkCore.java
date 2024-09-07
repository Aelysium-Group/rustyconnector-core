package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.common.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.CacheableMessage;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.proxy.util.ColorMapper;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class MagicLinkCore implements Particle {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final TimeoutCache<UUID, Packet.Local> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(10, TimeUnit.SECONDS));
    private final Map<String, List<Consumer<Packet>>> listeners = new ConcurrentHashMap<>();
    protected final AES cryptor;
    protected final MessageCache cache;
    protected final Packet.Target self;

    protected MagicLinkCore(
            @NotNull Packet.Target self,
            @NotNull AES cryptor,
            @NotNull MessageCache cache
    ) {
        this.self = self;
        this.cryptor = cryptor;
        this.cache = cache;
        
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(""))
                        .setScanners(Scanners.MethodsAnnotated)
        );

        Set<Method> endpoints = reflections.getMethodsAnnotatedWith(PacketListener.class);

        endpoints.forEach(method -> {
            PacketListener annotation = method.getAnnotation((PacketListener.class));
            try {
                Class<? extends Packet.Wrapper> packetWrapper = annotation.value();
                Constructor<? extends Packet.Wrapper> constructor = annotation.value().getConstructor(Packet.class);
                PacketIdentification packetIdentification = constructor.getAnnotation(PacketIdentification.class);
                if(packetIdentification == null) throw new NullPointerException("You must provide a @PacketIdentification for Packet Wrappers. Missing on: " + packetWrapper.getSimpleName());
                this.listeners.computeIfAbsent(packetIdentification.value(), k -> new ArrayList<>()).add(packet -> {
                    try {
                        try {
                            method.invoke(null, constructor.newInstance(packet));
                        } catch (IllegalArgumentException ignore) {
                            method.invoke(null);
                        }
                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles the publishing of the provided packet.
     * This method really shouldn't be used directly, instead you should use {@link group.aelysium.rustyconnector.common.magic_link.packet.Packet.Builder} to construct and then publish a packet.
     * @throws IllegalStateException If the provided packet is from somewhere else and not created by the caller.
     */
    public abstract void publish(Packet.Local packet) throws IllegalStateException;

    /**
     * Queues the packet into the reply queue.
     * If a reply is received which contains a Reply Target pointing to this packet,
     * any listeners registered in {@link group.aelysium.rustyconnector.common.magic_link.packet.Packet.Local#onReply(Consumer)} will run.
     * @param packet The packet to queue.
     */
    public void awaitReply(Packet.Local packet) {
        packetsAwaitingReply.put(packet.replyTarget().ownTarget(), packet);
    }

    /**
     * Registers a new packet handler to the magic link provider.
     * @param identification The identification of the specific packet.
     * @param handler The handler to use for handling the packet.
     *                This handler will be handled asynchronously and will not affect other handlers, even if it throws an exception.
     */
    public void on(String identification, Consumer<Packet> handler) {
        this.listeners.computeIfAbsent(identification, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Fetches the message cache for this magic link provider.
     */
    public MessageCache messageCache() {
        return this.cache;
    }

    public void close() {
        this.listeners.clear();
        this.cache.close();
        this.packetsAwaitingReply.close();
    }

    /**
     * Handles all the MagicLink/RustyConnector internals of handling MagicLink packets.
     *
     * @param rawMessage An AES-256 encrypted MagicLink packet.
     */
    protected void handleMessage(String rawMessage) {
        CacheableMessage cachedMessage = null;
        String decryptedMessage;
        try {
            decryptedMessage = this.cryptor.decrypt(rawMessage);
            cachedMessage = this.cache.cacheMessage(decryptedMessage, Packet.Status.UNDEFINED);
        } catch (Exception e) {
            cachedMessage = this.cache.cacheMessage(rawMessage, Packet.Status.UNDEFINED);
            cachedMessage.sentenceMessage(Packet.Status.AUTH_DENIAL, "This message was encrypted using a different private key from what I have!");
            throw new RuntimeException();
        }

        Packet.Remote packet = Packet.parseReceived(decryptedMessage);

        if(this.cache.ignoredType(packet)) this.cache.removeMessage(cachedMessage.getSnowflake());

        if(!this.self.isEquivalent(packet.target())) {
            cachedMessage.sentenceMessage(Packet.Status.TRASHED, "This packet wasn't addressed to me.");
            return;
        }

        if(packet.replying()) {
            Packet.Local replyTarget = this.packetsAwaitingReply.get(packet.replyTarget().remoteTarget().orElse(null));

            if(replyTarget == null) {
                cachedMessage.sentenceMessage(Packet.Status.TRASHED, "The packet that this is replying to doesn't exist.");
                return;
            }

            replyTarget.handleReply(packet);
            return;
        }

        List<Consumer<Packet>> listeners = this.listeners.get(packet.identification().get());
        if(listeners == null) {
            cachedMessage.sentenceMessage(Packet.Status.TRASHED, "No listeners exist to handle this packet.");
            return;
        }
        if(listeners.isEmpty()) {
            cachedMessage.sentenceMessage(Packet.Status.TRASHED, "No listeners exist to handle this packet.");
            return;
        }

        Consumer<Consumer<Packet>> handler = listener -> {
            try {
                listener.accept(packet);
            } catch (Exception ignore) {}
        };
        listeners.forEach(handler);
    }

    public interface Packets {
        interface Handshake {
            @PacketIdentification("RC-MLH")
            class Ping extends Packet.Wrapper {
                public String address() {
                    return this.parameters().get(Parameters.ADDRESS).getAsString();
                }
                public Optional<String> displayName() {
                    String displayName = this.parameters().get(Parameters.DISPLAY_NAME).getAsString();
                    if(displayName.isEmpty()) return Optional.empty();
                    return Optional.of(displayName);
                }
                public String serverRegistration() {
                    return this.parameters().get(Parameters.SERVER_REGISTRATION).getAsString();
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
                    String SERVER_REGISTRATION = "sr";
                    String PLAYER_COUNT = "pc";
                    String POD_NAME = "pn";
                }
            }

            @PacketIdentification("RC-MLHF")
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

            @PacketIdentification("RC-MLHS")
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

        @PacketIdentification("RC-MLHK")
        class Disconnect extends Packet.Wrapper {
            public Disconnect(Packet packet) {
                super(packet);
            }
        }

        @PacketIdentification("RC-MLHSP")
        class StalePing extends Packet.Wrapper {
            public StalePing(Packet packet) {
                super(packet);
            }
        }

        @PacketIdentification("RC-SP")
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
        protected final AtomicInteger delay = new AtomicInteger(5);
        protected final AtomicBoolean stopPinging = new AtomicBoolean(false);
        protected IPV6Broadcaster broadcaster;
        protected String registrationConfiguration;

        protected Server(
                @NotNull Packet.@NotNull Target self,
                @NotNull AES cryptor,
                @NotNull MessageCache cache,
                @NotNull String registrationConfiguration,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, cryptor, cache);
            this.registrationConfiguration = registrationConfiguration;
            this.broadcaster = broadcaster;
        }
    }

    public static abstract class Proxy extends MagicLinkCore {
        protected IPV6Broadcaster broadcaster;
        protected Map<String, ServerRegistrationConfiguration> registrationConfigurations;

        protected Proxy(
                @NotNull Packet.@NotNull Target self,
                @NotNull AES cryptor,
                @NotNull MessageCache cache,
                @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, cryptor, cache);
            this.registrationConfigurations = registrationConfigurations;
            this.broadcaster = broadcaster;
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
