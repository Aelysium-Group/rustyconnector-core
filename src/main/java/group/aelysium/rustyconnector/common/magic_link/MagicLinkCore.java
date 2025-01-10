package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketType;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class MagicLinkCore implements Particle {
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

    public static abstract class Tinder<T extends MagicLinkCore> extends RC.Plugin.Tinder<T> {
        public Tinder() {
            super(
                    "MagicLink",
                    "Provides packet communication services for the proxy.",
                    "rustyconnector-magicLinkDetails"
            );
        }
    }

    public interface Packets {
        @PacketType("RC-P")
        class Ping extends Packet.Remote {
            public String address() {
                return this.parameters().get(Parameters.ADDRESS).getAsString();
            }
            public String targetFamily() {
                return this.parameters().get(Parameters.TARGET_FAMILY).getAsString();
            }
            public Integer playerCount() {
                return this.parameters().get(Parameters.PLAYER_COUNT).getAsInt();
            }

            /**
             * A list of all metadata carried by this packet.
             * The Map should contain the exact same key-value pairs that were originally passed to it.
             */
            public @NotNull Map<String, Object> metadata() {
                try {
                    Map<String, Object> metadata = new HashMap<>();
                    this.parameters().get(Parameters.METADATA).getAsJsonObject().entrySet().forEach(e->{
                        metadata.put(e.getKey(), Parameter.fromJSON(e.getValue()).getOriginalValue());
                    });
                    return Collections.unmodifiableMap(metadata);
                } catch (Exception ignore) {}
                return Map.of();
            }

            public Ping(Packet packet) {
                super(packet);
            }

            public interface Parameters {
                String ADDRESS = "a";
                String TARGET_FAMILY = "tf";
                String PLAYER_COUNT = "pc";
                String METADATA = "m";
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
                    if(!this.flags().contains(Flag.SERVER)) return Optional.empty();
                    return genericTarget();
                } catch (NullPointerException ignore) {}
                return Optional.empty();
            }
            public Optional<String> targetFamily() {
                try {
                    if(!this.flags().contains(Flag.FAMILY)) return Optional.empty();
                    return genericTarget();
                } catch (NullPointerException ignore) {}
                return Optional.empty();
            }
            public Optional<String> genericTarget() {
                try {
                    return Optional.ofNullable(this.parameters().get(Parameters.GENERIC_TARGET).getAsString());
                } catch (NullPointerException ignore) {}
                    return Optional.empty();
            }

            public List<Flag> flags() {
                try {
                    String flagString = this.parameters().get(Parameters.FLAGS).getAsString();
                    String[] flagArray = flagString.split("");
                    List<Flag> flags = new ArrayList<>();

                    for(String f : flagArray) {
                        switch (f) {
                            case "f" -> flags.add(Flag.FAMILY);
                            case "s" -> flags.add(Flag.SERVER);
                            case "i" -> flags.add(Flag.MINIMAL);  // i because mIninmal
                            case "o" -> flags.add(Flag.MODERATE); // o because mOderate
                            case "a" -> flags.add(Flag.AGGRESSIVE);
                        }
                    }

                    return Collections.unmodifiableList(flags);
                } catch (Exception ignore) {}
                return List.of();
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
                String GENERIC_TARGET = "t";
                String PLAYER = "p";
                String FLAGS = "f";
            }

            public enum Flag {
                FAMILY,
                SERVER,
                MINIMAL,
                MODERATE,
                AGGRESSIVE
            }
        }
    }

    public static abstract class Server extends MagicLinkCore {
        protected final AtomicInteger delay = new AtomicInteger(5);
        protected IPV6Broadcaster broadcaster;

        protected Server(
                @NotNull Packet.@NotNull SourceIdentifier self,
                @NotNull AES aes,
                @NotNull PacketCache cache,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, aes, cache);
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
    }

    public static abstract class Proxy extends MagicLinkCore {
        protected IPV6Broadcaster broadcaster;
        protected Proxy(
                @NotNull Packet.@NotNull SourceIdentifier self,
                @NotNull AES aes,
                @NotNull PacketCache cache,
                @Nullable IPV6Broadcaster broadcaster
        ) {
            super(self, aes, cache);
            this.broadcaster = broadcaster;
        }
    }
}
