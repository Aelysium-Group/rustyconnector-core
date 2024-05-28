package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface IFamilyConnector extends IPlayerConnectable, AutoCloseable {
    @NotNull IMCLoader register(IMCLoader.Unregistered mcloader);
    void unregister(IMCLoader mcloader);
    void lock(IMCLoader mcloader);
    void unlock(IMCLoader mcloader);
    List<IMCLoader> mcloaders();
    long players();

    /**
     * Gets the whitelist flux used for this connector.
     * If there's no whitelist for this family connector, this will return an empty optional.
     */
    Optional<Particle.Flux<IWhitelist>> whitelist();
}
