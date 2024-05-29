package group.aelysium.rustyconnector.proxy.family.mcloader.packet_handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.packets.SendPlayerPacket;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import net.kyori.adventure.text.Component;

public class SendPlayerListener extends PacketListener<SendPlayerPacket> {
    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.SEND_PLAYER;
    }

    @Override
    public SendPlayerPacket wrap(Packet packet) {
        return new SendPlayerPacket(packet);
    }

    @Override
    public void execute(SendPlayerPacket packet) throws Exception {
        IPlayer player = RC.P.Player(packet.uuid());

        try {
            IMCLoader server;
            try {
                server = player.server().orElseThrow();
            } catch (Exception e) {
                throw new RuntimeException("You don't seem to be connected to a server at this moment!");
            }

            if(RC.P.Family(packet.targetFamilyName()).equals(server.family())) throw new RuntimeException("You're already connected to this server!");

            family.connect(player);
        } catch (Exception e) {
            player.sendMessage(Component.text(e.getMessage()));
        }
    }
}
