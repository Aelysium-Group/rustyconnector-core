package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.IMCLoader;

/**
 * Represents an MCLoader successfully registering to the Proxy.
 */
public class ServerRegisterEvent implements Event {
    protected final Particle.Flux<IFamily> family;
    protected final IMCLoader mcLoader;

    public ServerRegisterEvent(Particle.Flux<IFamily> family, IMCLoader mcLoader) {
        this.family = family;
        this.mcLoader = mcLoader;
    }

    public Particle.Flux<IFamily> family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
}