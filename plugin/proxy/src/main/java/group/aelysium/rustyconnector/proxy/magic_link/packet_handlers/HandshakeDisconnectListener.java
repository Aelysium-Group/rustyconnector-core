package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.common.packets.MagicLink;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

import java.util.Optional;

public class HandshakeDisconnectListener extends PacketListener<MagicLink.Disconnect> {
    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT;
    }

    @Override
    public MagicLink.Disconnect wrap(Packet packet) {
        return new MagicLink.Disconnect(packet);
    }

    @Override
    public void execute(MagicLink.Disconnect packet) throws Exception {
        Optional<IMCLoader> mcLoader = RC.P.MCLoader(packet.sender().uuid());
        mcLoader.orElseThrow().unregister(true);
    }
}
