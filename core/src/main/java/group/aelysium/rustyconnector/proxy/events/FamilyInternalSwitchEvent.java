package group.aelysium.rustyconnector.proxy.events;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.Event;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.player.Player;

/**
 * Represents a player switching from one MCLoader in a family to another MCLoader in that same family.
 */
public class FamilyInternalSwitchEvent implements Event {
    protected final Particle.Flux<Family> family;
    protected final Server previousServer;
    protected final Server newServer;
    protected final Player player;

    public FamilyInternalSwitchEvent(Particle.Flux<Family> family, Server previousServer, Server newServer, Player player) {
        this.family = family;
        this.previousServer = previousServer;
        this.newServer = newServer;
        this.player = player;
    }

    public Particle.Flux<Family> family() {
        return family;
    }
    public Server previousMCLoader() {
        return previousServer;
    }
    public Server newMCLoader() {
        return newServer;
    }
    public Player player() {
        return player;
    }
}