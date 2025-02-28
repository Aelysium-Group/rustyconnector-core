package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an Server being locked on its family.
 */
public class ServerLockedEvent extends Event.Cancelable {
    protected final Flux<? extends Family> family;
    protected final Server server;

    public ServerLockedEvent(Flux<? extends Family> family, Server server) {
        super();
        this.family = family;
        this.server = server;
    }

    public Flux<? extends Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
}