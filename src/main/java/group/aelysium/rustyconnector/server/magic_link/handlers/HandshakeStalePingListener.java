package group.aelysium.rustyconnector.server.magic_link.handlers;

import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.exceptions.PacketStatusResponse;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.events.TimeoutEvent;
import group.aelysium.rustyconnector.RC;

public class HandshakeStalePingListener {
    @PacketListener(MagicLinkCore.Packets.StalePing.class)
    public static void execute(MagicLinkCore.Packets.StalePing packet) {
        ServerKernel flame = RC.S.Kernel();
        RC.S.EventManager().fireEvent(new TimeoutEvent());

        RC.S.MagicLink().setDelay(5);
        Packet.New()
                .identification(Packet.Identification.from("RC","MLH"))
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName() + ":" + flame.address().getPort())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.SERVER_REGISTRATION, flame.MagicLink().orElseThrow().registration())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(flame.playerCount()))
                .addressTo(packet)
                .send();
    }
}
