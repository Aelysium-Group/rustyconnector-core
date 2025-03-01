package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.modules.ModuleBuilder;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A centralized location for registering and accessing any active Load Balancer algorithms.
 * This system allows for any familyRegistry to take advantage of load balancers added by modules.
 */
public class LoadBalancerGeneratorExchange {
    private static final Map<String, Function<LoadBalancer.Config, ModuleBuilder<LoadBalancer>>> algorithms = new ConcurrentHashMap<>();
    
    /**
     * Register a new builder into the exchange.
     * @param algorithm The name of the builder to register, case-insensitive.
     * @param builder The builder. The string must be the name that the load balancer will use, and the return is the built load balancer instance.
     * @throws IllegalAccessException If you attempt to write using an algorithm that's already been registered.
     */
    public static void registerBuilder(String algorithm, Function<LoadBalancer.Config, ModuleBuilder<LoadBalancer>> builder) throws IllegalAccessException {
        if(algorithms.containsKey(algorithm.toUpperCase())) throw new IllegalAccessException("Algorithm "+algorithm.toUpperCase()+" already exists!");
        algorithms.putIfAbsent(algorithm, builder);
    }
    
    /**
     * Remove a new builder from the exchange.
     * @param algorithm The name of the builder to remove, case-insensitive.
     */
    public static void removeBuilder(String algorithm) {
        algorithms.remove(algorithm);
    }
    
    /**
     * Clears all builders from the algorithm.
     */
    public static void clear() {
        algorithms.clear();
    }

    /**
     * Generates a LoadBalancer based on the provided algorithm.
     * @param algorithm The name of the algorithm to use, case-insensitive.
     * @param config The config to be given to the load balancer.
     * @return A new LoadBalancer.
     * @throws NoSuchElementException If the provided algorithm doesn't exist.
     * @throws ExceptionInInitializerError If there was an exception while generating the LoadBalancer.
     */
    public static ModuleBuilder<LoadBalancer> generate(String algorithm, LoadBalancer.Config config) throws NoSuchElementException, ExceptionInInitializerError {
        if(!algorithms.containsKey(algorithm.toUpperCase())) throw new NoSuchElementException("Algorithm "+algorithm.toUpperCase()+" doesn't exist!");
        try {
            return algorithms.get(algorithm).apply(config);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
