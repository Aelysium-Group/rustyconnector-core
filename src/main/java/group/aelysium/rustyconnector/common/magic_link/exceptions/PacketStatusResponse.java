package group.aelysium.rustyconnector.common.magic_link.exceptions;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public class PacketStatusResponse extends Throwable {
    protected final Packet.Status status;
    public PacketStatusResponse(Packet.Status status, String reason) {
        super(reason);
        this.status = status;
    }

    public Packet.Status status() {
        return this.status;
    }
}
