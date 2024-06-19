package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.toolkit.common.cache.MessageCache;
import group.aelysium.rustyconnector.toolkit.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.common.exception.BlockedMessageException;
import group.aelysium.rustyconnector.common.exception.NoOutputException;
import group.aelysium.rustyconnector.toolkit.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.toolkit.common.log_gate.GateKey;
import group.aelysium.rustyconnector.toolkit.common.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.IPacket;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketStatus;
import group.aelysium.rustyconnector.toolkit.common.message_cache.ICacheableMessage;
import group.aelysium.rustyconnector.toolkit.proxy.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MagicLinkCore implements IMagicLink {
    protected final AESCryptor cryptor;
    protected final MessageCache cache;
    protected final IMagicLink.MessageHandler messageHandler;

    protected MagicLinkCore(@NotNull AESCryptor cryptor, @NotNull MessageCache cache, @NotNull IMagicLink.MessageHandler messageHandler) {
        this.cryptor = cryptor;
        this.cache = cache;
        this.messageHandler = messageHandler;
    }

    @Override
    public Flux<IMagicLink.Connection> connection() {
        return null;
    }

    @Override
    public void close() throws Exception {
    }

    public static class Connection implements IMagicLink.Connection {
        private final TimeoutCache<UUID, IPacket> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(10, TimeUnit.SECONDS));

        public final void publish(IPacket packet) {
            packetsAwaitingReply.put(packet.responseTarget().ownTarget(), packet);
        }

        @Override
        public void on(PacketListener<IPacket> listener) {

        }

        @Override
        public void off(PacketListener<IPacket> listener) {

        }

        @Override
        public void close() throws Exception {
            this.packetsAwaitingReply.close();
        }
    }

    public static final Consumer<String> DEFAULT_MESSAGE_HANDLER = (String rawMessage) -> {
        // If the proxy doesn't have a message cache (maybe it's in the middle of a reload)
        // Set a temporary, worthless, message cache so that the system can still "cache" messages into the worthless cache if needed.
        if(messageCache == null) {
            this.messageCache = new MessageCache(1);
        }

        ICacheableMessage cachedMessage = null;
        try {
            String decryptedMessage;
            try {
                decryptedMessage = this.cryptor().decrypt(rawMessage);
                cachedMessage = messageCache.cacheMessage(decryptedMessage, PacketStatus.UNDEFINED);
            } catch (Exception e) {
                cachedMessage = messageCache.cacheMessage(rawMessage, PacketStatus.UNDEFINED);
                cachedMessage.sentenceMessage(PacketStatus.AUTH_DENIAL, "This message was encrypted using a different private key from what I have!");
                return;
            }

            Packet message = Packet.Serializer.parseReceived(decryptedMessage);

            if (messageCache.ignoredType(message)) messageCache.removeMessage(cachedMessage.getSnowflake());

            if(!self.isNodeEquivalentToMe(message.target())) throw new Exception("Message was not addressed to us.");

            try {
                cachedMessage.sentenceMessage(PacketStatus.ACCEPTED);


                if(message.replying()) {
                    ICoreMagicLinkService magicLink = null;
                    try {
                        magicLink = ((VelocityFlame<?>) RustyConnector.Toolkit.proxy().orElseThrow()).services().magicLink();
                    } catch (Exception ignore) {}
                    try {
                        magicLink = ((MCLoaderFlame) RustyConnector.Toolkit.mcLoader().orElseThrow()).services().magicLink();
                    } catch (Exception ignore) {}
                    if(magicLink == null) throw new Exception("There was no flame available to handle the message!");

                    Packet reply = magicLink.packetManager().activeReplyEndpoints().get(message.responseTarget().remoteTarget().orElseThrow(() ->
                            new NoSuchElementException("There was no available packet to reply to.")
                    ));

                    reply.issueResponse(message);
                    return;
                }

                List<PacketListener<? extends Packet.Wrapper>> listeners = this.listeners.get(message.identification());
                if(listeners == null) throw new NullPointerException("No packet handler with the type "+message.identification()+" exists!");
                if(listeners.isEmpty()) throw new NullPointerException("No packet handler with the type "+message.identification()+" exists!");

                listeners.forEach(listener -> {
                    try {
                        listener.wrapAndExecute(message);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (BlockedMessageException e) {
                cachedMessage.sentenceMessage(PacketStatus.AUTH_DENIAL, e.getMessage());

                if(!logger.loggerGate().check(GateKey.MESSAGE_TUNNEL_FAILED_MESSAGE)) return;

                logger.error("An incoming message from: "+message.sender().toString()+" was blocked by the message tunnel!");
                logger.log("To view the thrown away message use: /rc message get "+cachedMessage.getSnowflake());
            } catch (NoOutputException e) {
                cachedMessage.sentenceMessage(PacketStatus.AUTH_DENIAL, e.getMessage());
            }
        } catch (Exception e) {
            if(cachedMessage == null) cachedMessage = messageCache.cacheMessage(rawMessage, PacketStatus.UNDEFINED);

            if(logger.loggerGate().check(GateKey.SAVE_TRASH_MESSAGES))
                cachedMessage.sentenceMessage(PacketStatus.TRASHED, e.getMessage());
            else
                messageCache.removeMessage(cachedMessage.getSnowflake());

            if(!logger.loggerGate().check(GateKey.MESSAGE_PARSER_TRASH)) return;

            logger.error("An incoming message was thrown away!");
            logger.log("To view the thrown away message use: /rc message get "+cachedMessage.getSnowflake());
        }
    }
}
