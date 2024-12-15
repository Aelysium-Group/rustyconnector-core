package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.plugins.PluginHolder;
import group.aelysium.rustyconnector.common.plugins.PluginTinder;
import group.aelysium.rustyconnector.proxy.events.FamilyRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.FamilyUnregisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class FamilyRegistry implements PluginHolder, Particle {
    private final Map<String, Flux<? extends Family>> families = new ConcurrentHashMap<>();
    private String rootFamily = null;

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
    public @Nullable Flux<? extends Family> rootFamily() {
        if(this.rootFamily == null) return null;
        return this.families.get(this.rootFamily);
    }

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public Optional<Flux<? extends Family>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.get(id));
    }

    /**
     * Registers a new family.
     * @param id The id of the family to add.
     * @param flux The family flux to add.
     * @throws Exception If the family was not ignited and failed to ignite.
     */
    public void register(@NotNull String id, @NotNull Flux<? extends Family> flux) throws Exception {
        Family family = flux.observe(5, TimeUnit.SECONDS);
        this.families.put(id, flux);
        try {
            RC.EventManager().fireEvent(new FamilyRegisterEvent(family));
        } catch (Exception ignore) {}
    }

    /**
     * Remove a family from this manager.
     * @param id The id of the family to remove.
     */
    public void unregister(@NotNull String id) {
        try {
            RC.EventManager().fireEvent(new FamilyUnregisterEvent(this.families.get(id).observe(3, TimeUnit.SECONDS)));
        } catch (Exception ignore) {}

        Flux<? extends Family> flux = this.families.remove(id);
        flux.close();
    }

    /**
     * Gets a list of all families.
     */
    public List<Flux<? extends Family>> fetchAll() {
        return this.families.values().stream().toList();
    }

    public void clear() {
        this.families.clear();
    }

    /**
     * Get the number of families in this {@link FamilyRegistry}.
     * @return {@link Integer}
     */
    public int size() {
        return this.families.size();
    }

    public void close() {
        // Teardown logic for any families that need it
        for (Flux<? extends Family> family : this.families.values()) {
            try {
                family.close();
            } catch (Exception e) {
                RC.Error(Error.from(e));
            }
        }

        this.families.clear();
    }

    @Override
    public Map<String, Flux<? extends Particle>> plugins() {
        return Collections.unmodifiableMap(this.families);
    }

    public static class Tinder extends PluginTinder<FamilyRegistry> {
        public Tinder() {
            super(
                "FamilyRegistry",
                "Provides indexed access to families.",
                "rustyconnector-familyRegistryDetails"
            );
        }

        @Override
        public @NotNull FamilyRegistry ignite() throws Exception {
            return new FamilyRegistry();
        }

        /**
         * Returns the default configuration for a FamilyRegistry manager.
         * This default configuration has no root family set and no initial families loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
