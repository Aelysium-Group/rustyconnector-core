package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.common.packets.MagicLink;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.IPacket;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

import java.util.Optional;

public class HandshakeDisconnectListener extends PacketListener<MagicLink.Disconnect> {
    public HandshakeDisconnectListener() {
        super(
                BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT,
                new Wrapper<>() {
                    @Override
                    public MagicLink.Disconnect wrap(IPacket packet) {
                        return new MagicLink.Disconnect(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MagicLink.Disconnect packet) throws Exception {
        Optional<IMCLoader> mcLoader = RC.P.MCLoader(packet.sender().uuid());
        mcLoader.orElseThrow().unregister(true);
    }
}
