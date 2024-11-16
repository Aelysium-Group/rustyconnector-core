package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;

import java.security.InvalidAlgorithmParameterException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HandshakePingListener extends PacketListener<MagicLinkCore.Packets.Handshake.Ping> {
    public Packet.Response handle(WebSocketMagicLink.Packets.Handshake.Ping packet) {
        try {
            Server server = RC.P.Server(packet.local().uuid()).orElseThrow();

            server.setTimeout(15);
            server.setPlayerCount(packet.playerCount());
            return Packet.Response.success("Refreshed the server's timeout!");
        } catch (Exception ignore) {}

        MagicLinkCore.Proxy magicLink = RC.P.MagicLink();
        MagicLinkCore.Proxy.ServerRegistrationConfiguration config = magicLink.registrationConfig(packet.serverRegistration()).orElseThrow(
                () -> new NullPointerException("No Server Registration exists with the name "+packet.serverRegistration()+"!")
        );

        try {
            Particle.Flux<? extends Family> familyFlux = RC.P.Families().find(config.family()).orElseThrow(() ->
                    new InvalidAlgorithmParameterException("A family with the id `"+config.family()+"` doesn't exist!")
            );
            Family family = familyFlux.access().get(10, TimeUnit.SECONDS);

            RC.P.Server(packet.local().uuid()).ifPresent(m -> {
                throw new RuntimeException("Server " + packet.local().uuid() + " can't be registered twice!");
            });

            String displayName = packet.displayName().orElse(null);

            Server.Configuration configuration = new Server.Configuration(
                packet.local().uuid(),
                AddressUtil.parseAddress(packet.address()),
                displayName == null ? null : (displayName.isBlank() || displayName.isEmpty() ? null : displayName),
                config.soft_cap(),
                config.hard_cap(),
                config.weight(),
                15
            );
            RC.P.Kernel().registerServer(familyFlux, configuration);

            return Packet.Response.success(
                    "Connected to the proxy! Registered into the family `"+family.id()+"` using the configuration `"+packet.serverRegistration()+"`.",
                    Map.of(
                            WebSocketMagicLink.Packets.Handshake.Success.Parameters.INTERVAL, new Packet.Parameter(10)
                    )
            ).asReply();
        } catch(Exception e) {
            RC.Error(Error.from(e));
            return Packet.Response.error("Attempt to connect to proxy failed! " + e.getMessage()).asReply();
        }
    }
}
