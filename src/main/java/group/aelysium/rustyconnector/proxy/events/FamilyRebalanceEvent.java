package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;

/**
 * Represents a family rebalancing its Servers via it's load balancer.
 */
public class FamilyRebalanceEvent extends Event {
    protected final Particle.Flux<? extends Family> family;

    public FamilyRebalanceEvent(Particle.Flux<? extends Family> family) {
        super();
        this.family = family;
    }

    public Particle.Flux<? extends Family> family() {
        return this.family;
    }
}