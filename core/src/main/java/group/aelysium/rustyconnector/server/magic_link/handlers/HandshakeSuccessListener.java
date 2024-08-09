package group.aelysium.rustyconnector.server.magic_link.handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.server.events.ConnectedEvent;
import net.kyori.adventure.text.Component;

public class HandshakeSuccessListener extends PacketListener<MagicLinkCore.Packets.Handshake.Success> {
    public HandshakeSuccessListener() {
        super(
                Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING,
                new Wrapper<>() {
                    @Override
                    public MagicLinkCore.Packets.Handshake.Success wrap(Packet packet) {
                        return new MagicLinkCore.Packets.Handshake.Success(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MagicLinkCore.Packets.Handshake.Success packet) {
        RC.S.EventManager().fireEvent(new ConnectedEvent());

        RC.S.Adapter().log(Component.text(packet.message(), packet.color()));

        RC.S.MagicLink().setDelay(packet.pingInterval());
    }
}
