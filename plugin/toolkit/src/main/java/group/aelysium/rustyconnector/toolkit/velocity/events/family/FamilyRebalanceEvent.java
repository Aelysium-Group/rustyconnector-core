package group.aelysium.rustyconnector.toolkit.velocity.events.family;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;

/**
 * Represents a family rebalancing its MCLoaders via it's load balancer.
 */
public class FamilyRebalanceEvent implements Event {
    protected final Particle.Flux<IFamily> family;

    public FamilyRebalanceEvent(Particle.Flux<IFamily> family) {
        this.family = family;
    }

    public Particle.Flux<IFamily> family() {
        return this.family;
    }
}