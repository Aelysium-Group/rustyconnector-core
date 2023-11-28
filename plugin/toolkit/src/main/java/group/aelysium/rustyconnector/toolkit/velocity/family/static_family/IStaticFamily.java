package group.aelysium.rustyconnector.toolkit.velocity.family.static_family;

import com.velocitypowered.api.proxy.Player;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.UnavailableProtocol;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;

public interface IStaticFamily<TMCLoader extends IMCLoader, TRustyPlayer extends IPlayer> extends IFamily<TMCLoader, TRustyPlayer> {
    /**
     * Gets the {@link UnavailableProtocol} for this family. {@link UnavailableProtocol} governs what happens when a player's resident server is unavailable.
     * @return {@link UnavailableProtocol}
     */
    UnavailableProtocol unavailableProtocol();

    /**
     * Gets the {@link LiquidTimestamp resident server expiration} for this family.
     * Server expiration governs how long a server counts as a {@link Player player's} residence before it no longer counts for that player.
     * You can set this option to save resources on your network.
     * For example, you can set an expiration of 30 days, after that time if a player doesn't join this family, their record will be removed.
     * @return {@link LiquidTimestamp}
     */
    LiquidTimestamp homeServerExpiration();

    /**
     * Gets the {@link IResidenceDataEnclave} for this {@link IStaticFamily}.
     * Data enclave gives you an interface between this family and the remote storage connector that this family uses.
     * @return {@link IResidenceDataEnclave}
     */
    IResidenceDataEnclave dataEnclave();
}