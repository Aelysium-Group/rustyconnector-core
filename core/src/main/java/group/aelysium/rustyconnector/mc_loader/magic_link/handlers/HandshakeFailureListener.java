package group.aelysium.rustyconnector.mc_loader.magic_link.handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;

import java.util.concurrent.TimeUnit;

public class HandshakeFailureListener extends PacketListener<MagicLinkCore.Packets.Handshake.Failure> {
    public HandshakeFailureListener() {
        super(
                Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_FAIL,
                new Wrapper<>() {
                    @Override
                    public MagicLinkCore.Packets.Handshake.Failure wrap(Packet packet) {
                        return new MagicLinkCore.Packets.Handshake.Failure(packet);
                    }
                }
        );
    }

    @Override
    public void execute(MagicLinkCore.Packets.Handshake.Failure packet) {
        try {
            RC.S.Adapter().log(RC.S.Lang().lang().magicLinkHandshakeFailure(packet.reason(), 1, TimeUnit.MINUTES));
        } catch (Exception ignore) {}
        RC.S.MagicLink().setDelay(60);
    }
}
