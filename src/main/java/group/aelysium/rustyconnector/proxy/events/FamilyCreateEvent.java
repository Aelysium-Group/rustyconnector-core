package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a new family being created.
 */
public class FamilyCreateEvent extends Event {
    protected final Particle.Flux<? extends Family> family;

    public FamilyCreateEvent(@NotNull Particle.Flux<? extends Family> family) {
        super();
        this.family = family;
    }

    public Particle.Flux<? extends Family> family() {
        return family;
    }
}