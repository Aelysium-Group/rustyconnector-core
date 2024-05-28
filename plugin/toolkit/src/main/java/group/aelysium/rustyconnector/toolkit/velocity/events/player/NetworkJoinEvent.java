package group.aelysium.rustyconnector.toolkit.velocity.events.player;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

/**
 * Represents a player joining the network.
 * This event fires after {@link FamilyPostJoinEvent}.
 */
public class NetworkJoinEvent implements Event {
    protected final Particle.Flux<IFamily> family;
    protected final IMCLoader mcLoader;
    protected final IPlayer player;

    public NetworkJoinEvent(Particle.Flux<IFamily> family, IMCLoader mcLoader, IPlayer player) {
        this.family = family;
        this.mcLoader = mcLoader;
        this.player = player;
    }

    public Particle.Flux<IFamily> family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
    public IPlayer player() {
        return player;
    }
}