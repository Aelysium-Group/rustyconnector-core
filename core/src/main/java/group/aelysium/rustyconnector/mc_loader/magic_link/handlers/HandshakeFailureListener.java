package group.aelysium.rustyconnector.mc_loader.magic_link.handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.buitin_packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HandshakeFailureListener extends PacketListener<group.aelysium.rustyconnector.common.buitin_packets.MagicLink.Handshake.Failure> {
    public HandshakeFailureListener() {
        super(
                BuiltInIdentifications.MAGICLINK_HANDSHAKE_FAIL,
                new Wrapper<>() {
                    @Override
                    public group.aelysium.rustyconnector.common.buitin_packets.MagicLink.Handshake.Failure wrap(Packet packet) {
                        return new group.aelysium.rustyconnector.common.buitin_packets.MagicLink.Handshake.Failure(packet);
                    }
                }
        );
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.buitin_packets.MagicLink.Handshake.Failure packet) {
        logger.send(Component.text(packet.reason(), NamedTextColor.RED));
        logger.send(Component.text("Waiting 1 minute before trying again...", NamedTextColor.GRAY));
        RC.M.MagicLink().setDelay(60);
    }
}
