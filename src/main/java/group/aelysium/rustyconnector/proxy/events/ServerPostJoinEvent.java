package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.common.events.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player joining a Server.
 */
public class ServerPostJoinEvent extends Event {
    protected final Server server;
    protected final Player player;

    public ServerPostJoinEvent(@NotNull Server server, @NotNull Player player) {
        super();
        this.server = server;
        this.player = player;
    }

    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
}