package group.aelysium.rustyconnector.toolkit.velocity.events.family;

import group.aelysium.rustyconnector.toolkit.core.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.Family;

/**
 * Represents a family rebalancing its MCLoaders via it's load balancer.
 */
public class FamilyRebalanceEvent implements Event {
    protected final Family family;

    public FamilyRebalanceEvent(Family family) {
        this.family = family;
    }

    public Family family() {
        return this.family;
    }
}