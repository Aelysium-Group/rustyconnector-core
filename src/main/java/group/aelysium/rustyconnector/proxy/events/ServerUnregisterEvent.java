package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents an Server unregistering from the Proxy.
 */
public class ServerUnregisterEvent extends Event {
    protected final Family family;
    protected final Server server;

    public ServerUnregisterEvent(@NotNull Server server, @Nullable Family family) {
        super();
        this.family = family;
        this.server = server;
    }

    public @Nullable Family family() {
        return family;
    }
    public @NotNull Server server() {
        return server;
    }
}