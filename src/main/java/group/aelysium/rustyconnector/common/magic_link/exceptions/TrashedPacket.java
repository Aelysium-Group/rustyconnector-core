package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class TrashedPacket extends PacketStatusResponse {
    public TrashedPacket(String reason) {
        super(Packet.Status.TRASHED, reason);
    }
    public TrashedPacket() {
        this("The packet was thrown away.");
    }
}
