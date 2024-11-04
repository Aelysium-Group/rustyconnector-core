package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.Plugin;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.exceptions.PacketStatusResponse;
import group.aelysium.rustyconnector.common.magic_link.exceptions.SuccessPacket;
import group.aelysium.rustyconnector.common.magic_link.exceptions.TrashedPacket;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.common.util.ThrowableConsumer;
import group.aelysium.rustyconnector.proxy.util.ColorMapper;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class MagicLinkCore implements Plugin {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final TimeoutCache<NanoID, Packet.Local> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(15, TimeUnit.SECONDS));
    private final Map<String, List<Consumer<Packet.Remote>>> listeners = new ConcurrentHashMap<>();
    protected final AES aes;
    protected final MessageCache cache;
    protected final Packet.SourceIdentifier self;

    protected MagicLinkCore(
            @NotNull Packet.SourceIdentifier self,
            @NotNull AES aes,
            @NotNull MessageCache cache
    ) {
        this.self = self;
        this.aes = aes;
        this.cache = cache;
    }

    /**
     * Registers the provided listen.
     * If the listen method is static you can set this to be the class of your listen. (Object.class)
     * Or if you want an instance method, you can pass a listen instance. (new Object())
     * @param listener The listen to use.
     */
    public void listen(Object listener) {
        boolean isInstance = !(listener instanceof Class<?>);
        Class<?> objectClass = listener instanceof Class<?> ? (Class<?>) listener : listener.getClass();
        for (Method method : objectClass.getDeclaredMethods()) {
            if(isInstance && Modifier.isStatic(method.getModifiers())) continue;
            if(!isInstance && !Modifier.isStatic(method.getModifiers())) continue;
            if(!method.isAnnotationPresent(PacketListener.class)) continue;
            PacketListener annotation = method.getAnnotation((PacketListener.class));
            if(annotation == null) continue;
            try {
                Class<? extends Packet.Remote> packetWrapper = annotation.value();
                Constructor<? extends Packet.Remote> constructor = packetWrapper.getConstructor(Packet.class);
                PacketIdentification packetIdentification = packetWrapper.getAnnotation(PacketIdentification.class);
                if(packetIdentification == null) throw new NoSuchMethodException("You must provide a @PacketIdentification for Packet Wrappers. Missing on: " + packetWrapper.getSimpleName());
                this.listeners.computeIfAbsent(packetIdentification.value(), k -> new Vector<>()).add(packet -> {
                    try {
                        try {
                            method.invoke(isInstance ? listener : null, constructor.newInstance(packet));
                        } catch (IllegalArgumentException e) {
                            RC.Error(Error.from(e));
                            method.invoke(isInstance ? listener : null);
                        }
                    } catch (InvocationTargetException e) {
                        if(e.getCause() == null) return;

                        packet.statusMessage(e.getMessage());
                        packet.status(Packet.Status.ERROR);
                        if(e.getCause() instanceof PacketStatusResponse statusResponse) packet.status(statusResponse.status());
                    } catch (Throwable e) {
                        packet.status(Packet.Status.ERROR);
                        packet.statusMessage(e.getMessage());
                        RC.Error(Error.from(e));
                    }
                });
            } catch (Exception e) {
                RC.Error(Error.from(e));
            }
        }
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
     * any listeners registered in {@link group.aelysium.rustyconnector.common.magic_link.packet.Packet.Local#onReply(ThrowableConsumer)} will run.
     * @param packet The packet to queue.
     */
    public void awaitReply(Packet.Local packet) {
        packetsAwaitingReply.putIfAbsent(packet.local().replyEndpoint().orElseThrow(), packet);
    }

    /**
     * Registers a new packet handler to the magic link provider.
     * @param identification The identification of the specific packet.
     * @param handler The handler to use for handling the packet.
     *                This handler will be handled asynchronously and will not affect other handlers, even if it throws an exception.
     */
    public void on(String identification, Consumer<Packet.Remote> handler) {
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
     * @param rawMessage An AES-256 encrypted MagicLink packet.
     */
    protected void handleMessage(String rawMessage) {
        Packet.Remote packet = null;
        try {
            packet = Packet.parseIncoming(this.aes.decrypt(rawMessage));
            this.cache.cache(packet);
        } catch (Exception ignored) {}
        if(packet == null) return;

        try {
            if (!this.self.isEquivalent(packet.remote()))
                throw new TrashedPacket("The packet isn't addressed to this server.");

            if (packet.replying()) {
                Packet.Local replyTarget = this.packetsAwaitingReply.get(packet.remote().replyEndpoint().orElseThrow());
                if (replyTarget == null) throw new TrashedPacket("This packet is a response to another packet, which isn't available to receive responses anymore.");

                replyTarget.handleReply(packet);
                return;
            }

            List<Consumer<Packet.Remote>> listeners = this.listeners.get(packet.identification().toString());
            if (listeners == null || listeners.isEmpty()) throw new TrashedPacket("No listeners exist to handle this packet.");

            Packet.Remote finalPacket = packet;
            listeners.forEach(l -> l.accept(finalPacket));
            throw new SuccessPacket();
        } catch (PacketStatusResponse e) {
            packet.status(e.status());
            packet.statusMessage(e.getMessage());
        } catch (Exception e) {
            packet.status(Packet.Status.ERROR);
            packet.statusMessage(e.getMessage());
        }
    }

    @Override
    public @NotNull String name() {
        return "MagicLink";
    }

    @Override
    public @NotNull String description() {
        return "Provides cross-server packet transmission services.";
    }

    @Override
    public @NotNull Component details() {
        return RC.Lang("rustyconnector-magicLinkDetails").generate(this);
    }

    @Override
    public boolean hasPlugins() {
        return false;
    }

    @Override
    public @NotNull List<Flux<? extends Plugin>> plugins() {
        return List.of();
    }

    public interface Packets {
        interface Handshake {
            @PacketIdentification("RC-MLH")
            class Ping extends Packet.Remote {
                public String address() {
                    return this.parameters().get(Parameters.ADDRESS).getAsString();
                }
                public Optional<String> displayName() {
                    Packet.Parameter displayName = this.parameters().get(Parameters.DISPLAY_NAME);
                    if(displayName == null) return Optional.empty();
                    return Optional.of(displayName.getAsString());
                }
                public String serverRegistration() {
                    return this.parameters().get(Parameters.SERVER_REGISTRATION).getAsString();
                }
                public Integer playerCount() {
                    return this.parameters().get(Parameters.PLAYER_COUNT).getAsInt();
                }
                public Optional<String> podName() {
                    Packet.Parameter podName = this.parameters().get(Parameters.POD_NAME);
                    if(podName == null) return Optional.empty();
                    return Optional.of(podName.getAsString());
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
            class Failure extends Packet.Remote {
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
            class Success extends Packet.Remote {
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
        class Disconnect extends Packet.Remote {
            public Disconnect(Packet packet) {
                super(packet);
            }
        }

        @PacketIdentification("RC-MLHSP")
        class StalePing extends Packet.Remote {
            public StalePing(Packet packet) {
                super(packet);
            }
        }

        @PacketIdentification("RC-R")
        class ResponsePacket extends Packet.Remote {
            public ResponsePacket(Packet packet) {
                super(packet);
            }
            public String message() {
                return this.parameters().get(Parameters.RESPONSE_MESSAGE).getAsString();
            }
            public interface Parameters {
                String RESPONSE_MESSAGE = "r";
            }
        }

        @PacketIdentification("RC-SP")
        class SendPlayer extends Packet.Remote {
            public String targetFamilyName() {
                return this.parameters().get(Parameters.TARGET_FAMILY).getAsString();
            }
            public UUID targetServer() {
                return UUID.fromString(this.parameters().get(Parameters.TARGET_SERVER).getAsString());
            }

            public UUID playerUUID() {
                return UUID.fromString(this.parameters().get(Parameters.PLAYER_UUID).getAsString());
            }

            public SendPlayer(Packet packet) {
                super(packet);
            }

            public interface Parameters {
                String TARGET_FAMILY = "f";
                String TARGET_SERVER = "s";
                String PLAYER_UUID = "p";
            }
        }
    }

    public static abstract class Server extends MagicLinkCore {
        protected final AtomicInteger delay = new AtomicInteger(5);
        protected final AtomicBoolean closed = new AtomicBoolean(false);
        protected IPV6Broadcaster broadcaster;
        protected String registrationConfiguration;

        protected Server(
                @NotNull Packet.@NotNull SourceIdentifier self,
                @NotNull AES aes,
                @NotNull MessageCache cache,
                @NotNull String registrationConfiguration,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, aes, cache);
            this.registrationConfiguration = registrationConfiguration;
            this.broadcaster = broadcaster;
        }

        /**
         * Updates the delay which is used to determine how frequently the Server
         * must ping the Proxy.
         * @param delay The delay to set in seconds.
         */
        public void setDelay(int delay) {
            this.delay.set(delay);
        }

        /**
         * Returns the registration that this Server will register into.
         * @return The server's registration.
         */
        public String registration() {
            return this.registrationConfiguration;
        }
    }

    public static abstract class Proxy extends MagicLinkCore {
        protected IPV6Broadcaster broadcaster;
        protected Map<String, ServerRegistrationConfiguration> registrationConfigurations;

        protected Proxy(
                @NotNull Packet.@NotNull SourceIdentifier self,
                @NotNull AES aes,
                @NotNull MessageCache cache,
                @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, aes, cache);
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
