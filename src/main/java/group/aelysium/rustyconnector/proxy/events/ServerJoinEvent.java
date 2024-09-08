package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.common.events.Event;

/**
 * Represents a player joining an Server.
 * This event will only fire once {@link FamilyPostJoinEvent} has fired.
 */
public class ServerJoinEvent implements Event {
    protected final Server server;
    protected final Player player;

    public ServerJoinEvent(Server server, Player player) {
        this.server = server;
        this.player = player;
    }

    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
}