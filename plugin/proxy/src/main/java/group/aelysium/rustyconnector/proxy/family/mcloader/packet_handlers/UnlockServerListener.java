package group.aelysium.rustyconnector.proxy.family.mcloader.packet_handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;

public class UnlockServerListener extends PacketListener<group.aelysium.rustyconnector.common.packets.MCLoader.Unlock> {
    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.UNLOCK_SERVER;
    }

    @Override
    public group.aelysium.rustyconnector.common.packets.MCLoader.Unlock wrap(Packet packet) {
        return new group.aelysium.rustyconnector.common.packets.MCLoader.Unlock(packet);
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.packets.MCLoader.Unlock packet) throws Exception {
        RC.P.MCLoader(packet.sender().uuid()).unlock();
    }
}
