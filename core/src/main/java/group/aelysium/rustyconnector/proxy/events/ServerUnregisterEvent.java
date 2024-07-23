package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an MCLoader unregistering from the Proxy.
 */
public class ServerUnregisterEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final Server server;

    public ServerUnregisterEvent(Server server) {
        this.family = server.family();
        this.server = server;
    }

    public Particle.Flux<Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
}