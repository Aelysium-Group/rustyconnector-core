package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;

/**
 * Represents a family rebalancing its Servers via it's load balancer.
 */
public class FamilyRebalanceEvent extends Event {
    public final Family family;

    public FamilyRebalanceEvent(Family family) {
        super();
        this.family = family;
    }
}