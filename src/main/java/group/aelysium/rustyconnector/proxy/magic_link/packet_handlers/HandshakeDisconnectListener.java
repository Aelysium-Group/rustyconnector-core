package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Server;

public class HandshakeDisconnectListener {
    @PacketListener(MagicLinkCore.Packets.Disconnect.class)
    public PacketListener.Response handle(MagicLinkCore.Packets.Disconnect packet) {
        Server server = RC.P.Server(packet.local().id()).orElseThrow();

        RC.P.Kernel().unregisterServer(server);

        return PacketListener.Response.success("Successfully disconnected from the proxy.");
    }
}