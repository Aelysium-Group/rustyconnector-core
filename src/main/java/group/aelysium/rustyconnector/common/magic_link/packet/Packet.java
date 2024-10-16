package group.aelysium.rustyconnector.common.magic_link.packet;

import com.google.gson.*;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.magic_link.exceptions.PacketStatusResponse;
import group.aelysium.rustyconnector.common.magic_link.exceptions.SuccessPacket;
import group.aelysium.rustyconnector.common.util.JSONParseable;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.util.ThrowableConsumer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Packet implements JSONParseable {
    protected static final int protocolVersion = 3;

    protected final int messageVersion;
    protected final Identification identification;
    protected final SourceIdentifier local;
    protected final SourceIdentifier remote;
    protected final Map<String, Parameter> parameters;

    /**
     * The protocol version used by this packet.
     */
    public int messageVersion() { return this.messageVersion; }

    /**
     * The source that sent this packet.
     */
    public SourceIdentifier local() { return this.local; }

    /**
     * The source that's supposed to receive this packet.
     */
    public SourceIdentifier remote() { return this.remote; }

    /**
     * The identification of this packet.
     */
    public Identification identification() { return this.identification; }

    /**
     * The extra parameters that this packet caries.
     */
    public Map<String, Parameter> parameters() { return parameters; }

    /**
     * Checks whether this packet is a response to a previous packet.
     * @return `true` if this packet is a response to another packet. `false` otherwise.
     */
    public boolean replying() {
        return this.remote().replyEndpoint().isPresent();
    }
    
    private Packet(@NotNull Integer version, @NotNull Identification identification, @NotNull Packet.SourceIdentifier local, @NotNull Packet.SourceIdentifier remote, @NotNull Map<String, Parameter> parameters) {
        this.messageVersion = version;
        this.identification = identification;
        this.local = local;
        this.remote = remote;
        this.parameters = parameters;
    }

    /**
     * Returns the message as a string.
     * The returned string is actually the raw message that was received or is able to be sent through Redis.
     * @return The message as a string.
     */
    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JsonObject toJSON() {
        JsonObject object = new JsonObject();

        object.add(Parameters.PROTOCOL_VERSION, new JsonPrimitive(this.messageVersion));
        object.add(Parameters.IDENTIFICATION, new JsonPrimitive(this.identification.toString()));
        object.add(Parameters.LOCAL, this.local.toJSON());
        object.add(Parameters.REMOTE, this.remote.toJSON());

        JsonObject parameters = new JsonObject();
        this.parameters.forEach((key, value) -> parameters.add(key, value.toJSON()));
        object.add(Parameters.PARAMETERS, parameters);

        return object;
    }

    public static Builder New() {
        return new Builder();
    }

    protected static class NakedBuilder {
        private Integer protocolVersion = Packet.protocolVersion;
        private Identification id;
        private SourceIdentifier local;
        private SourceIdentifier remote;
        private final Map<String, Parameter> parameters = new HashMap<>();

        public NakedBuilder identification(@NotNull Identification id) {
            this.id = id;
            return this;
        }

        public NakedBuilder local(@NotNull Packet.SourceIdentifier local) {
            this.local = local;
            return this;
        }

        public NakedBuilder remote(@NotNull Packet.SourceIdentifier remote) {
            this.remote = remote;
            return this;
        }

        public NakedBuilder parameter(@NotNull String key, @NotNull String value) {
            this.parameters.put(key, new Parameter(value));
            return this;
        }
        public NakedBuilder parameter(@NotNull String key, @NotNull Parameter value) {
            this.parameters.put(key, value);
            return this;
        }

        protected NakedBuilder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Packet.Remote buildRemote() {
            return new Packet.Remote(this.protocolVersion, this.id, this.local, this.remote, this.parameters);
        }
        public Packet.Local buildLocal() {
            return new Packet.Local(this.protocolVersion, this.id, this.local, this.remote, this.parameters);
        }
    }

    // This implementation feels chunky. That's intentional, it's specifically written so that `.build()` isn't available until all the required params are filled in.
    public static class Builder {
        private final NakedBuilder builder = new NakedBuilder();

        protected Builder() {}

        public static class PrepareForSending {
            private final NakedBuilder builder;

            protected PrepareForSending(NakedBuilder builder) {
                this.builder = builder;
            }

            public PrepareForSending parameter(String key, String value) {
                this.builder.parameter(key, new Parameter(value));
                return this;
            }

            public PrepareForSending parameter(String key, Parameter value) {
                this.builder.parameter(key, value);
                return this;
            }

            /**
             * Addresses the packet to the specified {@link SourceIdentifier}.
             */
            public ReadyForSending addressTo(SourceIdentifier remote) {
                this.builder.local(SourceIdentifier.localSource());
                this.builder.remote(remote);

                return new ReadyForSending(this.builder);
            }

            /**
             * Prepares the packet as a reply to the specified {@link Packet}.
             * If the specified packet doesn't have at least one listen on it, this address will do absolutly nothing.
             * @throws RuntimeException If this packet was already sent or used in a reply, and then you try to send it again.
             */
            public ReadyForSending addressTo(Packet targetPacket) {
                this.builder.local(SourceIdentifier.localSource());
                this.builder.remote(targetPacket.local());

                return new ReadyForSending(this.builder);
            }
        }
        public static class ReadyForSending {
            private final NakedBuilder builder;

            protected ReadyForSending(NakedBuilder builder) {
                this.builder = builder;
            }

            /**
             * Sends the packet.
             * This method resolves the currently active MagicLink provider and calls {@link MagicLinkCore#publish(Packet.Local)}.
             * @throws RuntimeException If there was an issue sending the packet.
             */
            public Packet.Local send() throws RuntimeException {
                Packet.Local packet = this.builder.buildLocal();

                MagicLinkCore magicLink = RC.MagicLink();
                magicLink.publish(packet);

                return packet;
            }
        }

        /**
         * The identification of this packet.
         * Identification is what differentiates a "Server ping packet" from a "Teleport player packet"
         */
        public PrepareForSending identification(Identification id) {
            return new PrepareForSending(builder.identification(id));
        }
    }

    public static Packet.Remote parseIncoming(String rawMessage) {
        Gson gson = new Gson();
        JsonObject messageObject = gson.fromJson(rawMessage, JsonObject.class);

        NakedBuilder builder = new NakedBuilder();

        builder.protocolVersion(messageObject.get(Parameters.PROTOCOL_VERSION).getAsInt());
        builder.identification(new Identification(messageObject.get(Parameters.IDENTIFICATION).getAsString()));
        builder.local(SourceIdentifier.fromJSON(messageObject.get(Parameters.LOCAL).getAsJsonObject()));
        builder.remote(SourceIdentifier.fromJSON(messageObject.get(Parameters.REMOTE).getAsJsonObject()));

        messageObject.get(Parameters.PARAMETERS).getAsJsonObject().entrySet().forEach(entry -> {
            String key = entry.getKey();
            Parameter value = new Parameter(entry.getValue().getAsJsonPrimitive());

            builder.parameter(key, value);
        });

        return builder.buildRemote();
    }

    public static class SourceIdentifier implements JSONParseable {
        private final UUID uuid;
        private final Origin origin;
        private NanoID replyEndpoint;

        private SourceIdentifier(UUID uuid, @NotNull Origin origin) {
            this.uuid = uuid;
            this.origin = origin;
        }

        public UUID uuid() {
            return this.uuid;
        }
        public Origin origin() {
            return this.origin;
        }

        /**
         * Sets the reply endpoint for this source.
         * If this source represents the local machine, then this value is the uuid which can be used to reply to this specific packet.
         * If this source represents a remote machine, then this value is the uuid which a packet can be addressed to in order to reply to a packet received.
         */
        public void replyEndpoint(NanoID replyEndpoint) {
            this.replyEndpoint = replyEndpoint;
        }

        /**
         * The reply endpoint for this source.
         * If this source represents the local machine, then this value is the uuid which can be used to reply to this specific packet.
         * If this source represents a remote machine, then this value is the uuid which a packet can be addressed to in order to reply to a packet received.
         */
        public Optional<NanoID> replyEndpoint() {
            return Optional.ofNullable(this.replyEndpoint);
        }

        public JsonObject toJSON() {
            JsonObject object = new JsonObject();

            if(this.uuid != null) object.add("u", new JsonPrimitive(this.uuid.toString()));
            object.add("n", new JsonPrimitive(Origin.toInteger(this.origin)));
            if(this.replyEndpoint != null) object.add("r", new JsonPrimitive(this.replyEndpoint.toString()));

            return object;
        }

        public static SourceIdentifier fromJSON(JsonObject object) {
            SourceIdentifier source = new SourceIdentifier(
                    object.get("u") == null ? null : UUID.fromString(object.get("u").getAsString()),
                    Origin.fromInteger(object.get("n").getAsInt())
            );
            if(object.get("r") != null) source.replyEndpoint(NanoID.fromString(object.get("r").getAsString()));

            return source;
        }

        public static SourceIdentifier server(UUID uuid) {
            return new SourceIdentifier(uuid, Origin.SERVER);
        }
        public static SourceIdentifier proxy(UUID uuid) {
            return new SourceIdentifier(uuid, Origin.PROXY);
        }
        public static SourceIdentifier allAvailableProxies() {
            return new SourceIdentifier(null, Origin.ANY_PROXY);
        }
        public static SourceIdentifier allAvailableServers() {
            return new SourceIdentifier(null, Origin.ANY_SERVER);
        }

        /**
         * Resolves a SourceIdentifier based on the kernel of RustyConnector running.
         * This method will also assign a unique reply endpoint as well.
         * @return A new SourceIdentifier representing the machine calling this method.
         */
        public static SourceIdentifier localSource() {
            try {
                SourceIdentifier source = SourceIdentifier.server(RC.S.Kernel().uuid());
                source.replyEndpoint(NanoID.randomNanoID());
                return source;
            } catch (Exception ignore) {}
            try {
                SourceIdentifier source = SourceIdentifier.proxy(RC.P.Kernel().uuid());
                source.replyEndpoint(NanoID.randomNanoID());
                return source;
            } catch (Exception ignore) {}
            throw new RuntimeException("No available kernel existed in order to get your local source! How did you even fuck up bad enough to get this exception?");
        }

        /**
         * Checks if the passed remote can be considered the same as `this`.
         * For example, if `this` is of type {@link Origin#PROXY} and `remote` is of type {@link Origin#ANY_PROXY} this will return `true`
         * because `this` would be considered a part of `ANY_PROXY`.
         * @param target Some other remote.
         * @return `true` if the other remote is a valid way of identifying `this` remote. `false` otherwise.
         */
        public boolean isEquivalent(SourceIdentifier target) {
            // If the two match as defined by default expected behaviour, return true.
            if(Objects.equals(this, target)) return true;

            // If one of the two is of type "ANY_PROXY" and the other is of type "PROXY", return true.
            return (this.origin == Origin.ANY_PROXY && target.origin == Origin.PROXY) ||
                    (this.origin == Origin.PROXY && target.origin == Origin.ANY_PROXY) ||
                    (this.origin == Origin.ANY_PROXY && target.origin == Origin.ANY_PROXY) ||
                    (this.origin == Origin.ANY_SERVER && target.origin == Origin.SERVER) ||
                    (this.origin == Origin.SERVER && target.origin == Origin.ANY_SERVER) ||
                    (this.origin == Origin.ANY_SERVER && target.origin == Origin.ANY_SERVER);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceIdentifier target = (SourceIdentifier) o;

            // If the two match as defined by default expected behaviour, return true.
            return Objects.equals(uuid, target.uuid) && Objects.equals(origin, target.origin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, origin);
        }

        public enum Origin {
            PROXY,
            ANY_PROXY,
            SERVER,
            ANY_SERVER
            ;

            public static Origin fromInteger(int number) {
                return switch (number) {
                    case 0 -> Origin.PROXY;
                    case 1 -> Origin.ANY_PROXY;
                    case 2 -> Origin.SERVER;
                    case 3 -> Origin.ANY_SERVER;
                    default -> throw new ClassCastException(number+" has no associated value!");
                };
            }
            public static int toInteger(Origin origin) {
                return switch (origin) {
                    case PROXY -> 0;
                    case ANY_PROXY -> 1;
                    case SERVER -> 2;
                    case ANY_SERVER -> 3;
                };
            }
        }
    }

    /**
     * A convenience wrapper which allows the caller to pass just one, Packet,
     * parameter to the constructor instead of all the individual parameters.
     * Callers are free to use this, or to extend {@link Packet} directly.
     */
    public static class Wrapper extends Packet {
        public Wrapper(Packet packet) {
            super(
                    packet.messageVersion(),
                    packet.identification(),
                    packet.local(),
                    packet.remote(),
                    packet.parameters()
            );
        }

        /**
         * Converts the packet to a {@link Local}.
         * This will fail if said packet WAS NOT created on this machine.
         * @return A Local packet.
         * @throws IllegalStateException If the packet is not actually local.
         */
        public Local toLocal() throws IllegalStateException {
            SourceIdentifier local = SourceIdentifier.localSource();
            if(!this.local.isEquivalent(local)) throw new IllegalStateException("Packet is not fit to be converted to a local packet!");
            if(this.remote.isEquivalent(local)) throw new IllegalStateException("Packet is not fit to be converted to a local packet!");
            return new Local(this.messageVersion, this.identification, this.local, this.remote, this.parameters);
        }

        /**
         * Converts the packet to a {@link Remote}.
         * This will fail if said packet WAS created on this machine.
         * @return A Remote packet.
         * @throws IllegalStateException If the packet is not actually remote.
         */
        public Remote toRemote() throws IllegalStateException {
            SourceIdentifier local = SourceIdentifier.localSource();
            if(!this.remote.isEquivalent(local)) throw new IllegalStateException("Packet is not fit to be converted to a remote packet!");
            if(this.local.isEquivalent(local)) throw new IllegalStateException("Packet is not fit to be converted to a remote packet!");
            return new Remote(this.messageVersion, this.identification, this.local, this.remote, this.parameters);
        }
    }

    /**
     * A Packet which has been created by the system it's currently on.
     */
    public static class Local extends Packet {
        private Vector<ThrowableConsumer<Remote, Throwable>> catchAlls = null;
        private ConcurrentHashMap<Identification, Vector<ThrowableConsumer<Packet.Remote, Throwable>>> replyListeners = null;

        public Local(@NotNull Integer version, @NotNull Identification identification, @NotNull Packet.SourceIdentifier sender, @NotNull Packet.SourceIdentifier target, @NotNull Map<String, Parameter> parameters) {
            super(
                    version,
                    identification,
                    sender,
                    target,
                    parameters
            );
        }
        public Local(@NotNull Packet packet) {
            this(
                    packet.messageVersion,
                    packet.identification,
                    packet.local,
                    packet.remote,
                    packet.parameters
            );
        }

        public void handleReply(Packet.Remote packet) throws IllegalCallerException {
            if(this.replyListeners != null && this.replyListeners.containsKey(packet.identification))
                this.replyListeners.get(packet.identification).forEach(l -> {
                    try {
                        l.accept(packet);
                        throw new SuccessPacket();
                    } catch (PacketStatusResponse e) {
                        packet.status(e.status());
                        packet.statusMessage(e.getMessage());
                    } catch (Throwable e) {
                        packet.status(Status.ERROR);
                        packet.statusMessage(e.getMessage());
                    }
                });
            if(this.catchAlls != null)
                this.catchAlls.forEach(l -> {
                    try {
                        l.accept(packet);
                        throw new SuccessPacket();
                    } catch (PacketStatusResponse e) {
                        packet.status(e.status());
                        packet.statusMessage(e.getMessage());
                    } catch (Throwable e) {
                        packet.status(Status.ERROR);
                        packet.statusMessage(e.getMessage());
                    }
                });
        }

        /**
         * Returns any packets which are sent as a reply to this one.
         * It should be noted that, unless this method is run at least once,
         * it will be impossible for packets to be sent as a response to this one.
         * @param handler The handler for the packet.
         */
        public void onReply(@NotNull ThrowableConsumer<Packet.Remote, Throwable> handler) {
            if(this.catchAlls == null) this.catchAlls = new Vector<>();
            this.catchAlls.add(handler);

            try {
                RC.S.MagicLink().awaitReply(this);
            } catch (Exception ignore) {}
            try {
                RC.P.MagicLink().awaitReply(this);
            } catch (Exception ignore) {}
        }

        /**
         * Returns the packet which was sent as a reply to this one.
         * This method specifically listens for certain packets and will only run if a packet with the specific id is received
         * It should be noted that, unless this method is run at least once,
         * it will be impossible for packets to be handled as a response to this one.
         * @param packetClass The class of the packet to listen for. Whatever the class is, it must be annotated with the {@link PacketIdentification} annotation. If it's not, this method will just do nothing.
         * @param handler The handler for the packet.
         */
        public void onReply(@NotNull Class<? extends Remote> packetClass, @NotNull ThrowableConsumer<Packet.Remote, Throwable> handler) {
            if(!packetClass.isAnnotationPresent(PacketIdentification.class)) return;
            PacketIdentification annotation = packetClass.getAnnotation(PacketIdentification.class);
            Identification identification = Identification.parseString(annotation.value());

            if(this.replyListeners == null) this.replyListeners = new ConcurrentHashMap<>();
            this.replyListeners.putIfAbsent(identification, new Vector<>());
            this.replyListeners.get(identification).add(handler);

            try {
                RC.S.MagicLink().awaitReply(this);
            } catch (Exception ignore) {}
            try {
                RC.P.MagicLink().awaitReply(this);
            } catch (Exception ignore) {}
        }
    }

    /**
     * A packet which has been created by some other system.
     */
    public static class Remote extends Packet {
        protected Instant received = Instant.now();
        protected Status status = Status.UNDEFINED;
        protected String statusMessage = "No status has been assigned to this packet.";

        public Remote(@NotNull Integer version, @NotNull Identification identification, @NotNull Packet.SourceIdentifier sender, @NotNull Packet.SourceIdentifier target, @NotNull Map<String, Parameter> parameters) {
            super(
                    version,
                    identification,
                    sender,
                    target,
                    parameters
            );
        }
        public Remote(@NotNull Packet packet) {
            this(
                    packet.messageVersion,
                    packet.identification,
                    packet.local,
                    packet.remote,
                    packet.parameters
            );
        }

        public Instant received() {
            return this.received;
        }
        public NanoID id() {
            return this.local.replyEndpoint;
        }

        public void status(Status status) {
            this.status = status;
        }
        public Status status() {
            return this.status;
        }

        public void statusMessage(String statusMessage) {
            if(statusMessage == null) {
                this.statusMessage = "No status message has been provided.";
                return;
            }
            this.statusMessage = statusMessage;
        }
        public String statusMessage() {
            return this.statusMessage;
        }

        /**
         * Returns the source identifier of the machine that sent this packet.
         * @return A SourceIdentifier.
         */
        @Override
        public SourceIdentifier local() {
            return super.local();
        }

        /**
         * Returns the source identifier of the machine that this packet is addressed to.
         * You should really only ever be able to see this if you're receiving this Packet.
         * @return A SourceIdentifier.
         */
        @Override
        public SourceIdentifier remote() {
            return super.remote();
        }
    }

    interface Parameters {
        String PROTOCOL_VERSION = "v";
        String IDENTIFICATION = "i";
        String LOCAL = "s";
        String REMOTE = "t";
        String PARAMETERS = "p";
    }

    public enum Status {
        UNDEFINED,
        BAD_ATTITUDE,
        WRONG_SOURCE,
        TRASHED,
        SUCCESS,
        ERROR,
        CANCELED;

        public NamedTextColor color() {
            if(this == BAD_ATTITUDE) return NamedTextColor.DARK_GRAY;
            if(this == WRONG_SOURCE) return NamedTextColor.DARK_GRAY;
            if(this == TRASHED) return NamedTextColor.DARK_GRAY;
            if(this == SUCCESS) return NamedTextColor.GREEN;
            if(this == ERROR) return NamedTextColor.DARK_RED;
            if(this == CANCELED) return NamedTextColor.YELLOW;
            return NamedTextColor.GRAY;
        }
    }

    public static class Identification {
        protected String id;

        public Identification(String id) {
            this.id = id;
        }

        public String get() {
            return this.id;
        }

        @Override
        public String toString() {
            return this.id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Identification mapping = (Identification) o;
            return Objects.equals(this.get(), mapping.get());
        }

        @Override
        public int hashCode() {
            return Objects.hash(get());
        }

        /**
         * Create a new Identification from a namespace and packetID.
         * @param namespace
         *        Should be a name representing your plugin.<br>
         *        Should be in the format of UPPER_SNAKE_CASE.<br>
         *        Should start with the prefix `RC_`.<br>
         *        Example: `RC_COMMAND_SYNC`.<br>
         * @param packetID
         *        The ID you want to assign this packet.<br>
         *        Should be in the format of UPPER_SNAKE_CASE.<br>
         *        Can be whatever you want.<br>
         * @return {@link Identification}
         * @throws IllegalArgumentException If illegal names are passed.
         */
        public static Identification from(@NotNull String namespace, @NotNull String packetID) throws IllegalArgumentException {
            String idToCheck = namespace.toUpperCase();
            if(idToCheck.isEmpty()) throw new IllegalArgumentException("pluginID can't be empty!");
            if(packetID.isEmpty()) throw new IllegalArgumentException("packetID can't be empty!");

            return new Identification(namespace + "-" + packetID);
        }

        /**
         * Create a new Identification from the passed string.
         * @param value Must be in the format `[namespace]-[packetID]`.
         * @return {@link Identification}
         * @throws IllegalArgumentException If illegal names are passed.
         */
        public static Identification parseString(@NotNull String value) throws IllegalArgumentException {
            String[] tokens = value.split("-");
            if(tokens.length > 2) throw new IllegalArgumentException("Invalid identification passed.");
            return new Identification(value.toUpperCase());
        }
    }

    public static class Parameter {
        protected char type;
        protected Object object;

        private Parameter(@NotNull Object object, char type) {
            this.object = object;
            this.type = type;
        }
        public Parameter(@NotNull Number object) {
            this(object, 'n');
        }
        public Parameter(@NotNull Boolean object) {
            this(object, 'b');
        }
        public Parameter(@NotNull String object) {
            this(object, 's');
        }
        public Parameter(@NotNull JsonArray object) {
            this(object, 'a');
        }
        public Parameter(@NotNull JsonObject object) {
            this(object, 'j');
        }
        public Parameter(@NotNull JsonPrimitive object) {
            if(object.isNumber()) {
                this.object = object.getAsNumber();
                this.type = 'n';
                return;
            }
            if(object.isBoolean()) {
                this.object = object.getAsBoolean();
                this.type = 'b';
                return;
            }
            if(object.isString()) {
                this.object = object.getAsString();
                this.type = 's';
                return;
            }
            if(object.isJsonArray()) {
                this.object = object.getAsJsonArray();
                this.type = 'a';
                return;
            }
            if(object.isJsonObject()) {
                this.object = object.getAsJsonObject();
                this.type = 'j';
                return;
            }
            throw new IllegalStateException("Unexpected value: " + type);
        }

        public char type() {
            return this.type;
        }

        public int getAsInt() {
            return ((Number) this.object).intValue();
        }
        public long getAsLong() {
            return ((Number) this.object).longValue();
        }
        public double getAsDouble() {
            return ((Number) this.object).doubleValue();
        }
        public boolean getAsBoolean() {
            return (boolean) this.object;
        }
        public String getAsString() {
            return (String) this.object;
        }
        public UUID getStringAsUUID() {
            return UUID.fromString(this.getAsString());
        }
        public JsonArray getAsJsonArray() {
            return (JsonArray) this.object;
        }
        public JsonObject getAsJsonObject() {
            return (JsonObject) this.object;
        }

        public JsonElement toJSON() {
            return switch (type) {
                case 'n' -> new JsonPrimitive((Number) this.object);
                case 'b' -> new JsonPrimitive((Boolean) this.object);
                case 's' -> new JsonPrimitive((String) this.object);
                case 'a' -> (JsonArray) this.object;
                case 'j' -> (JsonObject) this.object;
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        }
    }
}