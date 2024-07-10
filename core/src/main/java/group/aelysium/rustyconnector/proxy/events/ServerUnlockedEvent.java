package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an MCLoader being unlocked on this family.
 */
public class ServerUnlockedEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final Server server;

    public ServerUnlockedEvent(Particle.Flux<Family> family, Server server) {
        this.family = family;
        this.server = server;
    }

    public Particle.Flux<Family> family() {
        return family;
    }
    public Server mcLoader() {
        return server;
    }
}