package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Server;

public class HandshakeDisconnectListener extends PacketListener<MagicLinkCore.Packets.Disconnect> {
    public Packet.Response handle(MagicLinkCore.Packets.Disconnect packet) {
        Server server = RC.P.Server(packet.local().id()).orElseThrow();

        RC.P.Kernel().unregisterServer(server);

        return Packet.Response.success("Successfully disconnected from the proxy.");
    }
}