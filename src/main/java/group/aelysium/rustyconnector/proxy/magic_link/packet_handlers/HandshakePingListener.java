package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HandshakePingListener {
    @PacketListener(MagicLinkCore.Packets.Ping.class)
    public PacketListener.Response handle(WebSocketMagicLink.Packets.Ping packet) {
        try {
            Server server = RC.P.Server(packet.local().id()).orElseThrow();

            server.setTimeout(15);
            server.setPlayerCount(packet.playerCount());
            return PacketListener.Response.success("Refreshed the server's timeout!");
        } catch (Exception ignore) {}

        try {
            Flux<Family> familyFlux = RC.P.Families().find(packet.targetFamily());
            Family family = familyFlux.get(10, TimeUnit.SECONDS);

            RC.P.Server(packet.local().id()).ifPresent(m -> {
                throw new RuntimeException("Server " + packet.local().id() + " can't be registered twice!");
            });

            // Some family metadata is supposed to effect the server meta, that's done here.
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("softCap", family.fetchMetadata("serverSoftCap").orElse(30));
            metadata.put("hardCap", family.fetchMetadata("serverHardCap").orElse(40));
            if(family.fetchMetadata("displayName").isPresent())
                metadata.put("displayName", family.fetchMetadata("displayName").orElse(null));
            metadata.putAll(packet.metadata());

            Server.Configuration configuration = new Server.Configuration(
                packet.local().id(),
                AddressUtil.parseAddress(packet.address()),
                metadata,
                15
            );
            RC.P.Kernel().registerServer(familyFlux, configuration);

            return PacketListener.Response.success(
                    "Connected to the proxy! Registered into the family `"+family.id()+"` using the configuration `"+packet.targetFamily()+"`.",
                    Map.of(
                            "i", new Parameter(10)
                    )
            ).asReply();
        } catch(Exception e) {
            RC.Error(Error.from(e));
            return PacketListener.Response.error("Attempt to connect to proxy failed! " + e.getMessage()).asReply();
        }
    }
}