package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Server;

public class HandshakeDisconnectListener {
    @PacketListener(MagicLinkCore.Packets.Disconnect.class)
    public static void execute(MagicLinkCore.Packets.Disconnect packet) throws Exception {
        Server server = RC.P.Server(packet.local().uuid()).orElseThrow();

        RC.P.Adapter().unregisterServer(server);
        try {
            server.family().orElseThrow().executeNow(f -> f.deleteServer(server));
        } catch (Exception ignore) {}

        try {
            Packet.New()
                    .identification(Packet.Identification.from("RC","MLHSP"))
                    .addressTo(packet)
                    .send();
        } catch (Exception ignore) {}

        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));
    }
}