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
    protected final Flux<? extends Family> family;
    protected final Server server;
    protected final Player player;

    public NetworkPostJoinEvent(Flux<? extends Family> family, Server server, Player player) {
        super();
        this.family = family;
        this.server = server;
        this.player = player;
    }

    public Flux<? extends Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
}