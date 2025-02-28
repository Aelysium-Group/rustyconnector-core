package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A centralized location for registering and accessing any active Load Balancer algorithms.
 * This system allows for any familyRegistry to take advantage of load balancers added by modules.
 */
public class LoadBalancerGeneratorExchange {
    private static final Map<String, Function<String, Flux<LoadBalancer>>> algorithms = new ConcurrentHashMap<>();

    /**
     * Register a new builder into the exchange.
     * @param algorithm The name of the builder to register, case-insensitive.
     * @param builder The builder. The string must be the name that the load balancer will use, and the return is the built load balancer instance.
     * @throws IllegalAccessException If you attempt to write using an algorithm that's already been registered.
     */
    public static void registerBuilder(String algorithm, Function<String, Flux<LoadBalancer>> builder) throws IllegalAccessException {
        if(algorithms.containsKey(algorithm.toUpperCase())) throw new IllegalAccessException("Algorithm "+algorithm.toUpperCase()+" already exists!");
        algorithms.putIfAbsent(algorithm, builder);
    }

    /**
     * Generates a LoadBalancer based on the provided algorithm.
     * @param algorithm The name of the algorithm to use, case-insensitive.
     * @param name The name to be given to the load balancer.
     * @return A new LoadBalancer.
     * @throws NoSuchElementException If the provided algorithm doesn't exist.
     * @throws ExceptionInInitializerError If there was an exception while generating the LoadBalancer.
     */
    public static <T extends LoadBalancer> Flux<T> generate(String algorithm, String name) throws NoSuchElementException, ExceptionInInitializerError {
        if(!algorithms.containsKey(algorithm.toUpperCase())) throw new NoSuchElementException("Algorithm "+algorithm.toUpperCase()+" doesn't exist!");
        try {
            return (Flux<T>) algorithms.get(algorithm).apply(name);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
