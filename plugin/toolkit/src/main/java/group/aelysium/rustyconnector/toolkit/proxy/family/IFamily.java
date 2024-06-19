package group.aelysium.rustyconnector.toolkit.proxy.family;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.proxy.connection.IPlayerConnectable;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public interface IFamily extends IPlayerConnectable, Particle {
    String id();
    String displayName();

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     * @return {@link IFamily}
     */
    Optional<Particle.Flux<IFamily>> parent();

    /**
     * Returns this family's {@link IFamilyConnector}.
     * The family's connector is used to handle players when they connect or disconnect from this family.
     * @return {@link IFamilyConnector}
     */
    IFamilyConnector connector();

    /**
     * Returns the details of this family in a component which can be
     * printed to the console or sent to a player.
     */
    Component details();
}
