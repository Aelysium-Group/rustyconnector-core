package group.aelysium.rustyconnector.common.magic_link.packet;

import com.google.gson.*;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.JSONParseable;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.RC;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The base Packet class.
 */
public abstract class Packet implements JSONParseable {
    protected static final int protocolVersion = 3;

    protected final int messageVersion;
    protected final Instant created = Instant.now();
    protected final Type type;
    protected final SourceIdentifier local;
    protected final SourceIdentifier remote;
    protected final Map<String, Parameter> parameters;
    protected boolean successful = false;
    protected String statusMessage = "The packet was parsed properly but has not been processed in any way.";
    protected NanoID cacheID = null;

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
    public @NotNull SourceIdentifier remote() { return this.remote; }

    /**
     * The type of this packet.
     */
    public @NotNull Packet.Type type() { return this.type; }

    /**
     * The extra parameters that this packet caries.
     */
    public @NotNull Map<String, Parameter> parameters() { return parameters; }

    /**
     * Checks whether this packet is a response to a previous packet.
     * @return `true` if this packet is a response to another packet. `false` otherwise.
     */
    public boolean replying() {
        return this.remote().replyEndpoint().isPresent();
    }

    /**
     * @return The exact time that the packet was created.
     */
    public @NotNull Instant created() {
        return this.created;
    }

    /**
     * Whether the packet has successfully achieved its mission.<br/>
     * A packet's "mission" depends on the context.<br/>
     * If the packet is {@link Local}, it's only goal is to be sent successfully.<br/>
     * If a packet is {@link Remote}, whether it achieves its goal is dictated by the handler(s) handling it.<br/>
     * <br/>
     * See {@link #statusMessage()} for details on the current status of the packet.
     * @return `true` if and only if the packet has achieved it's intended mission.
     */
    public boolean successful() {
        return this.successful;
    }

    /**
     * @return The message describing the current status of the packet.
     */
    public @NotNull String statusMessage() {
        return this.statusMessage;
    }

    public @Nullable NanoID cacheID() {
        return this.cacheID;
    }

    /**
     * @return `true` if the packet originated from this server. `false` if the packet is a remote packet.
     *          More specifically, `false` if the packet is of type {@link Remote}.
     */
    public abstract boolean isLocal();
    
    private Packet(@NotNull Integer version, @NotNull Packet.Type type, @NotNull Packet.SourceIdentifier local, @NotNull Packet.SourceIdentifier remote, @NotNull Map<String, Parameter> parameters) {
        this.messageVersion = version;
        this.type = type;
        this.local = local;
        this.remote = remote;
        this.parameters = parameters;
    }

    public void status(boolean successful, @Nullable String message) {
        this.successful = successful;
        this.statusMessage = message == null ? "No message was provided for this status." : message;
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
        object.add(Parameters.IDENTIFICATION, new JsonPrimitive(this.type.toString()));
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
        private Type type;
        private SourceIdentifier local;
        private SourceIdentifier remote;
        private final Map<String, Parameter> parameters = new HashMap<>();

        public NakedBuilder type(@NotNull Packet.Type type) {
            this.type = type;
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
            return new Packet.Remote(this.protocolVersion, this.type, this.local, this.remote, this.parameters);
        }
        public Packet.Local buildLocal() {
            return new Packet.Local(this.protocolVersion, this.type, this.local, this.remote, this.parameters);
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
         * The type of this packet.
         * Identification is what differentiates a "Server ping packet" from a "Teleport player packet"
         */
        public PrepareForSending identification(Type id) {
            return new PrepareForSending(builder.type(id));
        }
    }

    public static Packet.Remote parseIncoming(String rawMessage) {
        Gson gson = new Gson();
        JsonObject messageObject = gson.fromJson(rawMessage, JsonObject.class);

        NakedBuilder builder = new NakedBuilder();

        builder.protocolVersion(messageObject.get(Parameters.PROTOCOL_VERSION).getAsInt());
        builder.type(new Type(messageObject.get(Parameters.IDENTIFICATION).getAsString()));
        builder.local(SourceIdentifier.fromJSON(messageObject.get(Parameters.LOCAL).getAsJsonObject()));
        builder.remote(SourceIdentifier.fromJSON(messageObject.get(Parameters.REMOTE).getAsJsonObject()));

        messageObject.get(Parameters.PARAMETERS).getAsJsonObject().entrySet().forEach(entry -> {
            String key = entry.getKey();
            Parameter value = new Parameter(entry.getValue().getAsJsonPrimitive());

            builder.parameter(key, value);
        });

        return builder.buildRemote();
    }

    /**
     * Identifies the source of a packet across various machines.
     */
    public static class SourceIdentifier implements JSONParseable {
        private final String id;
        private final Origin origin;
        private NanoID replyEndpoint;

        private SourceIdentifier(String id, @NotNull Origin origin) {
            this.id = id;
            this.origin = origin;
        }

        public String id() {
            return this.id;
        }
        public Origin origin() {
            return this.origin;
        }

        /**
         * Sets the reply endpoint for this source.
         * If this source represents the local machine, then this value is the id which can be used to reply to this specific packet.
         * If this source represents a remote machine, then this value is the id which a packet can be addressed to in order to reply to a packet received.
         */
        public void replyEndpoint(NanoID replyEndpoint) {
            this.replyEndpoint = replyEndpoint;
        }

        /**
         * The reply endpoint for this source.
         * If this source represents the local machine, then this value is the id which can be used to reply to this specific packet.
         * If this source represents a remote machine, then this value is the id which a packet can be addressed to in order to reply to a packet received.
         */
        public Optional<NanoID> replyEndpoint() {
            return Optional.ofNullable(this.replyEndpoint);
        }

        public JsonObject toJSON() {
            JsonObject object = new JsonObject();

            if(this.id != null) object.add("u", new JsonPrimitive(this.id));
            object.add("n", new JsonPrimitive(Origin.toInteger(this.origin)));
            if(this.replyEndpoint != null) object.add("r", new JsonPrimitive(this.replyEndpoint.toString()));

            return object;
        }

        public static SourceIdentifier fromJSON(JsonObject object) {
            SourceIdentifier source = new SourceIdentifier(
                    object.get("u") == null ? null : object.get("u").getAsString(),
                    Origin.fromInteger(object.get("n").getAsInt())
            );
            if(object.get("r") != null) source.replyEndpoint(NanoID.fromString(object.get("r").getAsString()));

            return source;
        }

        public static SourceIdentifier server(@NotNull String id) {
            return new SourceIdentifier(id, Origin.SERVER);
        }
        public static SourceIdentifier proxy(@NotNull String id) {
            return new SourceIdentifier(id, Origin.PROXY);
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
                SourceIdentifier source = SourceIdentifier.server(RC.S.Kernel().id());
                source.replyEndpoint(NanoID.randomNanoID());
                return source;
            } catch (Exception ignore) {}
            try {
                SourceIdentifier source = SourceIdentifier.proxy(RC.P.Kernel().id());
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
            return Objects.equals(id, target.id) && Objects.equals(origin, target.origin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, origin);
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
     * A Packet which has been created by the system it's currently on.
     * More specifically, if you use {@link Builder} to create a packet, it will be local.
     * Local packets can be sent and await replies.
     */
    public static class Local extends Packet {
        private Vector<PacketListener.Function<Remote, PacketListener.Response>> catchAlls = null;
        private ConcurrentHashMap<Class<? extends Remote>, Vector<PacketListener.Function<Remote, PacketListener.Response>>> replyListeners = null;
        private ConcurrentHashMap<Type, Class<? extends Remote>> packetTypeMappings = null;

        public Local(@NotNull Integer version, @NotNull Packet.Type type, @NotNull Packet.SourceIdentifier sender, @NotNull Packet.SourceIdentifier target, @NotNull Map<String, Parameter> parameters) {
            super(
                    version,
                    type,
                    sender,
                    target,
                    parameters
            );
        }
        public Local(@NotNull Packet packet) {
            this(
                    packet.messageVersion,
                    packet.type,
                    packet.local,
                    packet.remote,
                    packet.parameters
            );
        }

        public final boolean isLocal() {
            return true;
        }

        public <P extends Remote> void handleReply(P packet) throws Exception {
            if (this.replyListeners == null && this.catchAlls == null) {
                packet.status(false, "No listeners exist to handle this packet.");
                return;
            }
            if(this.catchAlls != null)
                this.catchAlls.forEach(l -> {
                    try {
                        PacketListener.Response response = l.apply(packet);
                        packet.status(response.successful, response.message);
                        if(response.shouldSendPacket) packet.reply(response);
                    } catch (Throwable e) {
                        RC.Error(Error.from(e));
                        packet.status(false, e.getMessage());
                    }
                });
            if (this.replyListeners != null && this.packetTypeMappings != null) {
                Class<? extends Remote> wrapperClass = this.packetTypeMappings.get(packet.type());
                if(wrapperClass == null) return;

                Remote wrapped = wrapperClass.getConstructor(Packet.class).newInstance(packet);

                this.replyListeners.get(wrapperClass).forEach(l -> {
                    try {
                        PacketListener.Response response = l.apply(wrapped);
                        packet.status(response.successful, response.message);
                        if (response.shouldSendPacket()) packet.reply(response);
                    } catch (Throwable e) {
                        RC.Error(Error.from(e));
                        packet.status(false, e.getMessage());
                    }
                });
            }
        }

        /**
         * Returns any packets which are sent as a reply to this one.
         * It should be noted that, unless this method is run at least once,
         * it will be impossible for packets to be sent as a response to this one.
         * @param handler The handler for the packet.
         */
        public void onReply(@NotNull PacketListener.Function<? extends Remote, PacketListener.Response> handler) {
            if(this.catchAlls == null) this.catchAlls = new Vector<>();
            this.catchAlls.add((PacketListener.Function<Remote, PacketListener.Response>) handler);
            RC.MagicLink().awaitReply(this);
        }

        /**
         * Returns the packet which was sent as a reply to this one.
         * This method specifically listens for certain packets and will only run if a packet with the specific id is received
         * It should be noted that, unless this method is run at least once,
         * it will be impossible for packets to be handled as a response to this one.
         * @param clazz The class of the packet to listen for.
         * @param handler The handler for the packet.
         */
        public <T extends Remote> void onReply(@NotNull Class<T> clazz, @NotNull PacketListener.Function<T, PacketListener.Response> handler) {
            if(!clazz.isAnnotationPresent(PacketType.class)) {
                RC.Error(Error.from("Packet classes used for PacketListeners must be annotated with @PacketType.").whileAttempting("To register a new packet listener."));
                return;
            }
            PacketType annotation = clazz.getAnnotation(PacketType.class);
            if(this.packetTypeMappings == null) this.packetTypeMappings = new ConcurrentHashMap<>();
            this.packetTypeMappings.putIfAbsent(Type.parseString(annotation.value()), clazz);

            if(this.replyListeners == null) this.replyListeners = new ConcurrentHashMap<>();
            this.replyListeners.computeIfAbsent(clazz, k->new Vector<>()).add((PacketListener.Function<Remote, PacketListener.Response>) handler);

            RC.MagicLink().awaitReply(this);
        }
    }

    /**
     * A packet which has been created by some other system.
     */
    public static class Remote extends Packet {
        public Remote(@NotNull Integer version, @NotNull Packet.Type type, @NotNull Packet.SourceIdentifier sender, @NotNull Packet.SourceIdentifier target, @NotNull Map<String, Parameter> parameters) {
            super(
                    version,
                    type,
                    sender,
                    target,
                    parameters
            );
        }
        public Remote(@NotNull Packet packet) {
            this(
                    packet.messageVersion,
                    packet.type,
                    packet.local,
                    packet.remote,
                    packet.parameters
            );
        }

        public final boolean isLocal() {
            return false;
        }

        public NanoID id() {
            return this.local.replyEndpoint;
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
        public @NotNull SourceIdentifier remote() {
            return super.remote();
        }

        /**
         * Creates and sends a simple response packet.
         * Specifically generates a {@link MagicLinkCore.Packets.Response} packet and sends it.
         * @param response The response data to generate a packet from.
         * @return The packet that was sent.
         */
        public @NotNull Packet.Local reply(@NotNull PacketListener.Response response) {
            Builder.PrepareForSending prepareForSending = Packet.New()
                    .identification(Type.from("RC", "R"))
                    .parameter(MagicLinkCore.Packets.Response.Parameters.SUCCESSFUL, new Parameter(this.successful))
                    .parameter(MagicLinkCore.Packets.Response.Parameters.MESSAGE, response.message);

            response.parameters.forEach(prepareForSending::parameter);

            return prepareForSending.addressTo(this).send();
        }
    }

    interface Parameters {
        String PROTOCOL_VERSION = "v";
        String IDENTIFICATION = "i";
        String LOCAL = "s";
        String REMOTE = "t";
        String PARAMETERS = "p";
    }

    public static class Type {
        protected String id;

        public Type(String id) {
            this.id = id;
        }

        public String type() {
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
            Type mapping = (Type) o;
            return Objects.equals(this.id, mapping.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id);
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
         * @return {@link Type}
         * @throws IllegalArgumentException If illegal names are passed.
         */
        public static Type from(@NotNull String namespace, @NotNull String packetID) throws IllegalArgumentException {
            String idToCheck = namespace.toUpperCase();
            if(idToCheck.isEmpty()) throw new IllegalArgumentException("pluginID can't be empty!");
            if(packetID.isEmpty()) throw new IllegalArgumentException("packetID can't be empty!");

            return new Type(namespace + "-" + packetID);
        }

        /**
         * Create a new Identification from the passed string.
         * @param value Must be in the format `[namespace]-[packetID]`.
         * @return {@link Type}
         * @throws IllegalArgumentException If illegal names are passed.
         */
        public static Type parseString(@NotNull String value) throws IllegalArgumentException {
            String[] tokens = value.split("-");
            if(tokens.length > 2) throw new IllegalArgumentException("Invalid type passed.");
            return new Type(value.toUpperCase());
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

        public Object getOriginalValue() {
            return this.object;
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