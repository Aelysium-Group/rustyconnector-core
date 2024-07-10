package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player joining the network.
 * This event fires after {@link FamilyPostJoinEvent}.
 */
public class NetworkJoinEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final Server server;
    protected final Player player;

    public NetworkJoinEvent(Particle.Flux<Family> family, Server server, Player player) {
        this.family = family;
        this.server = server;
        this.player = player;
    }

    public Particle.Flux<Family> family() {
        return family;
    }
    public Server mcLoader() {
        return server;
    }
    public Player player() {
        return player;
    }
}