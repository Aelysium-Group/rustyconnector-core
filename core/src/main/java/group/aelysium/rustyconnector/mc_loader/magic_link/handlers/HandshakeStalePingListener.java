package group.aelysium.rustyconnector.mc_loader.magic_link.handlers;

import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.mc_loader.ServerFlame;
import group.aelysium.rustyconnector.mc_loader.events.magic_link.TimeoutEvent;
import group.aelysium.rustyconnector.RC;

public class HandshakeStalePingListener extends PacketListener<MagicLinkCore.Packets.StalePing> {
    public HandshakeStalePingListener() {
        super(
                Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_STALE_PING,
                new Wrapper<>() {
                    @Override
                    public MagicLinkCore.Packets.StalePing wrap(Packet packet) {
                        return new MagicLinkCore.Packets.StalePing(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MagicLinkCore.Packets.StalePing packet) {
        ServerFlame flame = RC.S.Kernel();
        RC.S.EventManager().fireEvent(new TimeoutEvent());

        RC.S.MagicLink().setDelay(5);
        Packet.New()
                .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName() + ":" + flame.address().getPort())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.MAGIC_CONFIG_NAME, flame.MagicLink().orElseThrow().magicConfig())
                .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new PacketParameter(flame.playerCount()))
                .addressedTo(packet)
                .send();
    }
}
