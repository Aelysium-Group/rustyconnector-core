package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

import java.util.Optional;

/**
 * Represents the Magic Link connection to a Server timing out.
 */
public class ServerTimeoutEvent extends Event {
    protected final Family family;
    protected final Server server;

    public ServerTimeoutEvent(Server server, Family f) {
        super();
        this.server = server;
        this.family = f;
    }

    public Family family() {
        return family;
    }
    public Server server() {
        return server;
    }
}