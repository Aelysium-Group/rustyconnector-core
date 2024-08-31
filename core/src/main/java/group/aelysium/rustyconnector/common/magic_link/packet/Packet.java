package group.aelysium.rustyconnector.common.magic_link.packet;

import com.google.gson.*;
import group.aelysium.rustyconnector.common.JSONParseable;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class Packet implements JSONParseable {
    private static final int protocolVersion = 2;

    private UUID uuid = UUID.randomUUID();
    private final Date date = new Date();
    private Status status = Status.UNDEFINED;
    private String reason = "";
    
    private final int messageVersion;
    private final Identification identification;
    private final Target sender;
    private final Target target;
    private final ResponseTarget responseTarget;
    private final Map<String, Parameter> parameters;
    private List<Consumer<Packet>> replyListeners = null; // Intentionally left null, if no responses are saved here, we don't want to bother instantiating a list here.

    /**
     * The protocol version used by this packet.
     */
    public int messageVersion() { return this.messageVersion; }

    /**
     * The node that sent this packet.
     */
    public Target sender() { return this.sender; }

    /**
     * The node that this packet is addressed to.
     */
    public Target target() { return this.target; }

    /**
     * The target that any responses should be made to.
     */
    public ResponseTarget responseTarget() { return this.responseTarget; }

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
    public List<Consumer<Packet>> replyListeners() {
        return this.replyListeners;
    }

    /**
     * Checks whether this packet is a response to a previous packet.
     * @return `true` if this packet is a response to another packet. `false` otherwise.
     */
    public boolean replying() {
        return this.responseTarget.remoteTarget().isPresent();
    }

    /**
     * Returns the packet which was sent as a reply to this one.
     */
    public void handleReply(Consumer<Packet> response) {
        if(this.replyListeners == null) this.replyListeners = new ArrayList<>();
        this.replyListeners.add(response);
    }
    
    protected Packet(@NotNull Integer version, @NotNull Identification identification, @NotNull Packet.Target sender, @NotNull Packet.Target target, @NotNull Packet.ResponseTarget responseTarget, @NotNull Map<String, Parameter> parameters) {
        this.messageVersion = version;
        this.identification = identification;
        this.sender = sender;
        this.target = target;
        this.responseTarget = responseTarget;
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
        object.add(Parameters.SENDER, this.sender.toJSON());
        object.add(Parameters.TARGET, this.target.toJSON());
        object.add(Parameters.RESPONSE, this.responseTarget.toJSON());

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
        private Target sender;
        private Target target;
        private ResponseTarget responseTarget = ResponseTarget.chainStart();
        private final Map<String, Parameter> parameters = new HashMap<>();

        public NakedBuilder identification(@NotNull Identification id) {
            this.id = id;
            return this;
        }

        public NakedBuilder sender(@NotNull Packet.Target sender) {
            this.sender = sender;
            return this;
        }

        public NakedBuilder target(@NotNull Packet.Target target) {
            this.target = target;
            return this;
        }

        public NakedBuilder response(@NotNull Packet.ResponseTarget responseTarget) {
            this.responseTarget = responseTarget;
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

        public Packet build() {
            return new Packet(this.protocolVersion, this.id, this.sender, this.target, this.responseTarget, this.parameters);
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

            private void assignTargetAndSender(@NotNull Packet.Target target, Target sender) {
                this.builder.target(target);

                if(sender != null) this.builder.sender(sender);

                try {
                    this.builder.sender(Target.server(RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().uuid()));
                    return;
                } catch (Exception ignore) {}
                try {
                    this.builder.sender(Target.server(RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().uuid()));
                    return;
                } catch (Exception ignore) {}
                throw new RuntimeException("No available flames existed in order to send the packet!");
            }
            private void assignTargetAndSender(@NotNull Packet.Target target) {
                assignTargetAndSender(target, null);
            }

            /**
             * Prepares the packet to the specified {@link Target}.
             * @throws RuntimeException If this packet was already sent or used in a reply, and then you try to send it again.
             */
            public ReadyForSending addressedTo(Target target) {
                assignTargetAndSender(target);

                return new ReadyForSending(this.builder);
            }

            /**
             * Prepares the packet as a reply to the specified {@link Packet}.
             * @throws RuntimeException If this packet was already sent or used in a reply, and then you try to send it again.
             */
            public ReadyForSending addressedTo(Packet targetPacket) {
                assignTargetAndSender(targetPacket.target(), targetPacket.sender());
                this.builder.response(ResponseTarget.respondTo(targetPacket.responseTarget().ownTarget()));

                return new ReadyForSending(this.builder);
            }
        }
        public static class ReadyForSending {
            private final NakedBuilder builder;

            protected ReadyForSending(NakedBuilder builder) {
                this.builder = builder;
            }

            private MagicLinkCore fetchMagicLink() {
                try {
                    return RC.P.MagicLink();
                } catch (Exception ignore) {}
                try {
                    return RC.S.MagicLink();
                } catch (Exception ignore) {}
                throw new RuntimeException("No available flames existed in order to send the packet!");
            }

            /**
             * Sends the packet.
             * This method resolves the currently active MagicLink provider and calls {@link MagicLinkCore#publish(Packet)}.
             * @throws RuntimeException If there was an issue sending the packet.
             */
            public void send() throws RuntimeException {
                Packet packet = this.builder.build();

                fetchMagicLink().publish(packet);
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

    public static Packet parseReceived(String rawMessage) {
        Gson gson = new Gson();
        JsonObject messageObject = gson.fromJson(rawMessage, JsonObject.class);

        NakedBuilder builder = new NakedBuilder();

        messageObject.entrySet().forEach(entry -> {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            switch (key) {
                case Parameters.PROTOCOL_VERSION -> builder.protocolVersion(value.getAsInt());
                case Parameters.IDENTIFICATION -> builder.identification(new Identification(value.getAsString()));
                case Parameters.SENDER -> builder.sender(Target.fromJSON(value.getAsJsonObject()));
                case Parameters.TARGET -> builder.target(Target.fromJSON(value.getAsJsonObject()));
                case Parameters.RESPONSE -> builder.response(ResponseTarget.fromJSON(value.getAsJsonObject()));
                case Parameters.PARAMETERS -> {
                    value.getAsJsonObject().entrySet().forEach(entry2 -> {
                        String key2 = entry.getKey();
                        Parameter value2 = new Parameter(entry2.getValue().getAsJsonPrimitive());

                        builder.parameter(key2, value2);
                    });
                }
            }
        });

        return builder.build();
    }

    public static class Target implements JSONParseable {
        private final UUID uuid;
        private final Origin origin;

        private Target(UUID uuid, @NotNull Origin origin) {
            this.uuid = uuid;
            this.origin = origin;
        }

        public UUID uuid() {
            return this.uuid;
        }
        public Origin origin() {
            return this.origin;
        }

        public JsonObject toJSON() {
            JsonObject object = new JsonObject();

            object.add("u", new JsonPrimitive(this.uuid.toString()));
            object.add("n", new JsonPrimitive(Origin.toInteger(this.origin)));

            return object;
        }

        public static Target fromJSON(JsonObject object) {
            return new Target(
                    UUID.fromString(object.get("u").getAsString()),
                    Origin.fromInteger(object.get("n").getAsInt())
            );
        }

        public static Target server(UUID uuid) {
            return new Target(uuid, Origin.SERVER);
        }
        public static Target proxy(UUID uuid) {
            return new Target(uuid, Origin.PROXY);
        }
        public static Target allAvailableProxies() {
            return new Target(null, Origin.ANY_PROXY);
        }

        /**
         * Checks if the passed node can be considered the same as `this`.
         * For example, if `this` is of type {@link Origin#PROXY} and `node` is of type {@link Origin#ANY_PROXY} this will return `true`
         * because `this` would be considered a part of `ANY_PROXY`.
         * @param target Some other node.
         * @return `true` if the other node is a valid way of identifying `this` node. `false` otherwise.
         */
        public boolean isNodeEquivalentToMe(Target target) {
            // If the two match as defined by default expected behaviour, return true.
            if(Objects.equals(uuid, target.uuid) && origin == target.origin) return true;

            // If one of the two is of type "ANY_PROXY" and the other is of type "PROXY", return true.
            if(
                    (this.origin == Origin.ANY_PROXY && target.origin == Origin.PROXY) ||
                    (this.origin == Origin.PROXY && target.origin == Origin.ANY_PROXY) ||
                    (this.origin == Origin.ANY_PROXY && target.origin == Origin.ANY_PROXY)
            ) return true;

            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Target target = (Target) o;

            // If the two match as defined by default expected behaviour, return true.
            return Objects.equals(uuid, target.uuid) && origin == target.origin;
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

    public static class ResponseTarget implements JSONParseable {
        private final UUID ownTarget;
        private final UUID remoteTarget;

        private ResponseTarget() {
            this(null);
        }
        private ResponseTarget(UUID remoteTarget) {
            this(UUID.randomUUID(), remoteTarget);
        }
        protected ResponseTarget(@NotNull UUID ownTarget, UUID remoteTarget) {
            this.ownTarget = ownTarget;
            this.remoteTarget = remoteTarget;
        }

        public UUID ownTarget() {
            return this.ownTarget;
        }
        public Optional<UUID> remoteTarget() {
            if(this.remoteTarget == null) return Optional.empty();
            return Optional.of(this.remoteTarget);
        }

        public static ResponseTarget chainStart() {
            return new ResponseTarget();
        }
        public static ResponseTarget respondTo(UUID remoteTarget) {
            return new ResponseTarget(remoteTarget);
        }
        public static ResponseTarget fromJSON(@NotNull JsonObject object) {
            return new ResponseTarget(
                    UUID.fromString(object.get("o").getAsString()),
                    UUID.fromString(object.get("r").getAsString())
            );
        }

        public JsonObject toJSON() {
            JsonObject object = new JsonObject();

            String remoteTarget = "";
            if(this.remoteTarget != null) remoteTarget = this.remoteTarget.toString();

            object.add("o", new JsonPrimitive(this.ownTarget.toString()));
            object.add("r", new JsonPrimitive(remoteTarget));

            return object;
        }
    }

    public static class Wrapper extends Packet {
        public Wrapper(Packet packet) {
            super(
                    packet.messageVersion(),
                    packet.identification(),
                    packet.sender(),
                    packet.target(),
                    packet.responseTarget(),
                    packet.parameters()
            );
        }
    }

    interface Parameters {
        String PROTOCOL_VERSION = "v";
        String IDENTIFICATION = "i";
        String SENDER = "s";
        String TARGET = "t";
        String RESPONSE = "r";
        String PARAMETERS = "p";
    }


    public interface BuiltInIdentifications {
        /**
         * `Server > Proxy` | Server requesting to interface with Proxy.
         *                    | If the Server is new, it will attempt to be registered.
         *                    | If the Server is already registered, it's connection will refresh.
         *
         *                    | This packet is simultaneously a handshake initializer and a keep-alive packet.
         */
        Identification MAGICLINK_HANDSHAKE_PING = Identification.from("RC","MLH");

        /**
         * `Proxy > Server` | Tells the Server it couldn't be registered
         */
        Identification MAGICLINK_HANDSHAKE_FAIL = Identification.from("RC","MLHF");

        /**
         * `Proxy > Server` | Tells the Server it was registered and how it should configure itself
         */
        Identification MAGICLINK_HANDSHAKE_SUCCESS = Identification.from("RC","MLHS");

        /**
         * `Server > Proxy` | Tells the Proxy to drop the Magic Link between this Server.
         *                    | Typically used when the Server is shutting down so that Magic Link doesn't have to manually scan it.
         */
        Identification MAGICLINK_HANDSHAKE_DISCONNECT = Identification.from("RC","MLHK");

        /**
         * `Proxy > Server` | Informs the Server that it's connection to the proxy has gone stale.
         *                    | It is expected that, if the Server is still available it will respond to this message with a {@link BuiltInIdentifications#MAGICLINK_HANDSHAKE_PING}
         */
        Identification MAGICLINK_HANDSHAKE_STALE_PING = Identification.from("RC","MLHSP");

        /**
         * `Server > Proxy` | Request to send a player to a family
         */
        Identification SEND_PLAYER = Identification.from("RC","SP");

        /**
         * `Server > Server` | Tells the proxy to open a server.
         */
        Identification UNLOCK_SERVER = Identification.from("RC","US");

        /**
         * `Server > Proxy` | Tells the proxy to close a server.
         */
        Identification LOCK_SERVER = Identification.from("RC","LS");

        static List<Identification> toList() {
            List<Identification> list = new ArrayList<>();

            list.add(MAGICLINK_HANDSHAKE_PING);
            list.add(MAGICLINK_HANDSHAKE_FAIL);
            list.add(MAGICLINK_HANDSHAKE_DISCONNECT);
            list.add(MAGICLINK_HANDSHAKE_SUCCESS);
            list.add(SEND_PLAYER);
            list.add(UNLOCK_SERVER);
            list.add(LOCK_SERVER);

            return list;
        }

        static Identification mapping(String id) {
            return toList().stream().filter(entry -> Objects.equals(entry.get(), id)).findFirst().orElseThrow(NullPointerException::new);
        }
    }

    public enum Status {
        UNDEFINED, // The message hasn't had any status set yet.
        AUTH_DENIAL, // If the message didn't contain the proper credentials (IP Address (for message tunnel), private-key, over max length, etc)
        PARSING_ERROR, // If the message failed to be parsed
        TRASHED, // If the message isn't intended for us.
        ACCEPTED, // Just cause a message was accepted doesn't mean it was processed. It could still cause an error
        EXECUTING_ERROR, // If the message failed to be parsed
        EXECUTED; // The message has successfully processed and handled.

        public NamedTextColor color() {
            if(this == AUTH_DENIAL) return NamedTextColor.RED;
            if(this == TRASHED) return NamedTextColor.DARK_GRAY;
            if(this == PARSING_ERROR) return NamedTextColor.DARK_RED;
            if(this == ACCEPTED) return NamedTextColor.YELLOW;
            if(this == EXECUTED) return NamedTextColor.GREEN;
            if(this == EXECUTING_ERROR) return NamedTextColor.DARK_RED;
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
         * Create a new Packet Mapping from a pluginID and a packetID.
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
        public Parameter(JsonPrimitive object) {
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