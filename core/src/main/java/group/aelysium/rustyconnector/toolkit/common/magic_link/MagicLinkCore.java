package group.aelysium.rustyconnector.toolkit.common.magic_link;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.cache.MessageCache;
import group.aelysium.rustyconnector.toolkit.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.toolkit.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketStatus;
import group.aelysium.rustyconnector.toolkit.common.message_cache.ICacheableMessage;
import group.aelysium.rustyconnector.toolkit.proxy.util.LiquidTimestamp;
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

    public void handleMessage(String rawMessage) {
        ICacheableMessage cachedMessage = null;
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
            cachedMessage.sentenceMessage(PacketStatus.TRASHED, "Message wasn't addressed to us.");
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
}
