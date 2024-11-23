package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketType;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
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
    protected final PacketCache cache;
    protected final Packet.SourceIdentifier self;

    protected MagicLinkCore(
            @NotNull Packet.SourceIdentifier self,
            @NotNull AES aes,
            @NotNull PacketCache cache
    ) {
        this.self = self;
        this.aes = aes;
        this.cache = cache;
        packetsAwaitingReply.onTimeout(p -> RC.Adapter().log(Component.text("The packet "+p.type()+" has expired and isn't waiting for replies anymore.")));
    }

    /**
     * Registers a new packet listener to MagicLink.
     * @param listener The listener to use.
     */
    public void listen(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if(!method.isAnnotationPresent(PacketListener.class)) continue;
            PacketListener annotation = method.getAnnotation(PacketListener.class);
            try {
                Parameter firstParameter = method.getParameters()[0];

                if(!annotation.value().equals(firstParameter.getType()))
                    throw new NoSuchMethodException("Methods annotated with @PacketListener must contain a single parameter of the same class type as defined on @PacketListener. Expected "+annotation.value().getName()+" but got "+firstParameter.getType().getName());
                if(!method.getReturnType().equals(PacketListener.Response.class))
                    throw new NoSuchMethodException("Methods annotated with @PacketListener must have PacketListener.Response as the return type.");

                Class<? extends Packet.Remote> packetWrapper = (Class<? extends Packet.Remote>) firstParameter.getType();
                if(!packetWrapper.isAnnotationPresent(PacketType.class))
                    throw new NoSuchMethodException("Packet classes used for PacketListeners must be annotated with @PacketType. Caused by "+firstParameter.getType().getName());
                String type = packetWrapper.getAnnotation(PacketType.class).value();

                Constructor<? extends Packet.Remote> constructor = packetWrapper.getConstructor(Packet.class);

                this.listeners.computeIfAbsent(type, k -> new Vector<>()).add(packet -> {
                    try {
                        PacketListener.Response response = (PacketListener.Response) method.invoke(listener, constructor.newInstance(packet));
                        packet.status(response.successful, response.message);
                        if(response.shouldSendPacket() || annotation.responsesAsPacketReplies()) packet.reply(response);
                    } catch (InvocationTargetException e) {
                        RC.Error(Error.from(e));
                        if(!annotation.responseFromExceptions()) return;

                        if(e.getCause() == null) {
                            packet.status(false, e.getMessage());
                            return;
                        }
                        packet.status(false, e.getMessage());

                        if(annotation.responsesAsPacketReplies()) packet.reply(PacketListener.Response.error(e.getMessage()));
                    } catch (Exception e) {
                        RC.Error(Error.from(e));
                        if(!annotation.responseFromExceptions()) return;

                        packet.status(false, e.getMessage());

                        if(annotation.responsesAsPacketReplies()) packet.reply(PacketListener.Response.error(e.getMessage()));
                    }
                });
            } catch (Exception e) {
                RC.Error(Error.from(e).urgent(true));
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
     * If a reply is received which contains a Reply Target pointing to this packet any listeners registered in will run.
     * @param packet The packet to queue.
     */
    public void awaitReply(Packet.Local packet) {
        packetsAwaitingReply.putIfAbsent(packet.local().replyEndpoint().orElseThrow(), packet);
    }

    /**
     * Registers a new packet handler to the magic link provider.
     * @param identification The type of the specific packet.
     * @param handler The handler to use for handling the packet.
     *                This handler will be handled asynchronously and will not affect other handlers, even if it throws an exception.
     */
    public void on(String identification, Consumer<Packet.Remote> handler) {
        this.listeners.computeIfAbsent(identification, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Fetches the message cache for this magic link provider.
     */
    public PacketCache packetCache() {
        return this.cache;
    }

    public void close() {
        this.listeners.clear();
        this.cache.close();
        this.packetsAwaitingReply.close();
    }

    /**
     * Handles all the MagicLink/RustyConnector internals of handling MagicLink packets.
     * @param rawMessage A Base64 encoded, AES-256 encrypted, MagicLink packet.
     */
    protected void handleMessage(String rawMessage) {
        Packet.Remote packet;
        try {
            packet = Packet.parseIncoming(this.aes.decrypt(rawMessage));
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return;
        }
        try {
            // Not addressed to us, completely ignore it.
            if (!this.self.isEquivalent(packet.remote())) return;

            this.cache.cache(packet);

            if (packet.replying()) {
                Packet.Local replyTarget = this.packetsAwaitingReply.get(packet.remote().replyEndpoint().orElseThrow());
                if (replyTarget == null) throw new Exception("This packet is a response to another packet, which isn't available to receive responses anymore.");

                replyTarget.handleReply(packet);
                return;
            }

            List<Consumer<Packet.Remote>> listeners = this.listeners.get(packet.type().toString());
            if (listeners == null || listeners.isEmpty()) throw new Exception("No listeners exist to handle this packet.");

            listeners.forEach(l -> l.accept(packet));
        } catch (Exception e) {
            packet.status(false, e.getMessage());
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
    public @NotNull Map<String, Flux<? extends Plugin>> plugins() {
        return Map.of();
    }

    public interface Packets {
        interface Handshake {
            @PacketType("RC-P")
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

            /**
             * Contains parameters used in the case of a successful MagicLink handshake.
             */
            interface Success {
                interface Parameters {
                    String INTERVAL = "i";
                }
            }
        }

        /**
         * Indicates to the Proxy that a Server wants to disconnect.
         */
        @PacketType("RC-D")
        class Disconnect extends Packet.Remote {
            public Disconnect(Packet packet) {
                super(packet);
            }
        }

        /**
         * Indicates to a Server that it's MagicLink connection has gone stale and it needs to re-register.
         */
        @PacketType("RC-SP")
        class StalePing extends Packet.Remote {
            public StalePing(Packet packet) {
                super(packet);
            }
        }

        /**
         * A general response packet.
         */
        @PacketType("RC-R")
        class Response extends Packet.Remote {
            public Response(Packet packet) {
                super(packet);
            }
            public boolean successful() {
                return this.parameters().get(Parameters.SUCCESSFUL).getAsBoolean();
            }
            public String message() {
                return this.parameters().get(Parameters.MESSAGE).getAsString();
            }
            public interface Parameters {
                String SUCCESSFUL = "s";
                String MESSAGE = "r";
            }
        }

        @PacketType("RC-PS")
        class SendPlayer extends Packet.Remote {
            public Optional<String> targetServer() {
                try {
                    return Optional.ofNullable(this.parameters().get(Parameters.TARGET_SERVER).getAsString());
                } catch (NullPointerException ignore) {}
                return Optional.empty();
            }
            public Optional<String> targetFamily() {
                try {
                    return Optional.ofNullable(this.parameters().get(Parameters.TARGET_SERVER).getAsString());
                } catch (NullPointerException ignore) {}
                return Optional.empty();
            }
            public Optional<String> genericTarget() {
                try {
                    return Optional.ofNullable(this.parameters().get(Parameters.GENERIC_TARGET).getAsString());
                } catch (NullPointerException ignore) {}
                    return Optional.empty();
            }

            public Optional<UUID> playerUUID() {
                try {
                    return Optional.of(UUID.fromString(this.parameters().get(Parameters.PLAYER).getAsString()));
                } catch (Exception ignore) {}
                return Optional.empty();
            }
            public Optional<String> playerUsername() {
                try {
                    UUID.fromString(this.parameters().get(Parameters.PLAYER).getAsString());
                    return Optional.empty();
                } catch (Exception ignore) {}
                return Optional.of(this.parameters().get(Parameters.PLAYER).getAsString());
            }

            /**
             * @return The player identifier. This can either be a UUID, or the player's username.
             *         Use either {@link #playerUsername()} or {@link #playerUUID()} to get the specific value.
             */
            public String player() {
                return this.parameters().get(Parameters.PLAYER).getAsString();
            }

            public SendPlayer(Packet packet) {
                super(packet);
            }

            public interface Parameters {
                String TARGET_FAMILY = "f";
                String TARGET_SERVER = "s";
                String GENERIC_TARGET = "t";
                String PLAYER = "p";
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
                @NotNull PacketCache cache,
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
                @NotNull PacketCache cache,
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
