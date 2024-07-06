package group.aelysium.rustyconnector.proxy.family.mcloader.packet_handlers;

import group.aelysium.rustyconnector.toolkit.common.magic_link.buitin_packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.toolkit.common.magic_link.buitin_packets.MCLoader;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;

public class UnlockServerListener extends PacketListener<MCLoader.Unlock> {
    public UnlockServerListener() {
        super(
                BuiltInIdentifications.UNLOCK_SERVER,
                new Wrapper<>() {
                    @Override
                    public MCLoader.Unlock wrap(Packet packet) {
                        return new MCLoader.Unlock(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MCLoader.Unlock packet) throws Exception {
        RC.P.MCLoader(packet.sender().uuid()).orElseThrow().unlock();
    }
}
