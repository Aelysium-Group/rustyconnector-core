package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player successfully connecting to a family.
 */
public class FamilyPostJoinEvent extends Event {
    protected final Flux<? extends Family> family;
    protected final Server server;
    protected final Player player;

    public FamilyPostJoinEvent(@NotNull Flux<? extends Family> family, @NotNull Server server, @NotNull Player player) {
        super();
        this.family = family;
        this.server = server;
        this.player = player;
    }

    public Flux<? extends Family> family() {
        return family;
    }
    public Server server() {
        return server;
    }
    public Player player() {
        return player;
    }
}