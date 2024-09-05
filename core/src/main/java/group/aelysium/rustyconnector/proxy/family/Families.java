package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Families implements Particle {
    private final Map<String, Flux<Family>> families = new ConcurrentHashMap<>();
    private String rootFamily;

    protected Families() {}

    /**
     * Sets the root family.
     * @param familyId The family's ID to be set as root.
     * @throws NoSuchElementException If no family with the id provided exists.
     */
    public void setRootFamily(@NotNull String familyId) throws NoSuchElementException {
        if(!this.families.containsKey(familyId)) throw new NoSuchElementException();
        this.rootFamily = familyId;
    }

    /**
     * Get the root family of this FamilyService.
     * If root family hasn't been set, or the family it references has been garbage collected,
     * this will return `null`.
     * @return The root family or `null`
     */
    public Flux<Family> rootFamily() {
        return this.families.get(this.rootFamily);
    }

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public Optional<Particle.Flux<Family>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.get(id));
    }

    /**
     * Registers a new family.
     * @param id The id of the family to add.
     * @param family The family to add.
     */
    public void put(@NotNull String id, @NotNull Flux<Family> family) {
        this.families.put(id, family);
    }

    /**
     * Remove a family from this manager.
     * @param id The id of the family to remove.
     */
    public void remove(@NotNull String id) {
        this.families.remove(id);
    }

    /**
     * Gets a list of all families.
     */
    public List<Flux<Family>> dump() {
        return this.families.values().stream().toList();
    }

    public void clear() {
        this.families.clear();
    }

    /**
     * Get the number of families in this {@link Families}.
     * @return {@link Integer}
     */
    public int size() {
        return this.families.size();
    }

    public void close() {
        // Teardown logic for any families that need it
        for (Particle.Flux<Family> family : this.families.values()) {
            try {
                family.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.families.clear();
    }

    public static class Tinder extends Particle.Tinder<Families> {
        protected Map<String, Flux<Family>> initialFamilies = new ConcurrentHashMap<>();
        protected String rootFamily = null;

        /**
         * Adds the family to be present on-boot
         */
        public void addFamily(Flux<Family> family) {
            family.executeNow(f -> this.initialFamilies.put(f.id(), family));
        }

        public void setRootFamily(Flux<Family> family) {
            family.executeNow(f -> {
                this.initialFamilies.put(f.id(), family);
                this.rootFamily = f.id();
            });
        }

        @Override
        public @NotNull Families ignite() throws Exception {
            Families families = new Families();

            initialFamilies.forEach(families::put);
            if(this.rootFamily != null) families.setRootFamily(this.rootFamily);

            families.dump().forEach(Flux::access);

            return families;
        }

        /**
         * Returns the default configuration for a Families manager.
         * This default configuration has no root family set and no initial families loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
