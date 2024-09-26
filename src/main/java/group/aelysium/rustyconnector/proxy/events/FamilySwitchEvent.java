package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player switching from one family to another family.
 */
public class FamilySwitchEvent extends Event {
    protected final Particle.Flux<? extends Family> oldFamily;
    protected final Particle.Flux<? extends Family> newFamily;
    protected final Server oldServer;
    protected final Server newServer;
    protected final Player player;

    public FamilySwitchEvent(@NotNull Particle.Flux<? extends Family> oldFamily, @NotNull Particle.Flux<? extends Family> newFamily, @NotNull Server oldServer, @NotNull Server newServer, @NotNull Player player) {
        super();
        this.oldFamily = oldFamily;
        this.newFamily = newFamily;
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }

    public Particle.Flux<? extends Family> oldFamily() {
        return oldFamily;
    }
    public Particle.Flux<? extends Family> newFamily() {
        return newFamily;
    }
    public Server oldServer() {
        return oldServer;
    }
    public Server newServer() {
        return newServer;
    }
    public Player player() {
        return player;
    }
}