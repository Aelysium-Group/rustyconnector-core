package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerLockedEvent;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.NoSuchElementException;

public class ServerLockListener {
    @PacketListener(Server.Packets.Lock.class)
    public PacketListener.Response handle(Server.Packets.Lock packet) {
        try {
            Server server = RC.P.Server(packet.local().namespace())
                    .orElseThrow(()->new NoSuchElementException("No server with the id "+packet.local().namespace()+" exists."));

            boolean success = server.lock();

            if(!success) throw new RuntimeException("An unknown error prevented the locking of the server: "+server.id());

            RC.EventManager().fireEvent(new ServerLockedEvent(server.family().orElseThrow(), server));
            return PacketListener.Response.success("Successfully locked the server.").asReply();
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return PacketListener.Response.error("There was an issue locking the server. Check the proxy for more details. "+e.getMessage()).asReply();
        }
    }
}
