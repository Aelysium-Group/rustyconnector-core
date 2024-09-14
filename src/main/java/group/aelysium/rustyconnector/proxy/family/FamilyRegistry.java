package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyRegistry implements Particle {
    private final Map<String, Flux<? extends Family>> families = new ConcurrentHashMap<>();
    private String rootFamily;

    protected FamilyRegistry() {}

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
    public Flux<? extends Family> rootFamily() {
        return this.families.get(this.rootFamily);
    }

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public Optional<Particle.Flux<? extends Family>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.get(id));
    }

    /**
     * Registers a new family.
     * @param id The id of the family to add.
     * @param family The family to add.
     */
    public void put(@NotNull String id, @NotNull Flux<? extends Family> family) {
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
     * Gets a list of all familyRegistry.
     */
    public List<Flux<? extends Family>> dump() {
        return this.families.values().stream().toList();
    }

    public void clear() {
        this.families.clear();
    }

    /**
     * Get the number of familyRegistry in this {@link FamilyRegistry}.
     * @return {@link Integer}
     */
    public int size() {
        return this.families.size();
    }

    public void close() {
        // Teardown logic for any familyRegistry that need it
        for (Particle.Flux<? extends Family> family : this.families.values()) {
            try {
                family.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.families.clear();
    }

    public static class Tinder extends Particle.Tinder<FamilyRegistry> {
        protected Map<String, Flux<? extends Family>> initialFamilies = new ConcurrentHashMap<>();
        protected String rootFamily = null;

        /**
         * Adds the family to be present on-boot
         */
        public void addFamily(Flux<? extends Family> family) {
            family.executeNow(f -> this.initialFamilies.put(f.id(), family));
        }

        public void setRootFamily(Flux<? extends Family> family) {
            family.executeNow(f -> {
                this.initialFamilies.put(f.id(), family);
                this.rootFamily = f.id();
            });
        }

        @Override
        public @NotNull FamilyRegistry ignite() throws Exception {
            FamilyRegistry familyRegistry = new FamilyRegistry();

            initialFamilies.forEach(familyRegistry::put);
            if(this.rootFamily != null) familyRegistry.setRootFamily(this.rootFamily);

            familyRegistry.dump().forEach(Flux::access);

            return familyRegistry;
        }

        /**
         * Returns the default configuration for a FamilyRegistry manager.
         * This default configuration has no root family set and no initial familyRegistry loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
