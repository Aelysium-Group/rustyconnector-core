package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player switching from one server to another.
 * This event doesn't care about what family the servers are a part of.
 */
public class ServerSwitchEvent extends Event {
    final Server oldServer;
    final Server newServer;
    final Player player;

    public ServerSwitchEvent(@NotNull Server oldServer, @NotNull Server newServer, @NotNull Player player) {
        super();
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }
}