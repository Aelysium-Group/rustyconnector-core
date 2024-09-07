package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player successfully connecting to a family.
 */
public class FamilyPostJoinEvent implements Event {
    protected final Particle.Flux<? extends Family> family;
    protected final Server server;
    protected final Player player;

    public FamilyPostJoinEvent(Particle.Flux<? extends Family> family, Server server, Player player) {
        this.family = family;
        this.server = server;
        this.player = player;
    }

    public Particle.Flux<? extends Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
}