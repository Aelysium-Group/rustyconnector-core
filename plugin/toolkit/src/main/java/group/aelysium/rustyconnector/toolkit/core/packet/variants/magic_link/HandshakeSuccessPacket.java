package group.aelysium.rustyconnector.toolkit.core.packet.variants.magic_link;

import group.aelysium.rustyconnector.toolkit.core.packet.GenericPacket;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.core.server.ServerAssignment;
import group.aelysium.rustyconnector.toolkit.velocity.util.ColorMapper;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HandshakeSuccessPacket extends GenericPacket {
    public String message() {
        return this.parameters.get(Parameters.MESSAGE).getAsString();
    }
    public NamedTextColor color() {
        return ColorMapper.map(this.parameters.get(Parameters.COLOR).getAsString());
    }
    public Integer pingInterval() {
        return this.parameters.get(Parameters.INTERVAL).getAsInt();
    }
    public ServerAssignment assignment() {
        try {
            return ServerAssignment.valueOf(this.parameters.get(Parameters.ASSIGNMENT).getAsString());
        } catch (Exception ignore) {}
        return ServerAssignment.GENERIC;
    }

    private HandshakeSuccessPacket() { super(); }

    public interface Parameters {
        String MESSAGE = "m";
        String COLOR = "c";
        String INTERVAL = "i";
        String ASSIGNMENT = "a";

        static List<String> toList() {
            List<String> list = new ArrayList<>();
            list.add(MESSAGE);
            list.add(COLOR);
            list.add(ASSIGNMENT);
            list.add(INTERVAL);

            return list;
        }
    }

    public static HandshakeFailurePacket create(UUID uuid) {
        return new MCLoaderPacketBuilder()
                .identification(PacketIdentification.Predefined.MAGICLINK_HANDSHAKE_FAIL)
                .sendingToAnotherMCLoader(uuid)
                .build();
    }
}
