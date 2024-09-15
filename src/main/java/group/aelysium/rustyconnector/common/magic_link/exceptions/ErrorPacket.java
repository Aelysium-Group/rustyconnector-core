package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class ErrorPacket extends PacketStatusResponse {
    public ErrorPacket(String reason) {
        super(Packet.Status.ERROR, reason);
    }
    public ErrorPacket() {
        this("The request made by the packet has finished with an error.");
    }
}
