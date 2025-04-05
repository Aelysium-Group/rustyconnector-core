package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player joining the network.
 * This event fires after {@link FamilyLeaveEvent}.
 */
public class NetworkLeaveEvent extends Event {
    public final Player player;

    public NetworkLeaveEvent(Player player) {
        super();
        this.player = player;
    }
}