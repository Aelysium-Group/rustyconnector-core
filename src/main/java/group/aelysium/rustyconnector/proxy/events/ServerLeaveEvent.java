package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player leaving a Server.
 * This event will only fire once {@link FamilyLeaveEvent} has fired.
 * It can be assumed that if this event fires, the player has successfully acquired a new origin.
 * This event will also fire if a player leaves the family by logging out of the network.
 */
public class ServerLeaveEvent extends Event {
    public final Server server;
    public final Player player;
    public final boolean disconnected;

    public ServerLeaveEvent(
            @NotNull Server server,
            @NotNull Player player,
            boolean disconnected
    ) {
        super();
        this.server = server;
        this.player = player;
        this.disconnected = disconnected;
    }
}