package group.aelysium.rustyconnector.proxy.family.mcloader.packet_handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.packet.PacketListener;

public class LockServerListener extends PacketListener<group.aelysium.rustyconnector.common.packets.MCLoader.Lock> {
    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.LOCK_SERVER;
    }

    @Override
    public group.aelysium.rustyconnector.common.packets.MCLoader.Lock wrap(Packet packet) {
        return new group.aelysium.rustyconnector.common.packets.MCLoader.Lock(packet);
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.packets.MCLoader.Lock packet) throws Exception {
        RC.P.MCLoader(packet.sender().uuid()).lock();
    }
}
