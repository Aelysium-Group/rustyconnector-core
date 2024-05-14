package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;

import java.util.Optional;

public interface IFamilyConnector<MCLoader extends IMCLoader> extends IPlayerConnectable, AutoCloseable {
    void add(MCLoader mcloader);

    void remove(MCLoader mcloader);

    /**
     * Gets the whitelist flux used for this connector.
     * If there's no whitelist for this family connector, this will return an empty optional.
     */
    Optional<Particle.Flux<IWhitelist>> whitelist();
}
