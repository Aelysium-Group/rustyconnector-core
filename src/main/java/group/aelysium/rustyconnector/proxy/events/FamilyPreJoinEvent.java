package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player attempting to connect to a family.
 */
public class FamilyPreJoinEvent extends Event.Cancelable {
    protected Flux<? extends Family> family;
    protected Player player;
    protected Player.Connection.Power power;

    public FamilyPreJoinEvent(
            @NotNull Flux<? extends Family> family,
            @NotNull Player player,
            @NotNull Player.Connection.Power power
    ) {
        super();
        this.family = family;
        this.player = player;
        this.power = power;
    }

    public Flux<? extends Family> family() {
        return family;
    }
    public Player player() {
        return player;
    }
}