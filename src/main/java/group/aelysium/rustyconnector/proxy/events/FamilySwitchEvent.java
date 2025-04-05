package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player switching from one family to another family.
 */
public class FamilySwitchEvent extends Event {
    public final Family oldFamily;
    public final Family newFamily;
    public final Server oldServer;
    public final Server newServer;
    public final Player player;

    public FamilySwitchEvent(@NotNull Family oldFamily, @NotNull Family newFamily, @NotNull Server oldServer, @NotNull Server newServer, @NotNull Player player) {
        super();
        this.oldFamily = oldFamily;
        this.newFamily = newFamily;
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }
}