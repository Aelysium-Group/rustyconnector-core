package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;

/**
 * Represents a family rebalancing its Servers via it's load balancer.
 */
public class FamilyRebalanceEvent implements Event {
    protected final Particle.Flux<Family> family;

    public FamilyRebalanceEvent(Particle.Flux<Family> family) {
        this.family = family;
    }

    public Particle.Flux<Family> family() {
        return this.family;
    }
}