package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player attempting to connect to a family.
 */
public class FamilyPreJoinEvent extends Event.Cancelable {
    public final Family family;
    public final Player player;
    public final Player.Connection.Power power;

    public FamilyPreJoinEvent(
            @NotNull Family family,
            @NotNull Player player,
            @NotNull Player.Connection.Power power
    ) {
        super();
        this.family = family;
        this.player = player;
        this.power = power;
    }
}