package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a new family being created.
 */
public class FamilyUnregisterEvent extends Event {
    public final Family family;

    public FamilyUnregisterEvent(@NotNull Family family) {
        super();
        this.family = family;
    }
}