package group.aelysium.rustyconnector.server.magic_link.handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;

import java.util.concurrent.TimeUnit;

public class HandshakeFailureListener {
    @PacketListener(MagicLinkCore.Packets.Handshake.Failure.class)
    public static void execute(MagicLinkCore.Packets.Handshake.Failure packet) {
        try {
            RC.S.Adapter().log(RC.S.Lang().lang().magicLinkHandshakeFailure(packet.reason(), 1, TimeUnit.MINUTES));
        } catch (Exception ignore) {}
        RC.S.MagicLink().setDelay(60);
    }
}
