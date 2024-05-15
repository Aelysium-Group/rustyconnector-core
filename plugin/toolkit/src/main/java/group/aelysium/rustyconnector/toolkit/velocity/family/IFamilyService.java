package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.ranked_family.IRankedFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IScalarFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IStaticFamily;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class IFamilyService implements Particle {
    public abstract void setRootFamily(IRootFamily family);

    /**
     * Get the root family of this FamilyService.
     * If root family hasn't been set, or the family it references has been garbage collected,
     * this will return `null`.
     * @return A {@link IRootFamily} or `null`
     */
    public abstract IRootFamily rootFamily();

    /**
     * Get the number of families in this {@link IFamilyService}.
     * @return {@link Integer}
     */
    public abstract int size();

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public abstract Optional<Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>>> find(String id);

    /**
     * Add a family to this manager.
     * @param id The id of the family to add.
     * @param family The family to add.
     */
    public abstract void put(@NotNull String id, @NotNull Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>> family);

    /**
     * Remove a family from this manager.
     * @param id The id of the family to remove.
     */
    public abstract void remove(@NotNull String id);

    /**
     * Gets a list of all families in this service.
     */
    public abstract List<Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>>> dump();

    public record Settings(
            IRootFamily.Settings rootFamily,
            List<IScalarFamily.Settings> scalarFamilies,
            List<IStaticFamily.Settings> staticFamilies,
            List<IRankedFamily.Settings> rankedFamilies
    ) {}
}
