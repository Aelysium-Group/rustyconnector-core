package group.aelysium.rustyconnector.toolkit.proxy.events.player;

import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;

/**
 * Represents a player switching from one family to another family.
 * Specifically, this event will fire after {@link FamilyLeaveEvent} is fired on the previous family, and after {@link FamilyPostJoinEvent} fires on the new family.
 */
public class FamilySwitchEvent implements Event {
    protected final Particle.Flux<IFamily> oldFamily;
    protected final Particle.Flux<IFamily> newFamily;
    protected final IMCLoader oldMCLoader;
    protected final IMCLoader newMCLoader;
    protected final IPlayer player;

    public FamilySwitchEvent(Particle.Flux<IFamily> oldFamily, Particle.Flux<IFamily> newFamily, IMCLoader oldMCLoader, IMCLoader newMCLoader, IPlayer player) {
        this.oldFamily = oldFamily;
        this.newFamily = newFamily;
        this.oldMCLoader = oldMCLoader;
        this.newMCLoader = newMCLoader;
        this.player = player;
    }

    public Particle.Flux<IFamily> oldFamily() {
        return oldFamily;
    }
    public Particle.Flux<IFamily> newFamily() {
        return newFamily;
    }
    public IMCLoader oldMCLoader() {
        return oldMCLoader;
    }
    public IMCLoader newMCLoader() {
        return newMCLoader;
    }
    public IPlayer player() {
        return player;
    }
}