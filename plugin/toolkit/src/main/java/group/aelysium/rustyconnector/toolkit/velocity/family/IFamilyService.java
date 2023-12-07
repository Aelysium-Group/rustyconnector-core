package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.core.serviceable.interfaces.Service;
import group.aelysium.rustyconnector.toolkit.velocity.family.version_filter.IFamilyCategory;
import group.aelysium.rustyconnector.toolkit.velocity.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;

import java.util.List;

public interface IFamilyService<TMCLoader extends IMCLoader, TPlayer extends IPlayer, TLoadBalancer extends ILoadBalancer<TMCLoader>, TRootFamily extends IRootFamily<TMCLoader, TPlayer, TLoadBalancer>, TFamily extends IFamily<TMCLoader, TPlayer, TLoadBalancer>> extends Service {
    boolean shouldCatchDisconnectingPlayers();

    void setRootFamily(TRootFamily family);

    /**
     * Get the root family of this FamilyService.
     * If root family hasn't been set, or the family it references has been garbage collected,
     * this will return `null`.
     * @return A {@link IRootFamily} or `null`
     */
    TRootFamily rootFamily();

    /**
     * Get the number of families in this {@link IFamilyService}.
     * @return {@link Integer}
     */
    int size();

    /**
     * Add a family to this manager.
     * @param family The family to add to this manager.
     */
    void add(TFamily family);

    /**
     * Remove a family from this manager.
     * @param family The family to remove from this manager.
     */
    void remove(TFamily family);

    void add(IFamilyCategory<TPlayer> category);

    void remove(IFamilyCategory<TPlayer> category);

    /**
     * Gets a list of all families in this service.
     * @return {@link List<TFamily>}
     */
    List<TFamily> dump();
}
