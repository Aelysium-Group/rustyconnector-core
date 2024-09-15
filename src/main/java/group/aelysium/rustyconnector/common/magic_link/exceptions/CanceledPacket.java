package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class CanceledPacket extends PacketStatusResponse {
    public CanceledPacket(String reason) {
        super(Packet.Status.CANCELED, reason);
    }
    public CanceledPacket() {
        this("The request made by the packet has been canceled.");
    }
}
