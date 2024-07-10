package group.aelysium.rustyconnector.common.magic_link.packet;

import com.google.gson.*;
import group.aelysium.rustyconnector.common.JSONParseable;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class Packet implements JSONParseable {
    private static final int protocolVersion = 2;

    private final int messageVersion;
    private final PacketIdentification identification;
    private final Target sender;
    private final Target target;
    private final ResponseTarget responseTarget;
    private final Map<String, PacketParameter> parameters;
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
    public PacketIdentification identification() { return this.identification; }

    /**
     * The extra parameters that this packet caries.
     */
    public Map<String, PacketParameter> parameters() { return parameters; }

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

    protected Packet(@NotNull Integer version, @NotNull PacketIdentification identification, @NotNull Packet.Target sender, @NotNull Packet.Target target, @NotNull Packet.ResponseTarget responseTarget, @NotNull Map<String, PacketParameter> parameters) {
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
        private PacketIdentification id;
        private Target sender;
        private Target target;
        private ResponseTarget responseTarget = ResponseTarget.chainStart();
        private final Map<String, PacketParameter> parameters = new HashMap<>();

        public NakedBuilder identification(@NotNull PacketIdentification id) {
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
            this.parameters.put(key, new PacketParameter(value));
            return this;
        }
        public NakedBuilder parameter(@NotNull String key, @NotNull PacketParameter value) {
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
                this.builder.parameter(key, new PacketParameter(value));
                return this;
            }

            public PrepareForSending parameter(String key, PacketParameter value) {
                this.builder.parameter(key, value);
                return this;
            }

            private void assignTargetAndSender(@NotNull Packet.Target target, Target sender) {
                this.builder.target(target);

                if(sender != null) this.builder.sender(sender);

                try {
                    this.builder.sender(Target.mcLoader(RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().uuid()));
                    return;
                } catch (Exception ignore) {}
                try {
                    this.builder.sender(Target.mcLoader(RustyConnector.Toolkit.MCLoader().orElseThrow().orElseThrow().uuid()));
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
                    return RC.M.MagicLink();
                } catch (Exception ignore) {}
                throw new RuntimeException("No available flames existed in order to send the packet!");
            }

            /**
             * Sends the packet.
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
        public PrepareForSending identification(PacketIdentification id) {
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
                case Parameters.IDENTIFICATION -> builder.identification(new PacketIdentification(value.getAsString()));
                case Parameters.SENDER -> builder.sender(Target.fromJSON(value.getAsJsonObject()));
                case Parameters.TARGET -> builder.target(Target.fromJSON(value.getAsJsonObject()));
                case Parameters.RESPONSE -> builder.response(ResponseTarget.fromJSON(value.getAsJsonObject()));
                case Parameters.PARAMETERS -> {
                    value.getAsJsonObject().entrySet().forEach(entry2 -> {
                        String key2 = entry.getKey();
                        PacketParameter value2 = new PacketParameter(entry2.getValue().getAsJsonPrimitive());

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

        public static Target mcLoader(UUID uuid) {
            return new Target(uuid, Origin.MCLOADER);
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
            MCLOADER,
            ANY_MCLOADER
            ;

            public static Origin fromInteger(int number) {
                return switch (number) {
                    case 0 -> Origin.PROXY;
                    case 1 -> Origin.ANY_PROXY;
                    case 2 -> Origin.MCLOADER;
                    case 3 -> Origin.ANY_MCLOADER;
                    default -> throw new ClassCastException(number+" has no associated value!");
                };
            }
            public static int toInteger(Origin origin) {
                return switch (origin) {
                    case PROXY -> 0;
                    case ANY_PROXY -> 1;
                    case MCLOADER -> 2;
                    case ANY_MCLOADER -> 3;
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
}