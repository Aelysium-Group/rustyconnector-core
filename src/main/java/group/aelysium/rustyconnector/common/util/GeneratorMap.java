package group.aelysium.rustyconnector.common.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class GeneratorMap<K, V> {
    private final Map<String, Function<K, V>> generators = new ConcurrentHashMap<>();

    /**
     * Register a new builder into the exchange.
     * @param name The name of the generator to register, case-insensitive.
     * @param generator The generator accepts a incoming value, like a config record, and returns a configured instance of the resource.
     * @throws IllegalAccessException If you attempt to register a generator with a name that's already being used.
     */
    public void register(String name, Function<K, V> generator) {
        if(this.generators.containsKey(name.toLowerCase())) throw new UnsupportedOperationException("Algorithm "+name.toUpperCase()+" already exists!");
        this.generators.putIfAbsent(name, generator);
    }

    /**
     * Remove a generator from the map.
     * @param name The name of the generator to remove, case-insensitive.
     */
    public void unregister(String name) {
        this.generators.remove(name.toLowerCase());
    }

    /**
     * Fetches the specific generator from the registry.
     * @return The generator if it exists. `null` otherwise.
     */
    public Function<K, V> fetch(String name) {
        return this.generators.get(name);
    }
}
