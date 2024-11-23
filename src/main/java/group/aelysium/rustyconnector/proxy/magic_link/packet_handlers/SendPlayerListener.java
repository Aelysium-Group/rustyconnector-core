package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import com.sun.jdi.request.DuplicateRequestException;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SendPlayerListener {
    @PacketListener(MagicLinkCore.Packets.SendPlayer.class)
    public PacketListener.Response handle(MagicLinkCore.Packets.SendPlayer packet) throws Exception {
        if(packet.targetFamily().isEmpty() && packet.targetServer().isEmpty() && packet.genericTarget().isEmpty())
            throw new IllegalStateException("You must define either a target family or server.");
        if(packet.playerUUID().isEmpty() && packet.playerUsername().isEmpty())
            throw new IllegalStateException("You must define a user to send.");

        Player player = null;
        try {
            if(packet.playerUsername().isPresent()) player = RC.P.Player(packet.playerUsername().orElseThrow()).orElseThrow();
            if(packet.playerUUID().isPresent()) player = RC.P.Player(packet.playerUUID().orElseThrow()).orElseThrow();
        } catch (NoSuchElementException ignore) {}
        if(player == null || !player.online()) throw new NoSuchElementException("No player '"+packet.player()+"' is online.");

        boolean sendFamily = packet.targetFamily().isPresent();
        boolean sendServer = packet.targetServer().isPresent();
        if(packet.genericTarget().isPresent()) {
            String target = packet.genericTarget().orElseThrow();
            Optional<? extends Family> familyOptional = RC.P.Family(target);
            Optional<Server> serverOptional = RC.P.Server(target);

            if(familyOptional.isPresent() && serverOptional.isPresent())
                throw new DuplicateRequestException("Both a server and family have the id `"+target+"`. Please clarify if you want to send the player to a family or a server.");

            sendFamily = familyOptional.isPresent();
            sendServer = serverOptional.isPresent();
        }

        if(sendFamily) {
            String familyID = packet.targetFamily().orElseThrow();
            Family family = RC.P.Family(familyID)
                    .orElseThrow(()->new NoSuchElementException("No family with the id '"+familyID+"' exists."));

            Player.Connection.Result result = family.connect(player).result().get(10, TimeUnit.SECONDS);

            if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
        }
        if(sendServer) {
            String serverID = packet.targetFamily().orElseThrow();
            Server server = RC.P.Server(serverID)
                    .orElseThrow(()->new NoSuchElementException("No family with the id '"+serverID+"' exists."));

            Player.Connection.Result result = server.connect(player).result().get(10, TimeUnit.SECONDS);

            if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
        }

        return PacketListener.Response.success("Successfully sent "+player.username()+"!").asReply();
    }
}
