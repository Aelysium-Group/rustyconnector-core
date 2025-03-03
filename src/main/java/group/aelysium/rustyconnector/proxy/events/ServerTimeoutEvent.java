package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;

/**
 * Represents the Magic Link connection to a Server timing out.
 */
public class ServerTimeoutEvent extends Event {
    public final Family family;
    public final Server server;

    public ServerTimeoutEvent(Server server, Family f) {
        super();
        this.server = server;
        this.family = f;
    }
}