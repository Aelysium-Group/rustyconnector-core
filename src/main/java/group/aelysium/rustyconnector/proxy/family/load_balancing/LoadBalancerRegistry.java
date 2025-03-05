package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class LoadBalancerRegistry implements Module {
    private final Map<String, Function<LoadBalancer.Config, Module.Builder<LoadBalancer>>> algorithms = new ConcurrentHashMap<>();
    private final Function<String, Module.Builder<LoadBalancer>> generator;
    
    public LoadBalancerRegistry(@NotNull Function<String, Module.Builder<LoadBalancer>> generator) {
        this.generator = generator;
    }
    
    public Set<String> algorithms() {
        return Collections.unmodifiableSet(algorithms.keySet());
    }
    
    /**
     * Register a new builder into the exchange.
     * @param algorithm The name of the builder to register, case-insensitive.
     * @param builder The builder. The string must be the name that the load balancer will use, and the return is the built load balancer instance.
     * @throws IllegalAccessException If you attempt to write using an algorithm that's already been registered.
     */
    public void register(String algorithm, Function<LoadBalancer.Config, Module.Builder<LoadBalancer>> builder) throws IllegalAccessException {
        if(algorithms.containsKey(algorithm.toUpperCase())) throw new IllegalAccessException("Algorithm "+algorithm.toUpperCase()+" already exists!");
        algorithms.putIfAbsent(algorithm, builder);
    }
    
    /**
     * Remove a new builder from the exchange.
     * @param algorithm The name of the builder to remove, case-insensitive.
     */
    public void unregister(String algorithm) {
        algorithms.remove(algorithm);
    }
    
    /**
     * Fetches the specific algorithm builder from the registry.
     * @return The algorithm builder if the algorithm exists. `null` otherwise.
     */
    public Function<LoadBalancer.Config, Module.Builder<LoadBalancer>> fetch(String algorithm) {
        return algorithms.get(algorithm);
    }
    
    /**
     * Clears all builders from the algorithm.
     */
    public void clear() {
        algorithms.clear();
    }

    /**
     * Generates a LoadBalancer based on the provided algorithm.
     * @param name The name of the load balancer to fetch a builder for.
     * @return A new LoadBalancer.
     * @throws NoSuchElementException If the provided algorithm doesn't exist.
     * @throws ExceptionInInitializerError If there was an exception while generating the LoadBalancer.
     */
    public Module.Builder<LoadBalancer> generate(String name) throws Exception {
        return this.generator.apply(name);
    }
    
    @Override
    public @Nullable Component details() {
        return join(
            newlines(),
            RC.Lang("rustyconnector-keyValue").generate("Available Algorithms", String.join(", ", this.algorithms.keySet()))
        );
    }
    
    @Override
    public void close() throws Exception {
        this.clear();
    }
}
