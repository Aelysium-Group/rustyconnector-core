package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents a Server being locked on its family.
 */
public class ServerLockedEvent extends Event.Cancelable {
    public final Family family;
    public final Server server;

    public ServerLockedEvent(Family family, Server server) {
        super();
        this.family = family;
        this.server = server;
    }
}