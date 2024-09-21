package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player attempting to join a Server.
 */
public class ServerPreJoinEvent extends Event.Cancelable {
    protected final Server server;
    protected final Player player;

    public ServerPreJoinEvent(@NotNull Server server, @NotNull Player player) {
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