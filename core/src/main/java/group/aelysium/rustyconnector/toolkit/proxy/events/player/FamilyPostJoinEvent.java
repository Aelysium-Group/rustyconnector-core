package group.aelysium.rustyconnector.toolkit.proxy.events.player;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.Family;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.toolkit.proxy.player.Player;

/**
 * Represents a player successfully connecting to a family.
 */
public class FamilyPostJoinEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final MCLoader mcLoader;
    protected final Player player;

    public FamilyPostJoinEvent(Particle.Flux<Family> family, MCLoader mcLoader, Player player) {
        this.family = family;
        this.mcLoader = mcLoader;
        this.player = player;
    }

    public Particle.Flux<Family> family() {
        return family;
    }
    public MCLoader mcLoader() {
        return mcLoader;
    }
    public Player player() {
        return player;
    }
}