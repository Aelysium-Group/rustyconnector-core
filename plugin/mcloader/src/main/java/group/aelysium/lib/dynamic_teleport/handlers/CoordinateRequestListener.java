package group.aelysium.lib.dynamic_teleport.handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.central.MCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.common.packet.PacketListener;
import group.aelysium.rustyconnector.common.packets.QueueTPAPacket;
import group.aelysium.TinderAdapterForCore;
import group.aelysium.lib.lang.MCLoaderLang;
import group.aelysium.lib.dynamic_teleport.CoordinateRequest;

import java.util.UUID;

public class CoordinateRequestListener extends PacketListener<QueueTPAPacket> {
    protected IMCLoaderTinder api;

    public CoordinateRequestListener(IMCLoaderTinder api) {
        this.api = api;
    }

    public PacketIdentification target() {
        return BuiltInIdentifications.QUEUE_TPA;
    }

    public QueueTPAPacket wrap(Packet packet) {
        return new QueueTPAPacket(packet);
    }

    @Override
    public void execute(QueueTPAPacket packet) throws Exception {
        UUID target = api.getPlayerUUID(packet.targetUsername());
        if(target == null) return;
        if(!api.isOnline(target)) return;

        CoordinateRequest coordinateRequest = ((MCLoaderTinder) api).services().dynamicTeleport().newRequest(packet.sourceUsername(), target);

        // Attempt to resolve the tpa right away! If the player isn't on the server, this should fail silently.
        try {
            coordinateRequest.resolveClient();

            try {
                coordinateRequest.teleport();
            } catch (Exception e) {
                e.printStackTrace();
                api.sendMessage(coordinateRequest.client().orElseThrow(), MCLoaderLang.TPA_FAILED_TELEPORT.build(TinderAdapterForCore.getTinder().getPlayerName(coordinateRequest.target())));
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
