package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class WrongSourcePacket extends PacketStatusResponse {
    public WrongSourcePacket(String reason) {
        super(Packet.Status.WRONG_SOURCE, reason);
    }
    public WrongSourcePacket() {
        this("The packet was thrown away because it's addressed to a source other than this one.");
    }
}
