package group.aelysium.rustyconnector.server.magic_link.handlers;

import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.events.TimeoutEvent;
import group.aelysium.rustyconnector.RC;

public class HandshakeStalePingListener {
    @PacketListener(MagicLinkCore.Packets.StalePing.class)
    public PacketListener.Response handle(MagicLinkCore.Packets.StalePing packet) {
        ServerKernel flame = RC.S.Kernel();
        RC.S.EventManager().fireEvent(new TimeoutEvent());

        ServerKernel kernel = RC.S.Kernel();
        JsonObject metadata = new JsonObject();
        kernel.parameterizedMetadata().forEach((k, v)-> metadata.add(k, v.toJSON()));

        RC.S.MagicLink().setDelay(5);
        Packet.New()
                .identification(Packet.Type.from("RC", "SP"))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.ADDRESS, flame.address().getHostName() + ":" + flame.address().getPort())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.METADATA, new Packet.Parameter(metadata))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.TARGET_FAMILY, kernel.targetFamily())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(flame.playerCount()))
                .addressTo(packet)
                .send();
        return PacketListener.Response.success("Successfully pinged the proxy.");
    }
}
