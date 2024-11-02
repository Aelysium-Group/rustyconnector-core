package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.exceptions.PacketStatusResponse;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Server;

public class HandshakeDisconnectListener {
    @PacketListener(MagicLinkCore.Packets.Disconnect.class)
    public static void execute(MagicLinkCore.Packets.Disconnect packet) throws PacketStatusResponse {
        try {
            Server server = RC.P.Server(packet.local().uuid()).orElseThrow();

            RC.P.Kernel().unregisterServer(server);

            Packet.New()
                    .identification(Packet.Identification.from("RC","MLHSP"))
                    .addressTo(packet)
                    .send();
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
    }
}