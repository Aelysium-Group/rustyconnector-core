package group.aelysium;

import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import org.jetbrains.annotations.NotNull;

public interface ProxyAdapter<Player> {
    /**
     * Converts the RustyConnector player object to the Proxy's version.
     * @param player The RustyConnector player.
     * @return The Proxy's version of the player object.
     */
    @NotNull Player convertToProxyPlayer(@NotNull IPlayer player);
    /**
     * Converts the Proxy player to RustyConnector's version of the player.
     * @param player The Proxy player.
     * @return The RustyConnector version of the player object.
     */
    @NotNull IPlayer convertToRCPlayer(@NotNull Player player);

    /**
     * Registers the MCLoader to the Proxy.
     */
    void registerMCLoader(@NotNull IMCLoader mcloader);

    /**
     * Unregisters the MCLoader from the Proxy.
     */
    void unregisterMCLoader(@NotNull IMCLoader mcloader);
}
