package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player attempting to connect to a family.
 */
public class FamilyPreJoinEvent implements Event {
    protected Particle.Flux<? extends Family> family;
    protected Player player;

    public FamilyPreJoinEvent(Particle.Flux<? extends Family> family, Player player) {
        this.family = family;
        this.player = player;
    }

    public Particle.Flux<? extends Family> family() {
        return family;
    }
    public Player player() {
        return player;
    }
}