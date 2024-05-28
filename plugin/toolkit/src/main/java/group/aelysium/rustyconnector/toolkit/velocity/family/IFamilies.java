package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class IFamilies implements Particle {
    public abstract void setRootFamily(Flux<IFamily> family);

    /**
     * Get the root family of this FamilyService.
     * If root family hasn't been set, or the family it references has been garbage collected,
     * this will return `null`.
     * @return A {@link IRootFamily} or `null`
     */
    public abstract Flux<IFamily> rootFamily();

    /**
     * Get the number of families in this {@link IFamilies}.
     * @return {@link Integer}
     */
    public abstract int size();

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public abstract Optional<Particle.Flux<IFamily>> find(String id);

    /**
     * Add a family to this manager.
     * @param id The id of the family to add.
     * @param family The family to add.
     */
    public abstract void put(@NotNull String id, @NotNull Particle.Flux<IFamily> family);

    /**
     * Remove a family from this manager.
     * @param id The id of the family to remove.
     */
    public abstract void remove(@NotNull String id);

    /**
     * Gets a list of all families in this service.
     */
    public abstract List<Particle.Flux<IFamily>> dump();
}
