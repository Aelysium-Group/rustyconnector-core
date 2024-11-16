package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerLockedEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SendPlayerListener extends PacketListener<MagicLinkCore.Packets.SendPlayer> {
    public Packet.Response handle(MagicLinkCore.Packets.SendPlayer packet) {
        try {
            if(packet.targetFamilyName() == null && packet.targetServer() == null)
                throw new IllegalStateException("You must define either a target family or server.");
            if(packet.playerUUID() == null)
                throw new IllegalStateException("You must define a user to send.");

            Player player = RC.P.Player(packet.playerUUID())
                    .orElseThrow(()->new NoSuchElementException("No player with the UUID '"+packet.playerUUID()+"' exists."));
            if(!player.online()) throw new NoSuchElementException("No player with the UUID '"+packet.playerUUID()+"' is online.");

            if(packet.targetFamilyName() != null) {
                Family family = RC.P.Family(packet.targetFamilyName())
                        .orElseThrow(()->new NoSuchElementException("No family with the id '"+packet.targetFamilyName()+"' exists."));

                Player.Connection.Result result = family.connect(player).result().get(10, TimeUnit.SECONDS);

                if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
            }
            if(packet.targetServer() != null) {
                Server server = RC.P.Server(packet.targetServer())
                        .orElseThrow(()->new NoSuchElementException("No family with the id '"+packet.targetFamilyName()+"' exists."));

                Player.Connection.Result result = server.connect(player).result().get(10, TimeUnit.SECONDS);

                if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
            }

            return Packet.Response.success("Successfully sent "+player.username()+"!").asReply();
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Packet.Response.error(e.getMessage()).asReply();
        }
    }
}
