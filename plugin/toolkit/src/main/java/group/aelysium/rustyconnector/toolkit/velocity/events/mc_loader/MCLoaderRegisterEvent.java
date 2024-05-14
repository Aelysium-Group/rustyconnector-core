package group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader;

import group.aelysium.rustyconnector.toolkit.core.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.Family;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;

/**
 * Represents an MCLoader successfully registering to the Proxy.
 */
public class MCLoaderRegisterEvent implements Event {
    protected final Family family;
    protected final IMCLoader mcLoader;

    public MCLoaderRegisterEvent(Family family, IMCLoader mcLoader) {
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