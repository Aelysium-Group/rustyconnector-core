package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an attempt to register a server to the proxy.
 */
public class ServerRegisterEvent extends Event.Cancelable {
    protected final Particle.Flux<? extends Family> family;
    protected final Server.Configuration server;

    public ServerRegisterEvent(Particle.Flux<? extends Family> family, Server.Configuration server) {
        super();
        this.family = family;
        this.server = server;
    }

    public Particle.Flux<? extends Family> family() {
        return family;
    }

    /**
     * The server configuration.
     * Because the server has not yet actually been registered,
     * there's no server object.
     * @return The server configuration being used to register.
     */
    public Server.Configuration server() {
        return server;
    }
}