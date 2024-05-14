package group.aelysium.rustyconnector.toolkit.velocity.family;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;

import java.util.List;
import java.util.Optional;

public abstract class IFamilyService extends Particle {
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
     * An alternate route of getting a family, other than "tinder.services().family().find()", can be to use {@link Family.Reference new Family.Reference(id)}{@link Family.Reference#get() .get()}.
     * @param id The id to search for.
     * @return {@link Optional< Family >}
     */
    public abstract Optional<Family> find(String id);

    /**
     * Add a family to this manager.
     * @param family The family to add to this manager.
     */
    public abstract void add(Family family);

    /**
     * Remove a family from this manager.
     * @param family The family to remove from this manager.
     */
    public abstract void remove(Family family);

    /**
     * Gets a list of all families in this service.
     * @return {@link List< Family >}
     */
    public abstract List<Family> dump();
}
