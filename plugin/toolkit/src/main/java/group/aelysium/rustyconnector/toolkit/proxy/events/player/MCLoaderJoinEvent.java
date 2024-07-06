package group.aelysium.rustyconnector.toolkit.proxy.events.player;

import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.toolkit.proxy.player.Player;

/**
 * Represents a player joining an MCLoader.
 * This event will only fire once {@link FamilyPostJoinEvent} has fired.
 */
public class MCLoaderJoinEvent implements Event {
    protected final MCLoader mcLoader;
    protected final Player player;

    public MCLoaderJoinEvent(MCLoader mcLoader, Player player) {
        this.mcLoader = mcLoader;
        this.player = player;
    }

    public MCLoader mcLoader() {
        return mcLoader;
    }
    public Player player() {
        return player;
    }
}