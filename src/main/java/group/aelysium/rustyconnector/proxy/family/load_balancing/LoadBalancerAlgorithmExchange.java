package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A centralized location for registering and accessing any active Load Balancer algorithms.
 * This system allows for any familyRegistry to take advantage of load balancers added by modules.
 */
public class LoadBalancerAlgorithmExchange {
    private static final Map<String, Function<LoadBalancer.Settings, Particle.Tinder<LoadBalancer>>> algorithms = new ConcurrentHashMap<>();

    /**
     * Register a new algorithm into the exchange.
     * @param algorithm The name of the algorithm to register, case-insensitive.
     * @param initializer The initializer. The provided Object parameter should contain settings pertaining to generating a Tinder for the specified LoadBalancer.
     * @throws IllegalAccessException If you attempt to write using an algorithm that's already been registered.
     */
    public static void registerAlgorithm(String algorithm, Function<LoadBalancer.Settings, Particle.Tinder<LoadBalancer>> initializer) throws IllegalAccessException {
        if(algorithms.containsKey(algorithm.toUpperCase())) throw new IllegalAccessException("Algorithm "+algorithm.toUpperCase()+" already exists!");
        algorithms.putIfAbsent(algorithm, initializer);
    }

    /**
     * Generates a Load Balancer Tinder based on the provided algorithm.
     * @param algorithm The name of the algorithm to use, case-insensitive.
     * @param initializer The object containing the settings needed to initialize the Tinder. Check the documentation of the algorithm you're using to see specifications on what they expect.
     * @return A new Load Balancer Tinder.
     * @throws NoSuchElementException If the provided algorithm doesn't exist.
     * @throws ExceptionInInitializerError If there was an exception while generating the Tinder.
     */
    public static Particle.Tinder<LoadBalancer> generateTinder(String algorithm, LoadBalancer.Settings initializer) throws NoSuchElementException, ExceptionInInitializerError {
        if(!algorithms.containsKey(algorithm.toUpperCase())) throw new NoSuchElementException("Algorithm "+algorithm.toUpperCase()+" doesn't exist!");
        try {
            return algorithms.get(algorithm).apply(initializer);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
