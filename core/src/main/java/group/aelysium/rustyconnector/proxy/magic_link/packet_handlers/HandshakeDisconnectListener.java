package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Server;

public class HandshakeDisconnectListener extends PacketListener<MagicLinkCore.Packets.Disconnect> {
    public HandshakeDisconnectListener() {
        super(
                Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT,
                new Wrapper<>() {
                    @Override
                    public MagicLinkCore.Packets.Disconnect wrap(Packet packet) {
                        return new MagicLinkCore.Packets.Disconnect(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MagicLinkCore.Packets.Disconnect packet) throws Exception {
        Server server = RC.P.Server(packet.sender().uuid()).orElseThrow();

        RC.P.Adapter().unregisterServer(server);
        server.family().executeNow(f -> f.deleteServer(server));

        try {
            Packet.New()
                    .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_STALE_PING)
                    .addressedTo(packet)
                    .send();
        } catch (Exception ignore) {}

        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));
    }
}