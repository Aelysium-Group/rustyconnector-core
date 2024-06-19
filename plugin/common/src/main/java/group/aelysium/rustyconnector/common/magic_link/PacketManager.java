package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.toolkit.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.toolkit.proxy.util.LiquidTimestamp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PacketManager implements AutoCloseable {
    private final TimeoutCache<UUID, Packet> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(10, TimeUnit.SECONDS));

    public Packet.Builder newPacketBuilder() {
        return new Packet.Builder();
    }

    public Map<UUID, Packet> activeReplyEndpoints() {
        return this.packetsAwaitingReply;
    }

    @Override
    public void close() throws Exception {
        try {
            this.packetsAwaitingReply.clear();
            this.packetsAwaitingReply.close();
        } catch (Exception ignore) {}
    }
}