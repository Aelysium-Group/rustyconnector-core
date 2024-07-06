package group.aelysium.rustyconnector.proxy.family.mcloader.packet_handlers;

import group.aelysium.rustyconnector.toolkit.common.magic_link.buitin_packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.toolkit.common.magic_link.buitin_packets.MCLoader;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;

public class LockServerListener extends PacketListener<MCLoader.Lock> {
    public LockServerListener() {
        super(
                BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING,
                new Wrapper<>() {
                    @Override
                    public MCLoader.Lock wrap(Packet packet) {
                        return new MCLoader.Lock(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MCLoader.Lock packet) throws Exception {
        RC.P.MCLoader(packet.sender().uuid()).orElseThrow().lock();
    }
}
