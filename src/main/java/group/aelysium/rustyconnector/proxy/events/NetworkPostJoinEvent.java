package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player successfully joining the network.
 * This event fires after {@link FamilyPostJoinEvent}.
 */
public class NetworkPostJoinEvent extends Event {
    public final Family family;
    public final Server server;
    public final Player player;

    public NetworkPostJoinEvent(Family family, Server server, Player player) {
        super();
        this.family = family;
        this.server = server;
        this.player = player;
    }
}