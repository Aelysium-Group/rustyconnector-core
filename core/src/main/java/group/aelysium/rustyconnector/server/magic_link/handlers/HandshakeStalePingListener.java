package group.aelysium.rustyconnector.server.magic_link.handlers;

import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.server.events.TimeoutEvent;
import group.aelysium.rustyconnector.RC;

public class HandshakeStalePingListener {
    @PacketListener(MagicLinkCore.Packets.StalePing.class)
    public void execute(MagicLinkCore.Packets.StalePing packet) {
        ServerFlame flame = RC.S.Kernel();
        RC.S.EventManager().fireEvent(new TimeoutEvent());

        RC.S.MagicLink().setDelay(5);
        Packet.New()
                .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName() + ":" + flame.address().getPort())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.SERVER_REGISTRATION, flame.MagicLink().orElseThrow().magicConfig())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(flame.playerCount()))
                .addressedTo(packet)
                .send();
    }
}
