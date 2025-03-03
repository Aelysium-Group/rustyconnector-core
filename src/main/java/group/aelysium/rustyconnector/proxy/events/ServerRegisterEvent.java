package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents an attempt to register a server to the proxy.
 */
public class ServerRegisterEvent extends Event.Cancelable {
    public final Family family;
    public final Server.Configuration server;

    public ServerRegisterEvent(Family family, Server.Configuration server) {
        super();
        this.family = family;
        this.server = server;
    }
}