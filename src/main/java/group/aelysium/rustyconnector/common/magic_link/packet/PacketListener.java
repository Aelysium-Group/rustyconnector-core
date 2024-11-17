package group.aelysium.rustyconnector.common.magic_link.packet;

public abstract class PacketListener<P extends Packet.Remote> {
    /**
     * If enabled, exceptions thrown by the {@link #handle(Packet.Remote)} method will be
     */
    protected boolean responseFromExceptions = true;
    /**
     * If enabled, overrides {@link Packet.Response#shouldSendPacket()} and will always send a packet when a response is made.<br/>
     * Note that if {@link #responseFromExceptions} is false, exceptions won't send replies to packets since no response was generated from said exception.
     */
    protected boolean responsesAsPacketReplies = false;

    /**
     * Handles a packet and returns a response.
     * @param packet The packet to handle.
     * @return A response indicating either success or failure. Responses may also trigger a reply packet is sent.
     * @throws Exception If there's an issue handling the packet.
     *                   Listener exceptions are automatically handled by MagicLink.
     *                   If you want a packet response to be generated from listener exceptions, look at {@link #responseFromExceptions} and {@link #responsesAsPacketReplies}.
     */
    public abstract Packet.Response handle(P packet) throws Exception;
}