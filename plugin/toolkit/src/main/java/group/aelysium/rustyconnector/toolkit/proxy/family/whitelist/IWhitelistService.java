package group.aelysium.rustyconnector.toolkit.proxy.family.whitelist;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.serviceable.interfaces.Service;

import java.util.List;
import java.util.Optional;

public abstract class IWhitelistService extends Particle {
    public abstract void proxyWhitelist(IWhitelist whitelist);
    public abstract Optional<IWhitelist> proxyWhitelist();

    /**
     * Add a whitelist to this manager.
     * @param whitelist The whitelist to add to this manager.
     */
    public abstract void add(IWhitelist whitelist);

    /**
     * Remove a whitelist from this manager.
     * @param whitelist The whitelist to remove from this manager.
     */
    public abstract void remove(IWhitelist whitelist);

    public abstract Optional<IWhitelist> find(String name);

    public abstract List<IWhitelist> dump();

    public abstract void clear();
}
