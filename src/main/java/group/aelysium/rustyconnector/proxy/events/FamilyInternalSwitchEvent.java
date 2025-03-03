package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player switching from one Server in a family to another Server in that same family.
 */
public class FamilyInternalSwitchEvent extends Event {
    public final Family family;
    public final Server previousServer;
    public final Server newServer;
    public final Player player;

    public FamilyInternalSwitchEvent(@NotNull Family family, @NotNull Server previousServer, @NotNull Server newServer, @NotNull Player player) {
        super();
        this.family = family;
        this.previousServer = previousServer;
        this.newServer = newServer;
        this.player = player;
    }
}