package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.Plugin;
import group.aelysium.rustyconnector.common.errors.Error;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class FamilyRegistry implements Plugin {
    private final Map<String, Flux<? extends Family>> families;
    private String rootFamily;

    protected FamilyRegistry(@NotNull Map<String, Flux<? extends Family>> families, @Nullable String rootFamily) {
        this.families = families;
        this.rootFamily = rootFamily;
    }

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
                RC.Error(Error.from(e));
            }
        }

        this.families.clear();
    }

    @Override
    public @NotNull String name() {
        return FamilyRegistry.class.getSimpleName();
    }

    @Override
    public @NotNull String description() {
        return "Provides indexed access to families.";
    }

    @Override
    public @NotNull Component details() {
        return RC.Lang("rustyconnector-familyRegistryDetails").generate(this);
    }

    @Override
    public boolean hasPlugins() {
        return true;
    }

    @Override
    public @NotNull List<Flux<? extends Plugin>> plugins() {
        return List.copyOf(this.families.values());
    }

    public static class Tinder extends Particle.Tinder<FamilyRegistry> {
        protected Map<String, Flux<? extends Family>> initialFamilies = new ConcurrentHashMap<>();
        protected String rootFamily = null;

        /**
         * Adds the family to be present on-boot
         */
        public void addFamily(String id, Flux<? extends Family> family) {
            this.initialFamilies.put(id, family);
        }

        public void setRootFamily(String id, Flux<? extends Family> family) {
            this.initialFamilies.put(id, family);
            this.rootFamily = id;
        }

        @Override
        public @NotNull FamilyRegistry ignite() throws Exception {
            return new FamilyRegistry(this.initialFamilies, this.rootFamily);
        }

        /**
         * Returns the default configuration for a FamilyRegistry manager.
         * This default configuration has no root family set and no initial familyRegistry loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
