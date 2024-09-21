package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class SuccessPacket extends PacketStatusResponse {
    public SuccessPacket(String reason) {
        super(Packet.Status.SUCCESS, reason);
    }
    public SuccessPacket() {
        this("The packet was handled successfully. This status doesn't necessarily mean that the handlers themselves succeeded. Just that there were no issue during the handling.");
    }
}
