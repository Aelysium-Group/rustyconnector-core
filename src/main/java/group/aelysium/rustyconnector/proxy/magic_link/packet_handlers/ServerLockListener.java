package group.aelysium.rustyconnector.proxy.magic_link.packet_handlers;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.events.ServerLockedEvent;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.NoSuchElementException;

public class ServerLockListener extends PacketListener<Server.Packets.Lock> {
    public Packet.Response handle(Server.Packets.Lock packet) {
        try {
            Server server = RC.P.Server(packet.local().id())
                    .orElseThrow(()->new NoSuchElementException("No server with the id "+packet.local().id()+" exists."));

            boolean success = server.lock();

            if(!success) throw new RuntimeException("An unknown error prevented the locking of the server: "+server.id()+" "+server.property("velocity_registration_name").orElse(""));

            RC.EventManager().fireEvent(new ServerLockedEvent(server.family().orElseThrow(), server));
            return Packet.Response.success("Successfully locked the server.").asReply();
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Packet.Response.error("There was an issue locking the server. Check the proxy for more details. "+e.getMessage()).asReply();
        }
    }
}