package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents a Server being unlocked on its family.
 */
public class ServerUnlockedEvent extends Event.Cancelable {
    protected final Particle.Flux<? extends Family> family;
    protected final Server server;

    public ServerUnlockedEvent(Particle.Flux<? extends Family> family, Server server) {
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