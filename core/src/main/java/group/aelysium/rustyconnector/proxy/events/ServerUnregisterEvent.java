package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.IMCLoader;

/**
 * Represents an MCLoader unregistering from the Proxy.
 */
public class ServerUnregisterEvent implements Event {
    protected final IFamily family;
    protected final IMCLoader mcLoader;

    public ServerUnregisterEvent(IFamily family, IMCLoader mcLoader) {
        this.family = family;
        this.mcLoader = mcLoader;
    }

    public IFamily family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
}