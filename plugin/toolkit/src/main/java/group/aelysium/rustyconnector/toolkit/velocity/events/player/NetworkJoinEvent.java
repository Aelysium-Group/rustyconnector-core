package group.aelysium.rustyconnector.toolkit.velocity.events.player;

import group.aelysium.rustyconnector.toolkit.core.events.Event;
import group.aelysium.rustyconnector.toolkit.velocity.family.Family;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;

/**
 * Represents a player joining the network.
 * This event fires after {@link FamilyPostJoinEvent}.
 */
public class NetworkJoinEvent implements Event {
    protected final Family family;
    protected final IMCLoader mcLoader;
    protected final IPlayer player;

    public NetworkJoinEvent(Family family, IMCLoader mcLoader, IPlayer player) {
        this.family = family;
        this.mcLoader = mcLoader;
        this.player = player;
    }

    public Family family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
    public IPlayer player() {
        return player;
    }
}