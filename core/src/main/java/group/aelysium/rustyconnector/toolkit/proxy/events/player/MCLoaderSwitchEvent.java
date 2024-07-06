package group.aelysium.rustyconnector.toolkit.proxy.events.player;

import group.aelysium.rustyconnector.toolkit.common.events.Event;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.toolkit.proxy.player.Player;

/**
 * Represents a player switching from one mcloader to another.
 * This event doesn't care about what family the mcloaders are a part of.
 */
public class MCLoaderSwitchEvent implements Event {
    protected final MCLoader oldMCLoader;
    protected final MCLoader newMCLoader;
    protected final Player player;

    public MCLoaderSwitchEvent(MCLoader oldMCLoader, MCLoader newMCLoader, Player player) {
        this.oldMCLoader = oldMCLoader;
        this.newMCLoader = newMCLoader;
        this.player = player;
    }

    public MCLoader oldMCLoader() {
        return oldMCLoader;
    }
    public MCLoader newMCLoader() {
        return newMCLoader;
    }
    public Player player() {
        return player;
    }
}