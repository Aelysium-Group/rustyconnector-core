package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Closure;
import group.aelysium.ara.Particle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ModuleCollection implements Closure, ModuleHolder {
    protected final Map<String, Particle.Flux<? extends ModuleParticle>> modules = new ConcurrentHashMap<>();

    /**
     * Registers a new module to the module collection
     * This method will also ignite the tinder just before officially registering it. If ignition of the flux fails, then the plugin wouldn't have been registered.
     * @param module The module tinder to register.
     * @return The newly created particle instance.
     * @throws IllegalStateException If a module with the key already exists.
     * @throws Exception If there's an issue initializing the module.
     */
    public @NotNull ModuleParticle registerModule(ModuleTinder<?> module) throws Exception {
        Particle.Flux<?> flux = module.flux();
        String name = flux.metadata("name");
        if(name == null) throw new IllegalArgumentException("Modules must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        return this.registerModule(name, module);
    }

    /**
     * Registers a module using the specific string as it's key.
     * This method will also ignite the tinder just before officially registering it. If ignition of the flux fails, then the plugin wouldn't have been registered.
     * @param module The module flux to register.
     * @return The newly created particle instance.
     * @throws IllegalArgumentException If a module with the key already exists.
     * @throws Exception If there's an issue initializing the module.
     */
    public @NotNull ModuleParticle registerModule(@NotNull String key, ModuleTinder<?> module) throws Exception {
        Particle.Flux<? extends ModuleParticle> flux = module.flux();
        if(flux.metadata("name") == null) throw new IllegalArgumentException("Modules must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        if(flux.metadata("description") == null) throw new IllegalArgumentException("Modules must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        if(flux.metadata("details") == null) throw new IllegalArgumentException("Modules must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        if(this.modules.containsKey(key.toLowerCase())) throw new IllegalStateException("A module with the name "+key.toLowerCase()+" was already annotated.");
        ModuleParticle p = flux.observe(1, TimeUnit.MINUTES);
        this.modules.put(key.toLowerCase(), flux);
        return p;
    }

    /**
     * Unregisters a module from the module collection.
     * This method will also attempt to {@link Particle.Flux#close} the plugin.
     * If no module exists with the provided name, nothing will happen.
     * @param name The name of the plugin to unregister.
     */
    public void unregisterModule(@NotNull String name) {
        try {
            Particle.Flux<?> plugin = this.modules.get(name);
            plugin.close();
        } catch (Exception ignore) {}
    }

    /**
     * Fetches a module based on the provided name.
     * @param name The name of the module to fetch.
     * @return A particle flux if it exists, otherwise null.
     * @param <T> The type of the particle.
     */
    public @Nullable <T extends ModuleParticle> Particle.Flux<T> fetchModule(String name) {
        return (Particle.Flux<T>) this.modules.get(name.toLowerCase());
    }

    public boolean containsModule(String name) {
        return this.modules.containsKey(name.toLowerCase());
    }

    @Override
    public void close() throws Exception {
        this.modules.forEach((k, v) -> {
            try {
                v.close();
            } catch (Exception ignore) {}
        });
        this.modules.clear();
    }

    @Override
    public Map<String, Particle.Flux<? extends ModuleParticle>> modules() {
        return Collections.unmodifiableMap(this.modules);
    }

    public int size() {
        return this.modules.size();
    }

    public boolean isEmpty() {
        return this.modules.isEmpty();
    }

    public void forEach(BiConsumer<String, Particle.Flux<?>> consumer) {
        this.modules.forEach(consumer);
    }
}
