package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SendPlayerListener {
    @PacketListener(MagicLinkCore.Packets.SendPlayer.class)
    public PacketListener.Response handle(MagicLinkCore.Packets.SendPlayer packet) throws Exception {
        if(packet.target().isEmpty())
            throw new IllegalStateException("You must define either a target family or server.");
        if(packet.playerID().isEmpty() && packet.playerUsername().isEmpty())
            throw new IllegalStateException("You must define a user to send.");

        Player player = null;
        try {
            if(packet.playerUsername().isPresent()) player = RC.P.PlayerFromUsername(packet.playerUsername().orElseThrow()).orElseThrow();
            else if(packet.playerID().isPresent()) player = RC.P.PlayerFromID(packet.playerID().orElseThrow()).orElseThrow();
        } catch (NoSuchElementException ignore) {}
        if(player == null || !player.online()) throw new NoSuchElementException("No player '"+packet.player()+"' is online.");

        Set<MagicLinkCore.Packets.SendPlayer.Flag> flags = packet.flags();
        boolean sendFamily = flags.contains(MagicLinkCore.Packets.SendPlayer.Flag.FAMILY);
        boolean sendServer = flags.contains(MagicLinkCore.Packets.SendPlayer.Flag.SERVER);
        String target = packet.target();
        if(!sendFamily && !sendServer) { // Neither of the flags was defined. It's a generic search.
            Optional<? extends Family> familyOptional = RC.P.Family(target);
            Optional<Server> serverOptional = RC.P.Server(target);

            if(familyOptional.isPresent() && serverOptional.isPresent())
                throw new RuntimeException("Both a server and family have the id `"+target+"`. Please clarify if you want to send the player to a family or a server.");

            sendFamily = familyOptional.isPresent();
            sendServer = serverOptional.isPresent();
        }

        Player.Connection.Power power = Player.Connection.Power.MINIMAL;
        if(flags.contains(MagicLinkCore.Packets.SendPlayer.Flag.MODERATE)) power = Player.Connection.Power.MODERATE;
        if(flags.contains(MagicLinkCore.Packets.SendPlayer.Flag.AGGRESSIVE)) power = Player.Connection.Power.AGGRESSIVE;

        if(sendFamily) {
            Family family = RC.P.Family(target)
                    .orElseThrow(()->new NoSuchElementException("No family with the id '"+target+"' exists."));

            Player.Connection.Result result = family.connect(player, power).result().get(10, TimeUnit.SECONDS);

            if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
        } else if(sendServer) {
            Server server = RC.P.Server(target)
                    .orElseThrow(()->new NoSuchElementException("No server with the id '"+target+"' exists."));

            Player.Connection.Result result = server.connect(player, power).result().get(10, TimeUnit.SECONDS);

            if(!result.connected()) throw new RuntimeException("Unable to connect the player to that server.");
        }

        return PacketListener.Response.success("Successfully sent "+player.username()+"!").asReply();
    }
}
