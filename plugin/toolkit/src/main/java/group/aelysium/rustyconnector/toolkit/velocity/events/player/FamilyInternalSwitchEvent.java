package group.aelysium.rustyconnector.toolkit.velocity.events.player;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.Family;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

/**
 * Represents a player switching from one MCLoader in a family to another MCLoader in that same family.
 */
public class FamilyInternalSwitchEvent implements Event {
    protected final Particle.Flux<IFamily> family;
    protected final IMCLoader previousMCLoader;
    protected final IMCLoader newMCLoader;
    protected final IPlayer player;

    public FamilyInternalSwitchEvent(Particle.Flux<IFamily> family, IMCLoader previousMCLoader, IMCLoader newMCLoader, IPlayer player) {
        this.family = family;
        this.previousMCLoader = previousMCLoader;
        this.newMCLoader = newMCLoader;
        this.player = player;
    }

    public Particle.Flux<IFamily> family() {
        return family;
    }
    public IMCLoader previousMCLoader() {
        return previousMCLoader;
    }
    public IMCLoader newMCLoader() {
        return newMCLoader;
    }
    public IPlayer player() {
        return player;
    }
}