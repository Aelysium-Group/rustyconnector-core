package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Closure;
import group.aelysium.ara.Flux;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ModuleCollection<P extends ModuleParticle> implements Closure, ModuleHolder<P> {
    protected final Map<String, Flux<P>> modules = new ConcurrentHashMap<>();

    /**
     * Registers a new module to the module collection
     * This method will also ignite the tinder just before officially registering it. If ignition of the flux fails, then the plugin wouldn't have been registered.
     * @param builder The module builder to register.
     * @return The newly created particle instance.
     * @throws IllegalStateException If a module with the key already exists.
     * @throws Exception If there's an issue initializing the module.
     */
    public @NotNull ModuleParticle registerModule(ModuleBuilder<? extends P> builder) throws Exception {
        return this.registerModule(builder.name, builder);
    }

    /**
     * Registers a module using the specific string as it's key.
     * This method will also ignite the tinder just before officially registering it. If ignition of the flux fails, then the plugin wouldn't have been registered.
     * @param builder The builder flux to register.
     * @return The newly created particle instance.
     * @throws IllegalArgumentException If a module with the key already exists.
     * @throws Exception If there's an issue initializing the module.
     */
    public @NotNull P registerModule(@NotNull String key, ModuleBuilder<? extends P> builder) throws Exception {
        Flux<P> flux = (Flux<P>) Flux.using(builder);
        if(this.modules.containsKey(key.toLowerCase())) throw new IllegalStateException("A module with the name "+key.toLowerCase()+" was already registered.");
        flux.metadata("name", builder.name);
        flux.metadata("description", builder.description);
        flux.build();
        P p = flux.get(1, TimeUnit.MINUTES);
        this.modules.put(key.toLowerCase(), flux);
        return p;
    }

    /**
     * Unregisters a module from the module collection.
     * This method will also attempt to {@link Flux#close} the plugin.
     * If no module exists with the provided name, nothing will happen.
     * @param name The name of the plugin to unregister.
     */
    public void unregisterModule(@NotNull String name) {
        try {
            Flux<? extends P> plugin = this.modules.get(name);
            plugin.close();
        } catch (Exception ignore) {}
    }

    /**
     * Fetches a module based on the provided name.
     * @param name The name of the module to fetch.
     * @return A particle flux if it exists, otherwise null.
     * @param <T> The type of the particle.
     */
    public <T extends P> Flux<T> fetchModule(String name) {
        return (Flux<T>) this.modules.get(name.toLowerCase());
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
    public Map<String, Flux<P>> modules() {
        return Collections.unmodifiableMap(this.modules);
    }

    public int size() {
        return this.modules.size();
    }

    public boolean isEmpty() {
        return this.modules.isEmpty();
    }

    public void forEach(BiConsumer<String, Flux<?>> consumer) {
        this.modules.forEach(consumer);
    }
}
