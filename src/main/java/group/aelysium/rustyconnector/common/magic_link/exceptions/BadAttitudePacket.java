package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class BadAttitudePacket extends PacketStatusResponse {
    public BadAttitudePacket(String reason) {
        super(Packet.Status.BAD_ATTITUDE, reason);
    }
    public BadAttitudePacket() {
        this("The packet has a bad attitude.");
    }
}
