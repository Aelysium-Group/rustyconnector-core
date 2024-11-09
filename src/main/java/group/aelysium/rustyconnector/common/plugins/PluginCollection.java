package group.aelysium.rustyconnector.common.plugins;

import group.aelysium.ara.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;

public class PluginCollection {
    private final Vector<KeyValue<String, Particle.Flux<Plugin>>> plugins = new Vector<>();

    /**
     * Adds the specific plugin to the collection.
     * Only works if {@link group.aelysium.ara.Particle.Flux#executeNow(Consumer)} successfully executes.
     * @param plugin The plugin flux.
     */
    public void add(Particle.Flux<Plugin> plugin) {
        plugin.executeNow(p -> this.add(p.name(), plugin));
    }

    /**
     * Adds the specific plugin to the collection.
     * @param name The name to add plugin under.
     * @param plugin The plugin to add.
     */
    public void add(String name, Particle.Flux<Plugin> plugin) {
        this.plugins.add(new KeyValue<>(name, plugin));
    }

    /**
     * Adds all plugins.
     * Only works on any specific plugin if {@link group.aelysium.ara.Particle.Flux#executeNow(Consumer)} resolves successfully for that plugin.
     * @param plugins The iterable of plugins.
     */
    public void addAll(Iterable<Particle.Flux<Plugin>> plugins) {
        plugins.forEach(f -> f.executeNow(p -> this.plugins.add(new KeyValue<>(p.name(), f))));
    }

    /**
     * Fetches a list of all plugins with the specific name.
     * @param name The name to look for.
     * @return A list of plugins.
     */
    public @NotNull List<Particle.Flux<Plugin>> fetchAll(String name) {
        return this.plugins.stream().filter(e -> e.key.equalsIgnoreCase(name)).map(e->e.value).toList();
    }

    /**
     * Fetches a single plugin.
     * If multiple plugins have the same name, this method will undeterministically return one of them.
     * @param name The name to look for.
     * @return A plugin if it exists.
     */
    public Optional<Particle.Flux<Plugin>> fetch(String name) {
        return this.plugins.stream().filter(e -> e.key.equalsIgnoreCase(name)).map(e->e.value).findAny();
    }

    public record KeyValue<K, V>(K key, V value) {}
}
