package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents a Server being locked on its family.
 */
public class ServerLockedEvent extends Event.Cancelable {
    protected final Family family;
    protected final Server server;

    public ServerLockedEvent(Family family, Server server) {
        super();
        this.family = family;
        this.server = server;
    }

    public Family family() {
        return family;
    }
    public Server server() {
        return server;
    }
}