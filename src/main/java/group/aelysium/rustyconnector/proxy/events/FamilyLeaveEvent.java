package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player leaving a family.
 * It can be assumed that if this event fires, the player has successfully acquired a new origin.
 * This event will also fire if a player leaves the family by logging out of the network.
 */
public class FamilyLeaveEvent extends Event {
    protected final Particle.Flux<? extends Family> family;
    protected final Server server;
    protected final Player player;
    protected final boolean disconnected;

    public FamilyLeaveEvent(
            @NotNull Particle.Flux<? extends Family> family,
            @NotNull Server server,
            @NotNull Player player,
            boolean disconnected
    ) {
        super();
        this.family = family;
        this.server = server;
        this.player = player;
        this.disconnected = disconnected;
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
    public boolean disconnected() {
        return disconnected;
    }
}