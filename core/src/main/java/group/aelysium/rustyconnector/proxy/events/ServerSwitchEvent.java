package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player switching from one server to another.
 * This event doesn't care about what family the servers are a part of.
 */
public class ServerSwitchEvent implements Event {
    protected final Server oldServer;
    protected final Server newServer;
    protected final Player player;

    public ServerSwitchEvent(Server oldServer, Server newServer, Player player) {
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }

    public Server oldServer() {
        return oldServer;
    }
    public Server newServer() {
        return newServer;
    }
    public Player player() {
        return player;
    }
}