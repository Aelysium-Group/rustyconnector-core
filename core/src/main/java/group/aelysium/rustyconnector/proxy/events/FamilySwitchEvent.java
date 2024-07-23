package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player switching from one family to another family.
 * Specifically, this event will fire after {@link FamilyLeaveEvent} is fired on the previous family, and after {@link FamilyPostJoinEvent} fires on the new family.
 */
public class FamilySwitchEvent implements Event {
    protected final Particle.Flux<Family> oldFamily;
    protected final Particle.Flux<Family> newFamily;
    protected final Server oldServer;
    protected final Server newServer;
    protected final Player player;

    public FamilySwitchEvent(Particle.Flux<Family> oldFamily, Particle.Flux<Family> newFamily, Server oldServer, Server newServer, Player player) {
        this.oldFamily = oldFamily;
        this.newFamily = newFamily;
        this.oldServer = oldServer;
        this.newServer = newServer;
        this.player = player;
    }

    public Particle.Flux<Family> oldFamily() {
        return oldFamily;
    }
    public Particle.Flux<Family> newFamily() {
        return newFamily;
    }
    public Server oldServer() {
        return oldServer;
    }
    public Server newServer() {
        return newServer;
    }
    public Player player() {
        return player;
    }
}