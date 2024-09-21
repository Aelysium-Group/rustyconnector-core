package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player attempting to join the network.
 */
public class NetworkPreJoinEvent extends Event.Cancelable {
    protected final Player player;

    public NetworkPreJoinEvent(Player player) {
        super();
        this.player = player;
    }

    public Player player() {
        return player;
    }
}