package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.TimeUnit;

public class HandshakePingListener {
    @PacketListener(WebSocketMagicLink.Packets.Handshake.Ping.class)
    public static void execute(WebSocketMagicLink.Packets.Handshake.Ping packet) throws Exception {
        try {
            Server server = RC.P.Server(packet.sender().uuid()).orElseThrow();

            server.setTimeout(15);
            server.setPlayerCount(packet.playerCount());
        } catch (Exception e) {
            MagicLinkCore.Proxy magicLink = RC.P.MagicLink();
            MagicLinkCore.Proxy.ServerRegistrationConfiguration config = magicLink.registrationConfig(packet.serverRegistration()).orElseThrow(
                    () -> new NullPointerException("No Server Registration exists with the name "+packet.serverRegistration()+"!")
            );

            try {
                Particle.Flux<? extends Family> familyFlux = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().find(config.family()).orElseThrow(() ->
                        new InvalidAlgorithmParameterException("A family with the id `"+config.family()+"` doesn't exist!")
                );
                Family family = familyFlux.access().get(10, TimeUnit.SECONDS);

                RC.P.Server(packet.sender().uuid()).ifPresent(m -> {
                    throw new RuntimeException("Server " + packet.sender().uuid() + " can't be registered twice!");
                });

                Server server = family.generateServer(
                        packet.sender().uuid(),
                        AddressUtil.parseAddress(packet.address()),
                        packet.podName().orElse(null),
                        packet.displayName().orElse(null),
                        config.soft_cap(),
                        config.hard_cap(),
                        config.weight(),
                        15
                );

                RC.P.EventManager().fireEvent(new ServerRegisterEvent(familyFlux, server));

                Packet.New()
                        .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_SUCCESS)
                        .parameter(WebSocketMagicLink.Packets.Handshake.Success.Parameters.MESSAGE, "Connected to the proxy! Registered as `"+server.uuidOrDisplayName()+"` into the family `"+family.id()+"`. Loaded using the magic config `"+packet.serverRegistration()+"`.")
                        .parameter(WebSocketMagicLink.Packets.Handshake.Success.Parameters.COLOR, NamedTextColor.GREEN.toString())
                        .parameter(WebSocketMagicLink.Packets.Handshake.Success.Parameters.INTERVAL, new Packet.Parameter(10))
                        .addressedTo(packet)
                        .send();
            } catch(Exception e2) {
                Packet.New()
                        .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_FAIL)
                        .parameter(WebSocketMagicLink.Packets.Handshake.Failure.Parameters.REASON, "Attempt to connect to proxy failed! " + e2.getMessage())
                        .addressedTo(packet)
                        .send();
            }
        }
    }
}
