package group.aelysium.rustyconnector.common.plugins;

import group.aelysium.ara.Closure;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.proxy.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PluginCollection implements Closure, PluginHolder {
    protected final Map<String, Particle.Flux<?>> plugins = new ConcurrentHashMap<>();

    /**
     * Registers a new plugin to the plugin collection
     * This method does not care if the flux provided has been ignited or not, it's the callers job to ignite the flux.
     * @param plugin The plugin flux to register. The caller is expected to ignite this flux if they want it running.
     * @throws IllegalStateException If a plugin with the key already exists.
     */
    public void registerPlugin(Particle.Flux<?> plugin) throws IllegalStateException {
        String name = plugin.metadata("name");
        if(name == null) throw new IllegalArgumentException("The provided flux must have the `name`, `description`, and `details` metadata added.");
        if(this.plugins.containsKey(name)) throw new IllegalStateException("A plugin with the name "+name+" was already annotated.");
        this.plugins.put(name, plugin);
    }

    /**
     * Registers a plugin using the specific string as it's key.
     * Certain callers may want/need to change names from the plugin's root name, that's what this exists for.
     * @param plugin The plugin flux to register. The caller is expected to ignite this flux if they want it running.
     * @throws IllegalArgumentException If a plugin with the key already exists.
     */
    public void registerPlugin(@NotNull String key, Particle.Flux<?> plugin) throws IllegalArgumentException {
        if(this.plugins.containsKey(key)) throw new IllegalArgumentException("A plugin with the name "+key+" was already annotated.");
        this.plugins.put(key, plugin);
    }

    /**
     * Unregisters a plugin from the plugin collection.
     * This method will also attempt to {@link Particle.Flux#close} the plugin.
     * If no plugin exists with the provided name, nothing will happen.
     * @param name The name of the plugin to unregister.
     */
    public void unregister(@NotNull String name) {
        try {
            Particle.Flux<?> plugin = this.plugins.get(name);
            plugin.close();
        } catch (Exception ignore) {}
    }

    /**
     * Fetches a plugin based on the provided name.
     * @param name The name of the plugin to fetch.
     * @return A particle flux if it exists, otherwise null.
     * @param <T> The type of the particle.
     */
    public <T extends Particle> Particle.Flux<T> fetchPlugin(String name) {
        return (Particle.Flux<T>) this.plugins.get(name);
    }

    public boolean contains(String name) {
        return this.plugins.containsKey(name);
    }

    @Override
    public void close() throws Exception {
        this.plugins.forEach((k, v) -> {
            try {
                v.close();
            } catch (Exception ignore) {}
        });
        this.plugins.clear();
    }

    @Override
    public Map<String, Particle.Flux<? extends Particle>> plugins() {
        return Collections.unmodifiableMap(this.plugins);
    }

    public int size() {
        return this.plugins.size();
    }

    public boolean isEmpty() {
        return this.plugins.isEmpty();
    }

    public void forEach(BiConsumer<String, Particle.Flux<?>> consumer) {
        this.plugins.forEach(consumer);
    }
}
