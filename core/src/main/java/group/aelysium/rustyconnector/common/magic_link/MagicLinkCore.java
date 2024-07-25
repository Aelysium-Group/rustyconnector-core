package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.CacheableMessage;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class MagicLinkCore implements Particle {
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

    public final void publish(Packet packet) {
        packetsAwaitingReply.put(packet.responseTarget().ownTarget(), packet);
    }

    public void on(PacketListener<? extends Packet> listener) {
        this.listeners.putIfAbsent(listener.target(), new ArrayList<>());
        this.listeners.get(listener.target()).add(listener);
    }

    public void close() throws Exception {
        this.listeners.clear();
        this.cache.kill();
        this.packetsAwaitingReply.close();
    }

    /**
     * Handles all of the MagicLink/RustyConnector internals of handling MagicLink packets.
     * @param rawMessage A AES-256 encrypted MagicLink packet.
     */
    public void handleMessage(String rawMessage) {
        CacheableMessage cachedMessage = null;
        String decryptedMessage;
        try {
            decryptedMessage = this.cryptor.decrypt(rawMessage);
            cachedMessage = this.cache.cacheMessage(decryptedMessage, PacketStatus.UNDEFINED);
        } catch (Exception e) {
            cachedMessage = this.cache.cacheMessage(rawMessage, PacketStatus.UNDEFINED);
            cachedMessage.sentenceMessage(PacketStatus.AUTH_DENIAL, "This message was encrypted using a different private key from what I have!");
            return;
        }

        Packet message = Packet.parseReceived(decryptedMessage);

        if(this.cache.ignoredType(message)) this.cache.removeMessage(cachedMessage.getSnowflake());

        if(!this.self.isNodeEquivalentToMe(message.target())) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "This packet wasn't addressed to me.");
            return;
        }

        if(message.replying()) {
            Packet reply = this.packetsAwaitingReply.get(message.responseTarget().remoteTarget().orElse(null));

            if(reply == null) {
                cachedMessage.sentenceMessage(PacketStatus.TRASHED, "The packet that this is replying to doesn't exist.");
                return;
            }

            ((Packet) reply).replyListeners().forEach(l -> l.accept(message));
            return;
        }

        List<PacketListener<? extends Packet>> listeners = this.listeners.get(message.identification());
        if(listeners == null) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "No listeners exist to handle this packet.");
            return;
        }
        if(listeners.isEmpty()) {
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "No listeners exist to handle this packet.");
            return;
        }

        listeners.forEach(listener -> {
            try {
                listener.wrapAndExecute(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
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
}
