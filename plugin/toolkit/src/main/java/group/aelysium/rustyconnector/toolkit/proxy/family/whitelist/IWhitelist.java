package group.aelysium.rustyconnector.toolkit.proxy.family.whitelist;

import group.aelysium.rustyconnector.toolkit.RustyConnector;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.proxy.IProxyFlame;
import group.aelysium.rustyconnector.toolkit.proxy.player.IPlayer;

import java.util.List;

public interface IWhitelist extends Particle {

    boolean usesPlayers();
    boolean usesPermission();
    String name();
    String message();
    boolean inverted();

    /**
     * Fetches a list of player filters.
     * @return {@link List<IWhitelistPlayerFilter>}
     */
    List<? extends IWhitelistPlayerFilter> playerFilters();

    /**
     * Validate a player against the {@link IWhitelist}.
     * @param player The {@link IPlayer} to validate.
     * @return `true` if the player is whitelisted. `false` otherwise.
     */
    boolean validate(IPlayer player);


    record Settings(
            String name,
            boolean usePlayers,
            boolean usePermission,
            String message,
            boolean strict,
            boolean inverted
    ) {}
}
