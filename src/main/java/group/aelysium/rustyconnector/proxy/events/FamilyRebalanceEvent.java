package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;

/**
 * Represents a family rebalancing its Servers via it's load balancer.
 */
public class FamilyRebalanceEvent extends Event {
    protected final Flux<? extends Family> family;

    public FamilyRebalanceEvent(Flux<? extends Family> family) {
        super();
        this.family = family;
    }

    public Flux<? extends Family> family() {
        return this.family;
    }
}