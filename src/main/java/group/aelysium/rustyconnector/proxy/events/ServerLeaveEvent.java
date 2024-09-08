package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player leaving an Server.
 * This event will only fire once {@link FamilyLeaveEvent} has fired.
 * It can be assumed that if this event fires, the player has successfully acquired a new origin.
 * This event will also fire if a player leaves the family by logging out of the network.
 */
public class ServerLeaveEvent implements Event {
    protected final Server server;
    protected final Player player;
    protected final boolean disconnected;

    public ServerLeaveEvent(
            @NotNull Server server,
            @NotNull Player player,
            boolean disconnected
    ) {
        this.server = server;
        this.player = player;
        this.disconnected = disconnected;
    }

    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
    public boolean disconnected() {
        return disconnected;
    }
}