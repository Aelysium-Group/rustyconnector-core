package group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader;

import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.Family;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

/**
 * Represents an MCLoader unregistering from the Proxy.
 */
public class MCLoaderUnregisterEvent implements Event {
    protected final Family family;
    protected final IMCLoader mcLoader;

    public MCLoaderUnregisterEvent(Family family, IMCLoader mcLoader) {
        this.family = family;
        this.mcLoader = mcLoader;
    }

    public Family family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
}