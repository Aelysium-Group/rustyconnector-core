package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player attempting to join a Server.
 */
public class ServerPreJoinEvent extends Event.Cancelable {
    public final Server server;
    public final Player player;
    public final Player.Connection.Power power;

    public ServerPreJoinEvent(
            @NotNull Server server,
            @NotNull Player player,
            @NotNull Player.Connection.Power power
    ) {
        super();
        this.server = server;
        this.player = player;
        this.power = power;
    }
}