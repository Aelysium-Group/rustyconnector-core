package group.aelysium.rustyconnector.common.magic_link;

import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PacketManager implements IPacketManagerCore {
    private final TimeoutCache<UUID, Packet> packetsAwaitingReply = new TimeoutCache<>(LiquidTimestamp.from(10, TimeUnit.SECONDS));

    public Packet.Builder newPacketBuilder() {
        return new Packet.Builder(this.flame);
    }

    public Map<UUID, Packet> activeReplyEndpoints() {
        return this.packetsAwaitingReply;
    }

    @Override
    public void kill() {
        try {
            this.packetsAwaitingReply.clear();
            this.packetsAwaitingReply.close();
        } catch (Exception ignore) {}
    }
}