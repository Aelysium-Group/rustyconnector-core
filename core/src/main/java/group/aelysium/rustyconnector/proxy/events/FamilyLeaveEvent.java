package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player leaving a family.
 * This event will only fire once {@link FamilyPostJoinEvent} has fired.
 * It can be assumed that if this event fires, the player has successfully acquired a new origin.
 * This event will also fire if a player leaves the family by logging out of the network.
 */
public class FamilyLeaveEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final Server server;
    protected final Player player;
    protected final boolean disconnected;

    public FamilyLeaveEvent(
            @NotNull Particle.Flux<Family> family,
            @NotNull Server server,
            @NotNull Player player,
            boolean disconnected
    ) {
        this.family = family;
        this.server = server;
        this.player = player;
        this.disconnected = disconnected;
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
    public boolean disconnected() {
        return disconnected;
    }
}