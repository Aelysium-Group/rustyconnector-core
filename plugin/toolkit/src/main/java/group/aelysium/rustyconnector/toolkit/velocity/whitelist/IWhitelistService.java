package group.aelysium.rustyconnector.toolkit.velocity.whitelist;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.serviceable.interfaces.Service;

import java.util.List;
import java.util.Optional;

public abstract class IWhitelistService extends Particle {
    void proxyWhitelist();
    IWhitelist proxyWhitelist();

    /**
     * Add a whitelist to this manager.
     * @param whitelist The whitelist to add to this manager.
     */
    void add(IWhitelist whitelist);

    /**
     * Remove a whitelist from this manager.
     * @param whitelist The whitelist to remove from this manager.
     */
    void remove(IWhitelist whitelist);

    Optional<IWhitelist> find(String name);

    List<IWhitelist> dump();

    void clear();
}
