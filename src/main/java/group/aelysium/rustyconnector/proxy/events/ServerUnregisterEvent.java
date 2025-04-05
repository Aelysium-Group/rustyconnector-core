package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Server unregistering from the Proxy.
 */
public class ServerUnregisterEvent extends Event {
    public final Family family;
    public final Server server;

    public ServerUnregisterEvent(@NotNull Server server, @Nullable Family family) {
        super();
        this.family = family;
        this.server = server;
    }
}