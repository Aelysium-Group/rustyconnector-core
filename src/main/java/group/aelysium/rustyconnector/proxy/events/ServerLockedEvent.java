package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an Server being locked on its family.
 */
public class ServerLockedEvent extends Event.Cancelable {
    protected final Particle.Flux<? extends Family> family;
    protected final Server server;

    public ServerLockedEvent(Particle.Flux<? extends Family> family, Server server) {
        super();
        this.family = family;
        this.server = server;
    }

    public Particle.Flux<? extends Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
}