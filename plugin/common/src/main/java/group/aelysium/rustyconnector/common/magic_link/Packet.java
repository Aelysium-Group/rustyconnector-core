package group.aelysium.rustyconnector.common.magic_link;

import com.google.gson.*;
import group.aelysium.rustyconnector.toolkit.common.JSONParseable;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderFlame;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public final class Packet implements JSONParseable {
    private static final int protocolVersion = 2;

    public static int protocolVersion() {
        return protocolVersion;
    }

    private final int messageVersion;
    private final PacketIdentification identification;
    private final Target sender;
    private final Target target;
    private final ResponseTarget responseTarget;
    private final Map<String, PacketParameter> parameters;
    private List<Consumer<Packet>> responseCallbacks = null; // Intentionally left null, if no responses are saved here, we don't want to bother instantiating a list here.

    public int messageVersion() { return this.messageVersion; }
    public Target sender() { return this.sender; }
    public Target target() { return this.target; }
    public ResponseTarget responseTarget() { return this.responseTarget; }
    public PacketIdentification identification() { return this.identification; }
    public Map<String, PacketParameter> parameters() { return parameters; }

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
    public void response(Consumer<Packet> consumer) {
        if(this.responseCallbacks == null) this.responseCallbacks = new ArrayList<>();
        this.responseCallbacks.add(consumer);
    }

    /**
     * Sends a packet as a response to this one.
     * If the packet is found to not have a {@link ResponseTarget#remoteTarget()} that matches this packet's {@link ResponseTarget#ownTarget()} the request will be ignored.
     * @param response The packet to be sent as a response to this one.
     */
    public void issueResponse(Packet response) {
        if(this.responseCallbacks == null) return;
        if(!this.responseTarget.ownTarget.equals(response.responseTarget.remoteTarget)) return;

        this.responseCallbacks.forEach(c -> c.accept(response));
    }

    public Packet(@NotNull Integer version, @NotNull PacketIdentification identification, @NotNull Packet.Target sender, @NotNull Packet.Target target, @NotNull Packet.ResponseTarget responseTarget, @NotNull Map<String, PacketParameter> parameters) {
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

    protected static class NakedBuilder {
        private Integer protocolVersion = Packet.protocolVersion();
        private PacketIdentification id;
        private Target sender;
        private Target target;
        private ResponseTarget responseTarget = ResponseTarget.chainStart();
        private final Map<String, PacketParameter> parameters = new HashMap<>();
        private final Consumer<Packet> publish;

        public NakedBuilder(Consumer<Packet> publish) {
            this.publish = publish;
        }

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
        private final NakedBuilder builder;

        public Builder(Consumer<Packet> publish) {
            this.builder = new NakedBuilder(publish);
        }

        public static class ReadyForSending {
            private final NakedBuilder builder;

            protected ReadyForSending(NakedBuilder builder) {
                this.builder = builder;
            }

            public ReadyForSending parameter(String key, String value) {
                this.builder.parameter(key, new PacketParameter(value));
                return this;
            }

            public ReadyForSending parameter(String key, PacketParameter value) {
                this.builder.parameter(key, value);
                return this;
            }

            private ICoreMagicLinkService fetchMagicLink() {
                if(this.flame instanceof VelocityFlame<?> velocityFlame)
                    return velocityFlame.services().magicLink();
                if(this.flame instanceof IMCLoaderFlame<?> mcloaderFlame)
                    return mcloaderFlame.services().magicLink();
                throw new RuntimeException("No available flames existed in order to send the packet!");
            }

            private void assignTargetAndSender(@NotNull Packet.Target target, Target sender) {
                this.builder.target(target);

                if(sender != null) this.builder.sender(sender);

                if(this.flame instanceof VelocityFlame<?> velocityFlame)
                    this.builder.sender(Target.mcLoader(velocityFlame.uuid()));
                if(this.flame instanceof IMCLoaderFlame<?> mcloaderFlame)
                    this.builder.sender(Target.mcLoader(mcloaderFlame.services().serverInfo().uuid()));
            }
            private void assignTargetAndSender(@NotNull Packet.Target target) {
                assignTargetAndSender(target, null);
            }

            /**
             * Sends the packet to the specified {@link Target}.
             */
            public Packet sendTo(Target target) {
                assignTargetAndSender(target);

                Packet packet = this.builder.build();

                ICoreMagicLinkService magicLinkService = fetchMagicLink();

                magicLinkService.connection().orElseThrow().publish(packet);
                magicLinkService.packetManager().activeReplyEndpoints().put(packet.responseTarget().ownTarget(), packet);

                return packet;
            }

            /**
             * Replies to the passed packet.
             * Only one reply can be sent for a packet.
             * If this packet has already been replied to, nothing will happen when a duplicate is sent.
             * @param targetPacket The packet to reply to.
             */
            public void replyTo(Packet targetPacket) {
                assignTargetAndSender(targetPacket.target(), targetPacket.sender());
                this.builder.response(ResponseTarget.respondTo(targetPacket.responseTarget().ownTarget()));

                Packet packet = this.builder.build();

                ICoreMagicLinkService magicLinkService = fetchMagicLink();
                magicLinkService.connection().orElseThrow().publish(packet);
                magicLinkService.packetManager().activeReplyEndpoints().put(packet.responseTarget().ownTarget(), packet);
            }
        }

        /**
         * The identification of this packet.
         * Identification is what differentiates a "Server ping packet" from a "Teleport player packet"
         */
        public ReadyForSending identification(PacketIdentification id) {
            return new ReadyForSending(this.flame, builder.identification(id));
        }
    }

    public static class Serializer {
        /**
         * Parses a raw string into a received RedisMessage.
         * @param rawMessage The raw message to parse.
         * @return A received RedisMessage.
         */
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
                    case Parameters.PARAMETERS -> parseParams(value.getAsJsonObject(), builder);
                }
            });

            return builder.build();
        }

        private static void parseParams(JsonObject object, NakedBuilder builder) {
            object.entrySet().forEach(entry -> {
                String key = entry.getKey();
                PacketParameter value = new PacketParameter(entry.getValue().getAsJsonPrimitive());

                builder.parameter(key, value);
            });
        }

    }

    public interface Parameters {
        String PROTOCOL_VERSION = "v";
        String IDENTIFICATION = "i";
        String SENDER = "s";
        String TARGET = "t";
        String RESPONSE = "r";
        String PARAMETERS = "p";
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

    public static class Wrapper {
        private final Packet packet;

        public int messageVersion() { return this.packet.messageVersion(); }
        public Target sender() { return this.packet.sender(); }
        public Target target() { return this.packet.target(); }
        public PacketIdentification identification() { return this.packet.identification(); }
        public ResponseTarget responseTarget() { return this.packet.responseTarget(); }
        public Map<String, PacketParameter> parameters() { return this.packet.parameters(); }
        public PacketParameter parameter(String key) { return this.packet.parameters().get(key); }
        public Packet packet() {
            return this.packet;
        }
        public void response(Consumer<Packet> consumer) {
            this.packet.response(consumer);
        }

        protected Wrapper(Packet packet) {
            this.packet = packet;
        }
    }
}