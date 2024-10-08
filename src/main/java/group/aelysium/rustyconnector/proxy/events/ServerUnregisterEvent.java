package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.Optional;

/**
 * Represents an Server unregistering from the Proxy.
 */
public class ServerUnregisterEvent extends Event {
    protected final Particle.Flux<? extends Family> family;
    protected final Server server;

    public ServerUnregisterEvent(Server server) {
        super();
        this.family = server.family().orElse(null);
        this.server = server;
    }

    public Optional<Particle.Flux<? extends Family>> family() {
        return Optional.ofNullable(family);
    }
    public Server server() {
        return server;
    }
}