package group.aelysium.rustyconnector.toolkit.core.absolute_redundancy;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Particles are the backbone of the Absolute Redundancy Architecture.
 * By leveraging {@link Flux}, Particles are able to exist in a state of super-positioning.
 */
public abstract class Particle implements AutoCloseable {
    protected Particle() {}

    /**
     * Tinder exists as an ignition point for new Particles.
     * @param <P> The Particles that will be launched via this Tinder.
     */
    public abstract static class Tinder<P extends Particle> {
        protected Tinder() {}

        public final Flux<P> flux() {
            return new Flux<>(this);
        }

        /**
         * Based on the contents of this Tinder, ignite a new Particle.
         * Only two results are acceptable, either a fully-functioning Particle is returned.
         * Or this method throws an exception.
         * @return A fully functional Particle.
         * @throws Exception If there is any issue at all constructing the microservice.
         */
        public abstract @NotNull P ignite() throws Exception;
    }

    /**
     * All microservices exist in a state of flux.
     * {@link Particle.Flux} exists to manage this state.
     * @param <P> The underlying Particle that exists within this flux.
     */
    public static class Flux<P extends Particle> implements AutoCloseable {
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private @Nullable CompletableFuture<P> resolvable = null;
        private final Particle.Tinder<P> tinder;

        protected Flux(Particle.Tinder<P> tinder) {
            this.tinder = tinder;
        }

        /**
         * Executes the consumer as soon as the Particle is ignited.
         * If the Particle is unable to ignite, this consumer is thrown away.
         * If the Particle is already ignited, this consumer will run instantly on the current thread.
         * <br/>
         * If the Particle has not yet ignited, this method will store the consumer on a parallel thread and wait for the Particle to attemp ignition.
         * This method is non-blocking.
         * @param consumer The consumer to execute with the ignited Particle.
         */
        public void optimistic(Consumer<P> consumer) {
            if(this.resolvable.isCancelled()) return;
            if(this.resolvable.isCompletedExceptionally()) return;
            if(this.exists()) {
                try {
                    consumer.accept(this.resolvable.get());
                } catch (Exception ignore) {}
                return;
            }

            executor.submit(()->{
                try {
                    consumer.accept(this.resolvable.get());
                } catch (Exception ignore) {}
            });
        }

        /**
         * Executes either the Consumer or the Runnable based on if the Particle is available or not.
         * This method is not thread-locking and will always execute either the Consumer or the Runnable instantly.
         * <br/>
         * This method respects Exceptions that may be thrown within the Consumer or Runnable.
         * Any exceptions that might be thrown will be passed along to the caller to handle.
         * @param success The consumer to execute if the Particle is available.
         * @param failed The Runnable if the Particle isn't available.
         */
        public void executeNow(Consumer<P> success, Runnable failed) {
            if(this.exists()) {
                Optional<P> p = Optional.empty();
                try {
                    p = Optional.ofNullable(this.resolvable.getNow(null));
                } catch (Exception ignore) {}

                if(p.isPresent()) {
                    success.accept(p.orElseThrow());
                    return;
                }
            }

            failed.run();
        }

        /**
         * Access the underlying Particle.
         * Particles exist in a state of super-position, there's no way to know if a microservice is currently active until you observe it.
         * This method is equivalent to calling {@link #access() .access()}{@link CompletableFuture#get() .get()}.
         * @return A Particle if it was able to ignite. If the Particle wasn't able to ignite, this method will throw an exception.
         * @throws Exception If the future completes exceptionally. i.e. the Particle failed to ignite.
         */
        public P observe() throws Exception {
            return this.access().get();
        }

        /**
         * Access the underlying Particle's CompletableFuture.
         * Particles exist in a state of super-position, there's no way to know if a microservice is currently active until you observe it.
         * @return A future that will resolve to the finished Particle if it's able to boot. If the microservice wasn't able to boot, the future will complete exceptionally.
         */
        public CompletableFuture<P> access() {
            if(this.resolvable != null)
                return this.resolvable;

            CompletableFuture<P> future = new CompletableFuture<>();

            executor.submit(() -> {
                try {
                    P microservice = tinder.ignite();
                    future.complete(microservice);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            this.resolvable = future;

            return this.resolvable;
        }

        /**
         * Checks if the Particle exists.
         * If this returns true, you should be able to instantly access the Particle.
         * @return `true` if the Particle exists. `false` otherwise.
         */
        public boolean exists() {
            if(this.resolvable == null) return false;
            return this.resolvable.isDone() && !this.resolvable.isCancelled() && !this.resolvable.isCompletedExceptionally();
        }

        /**
         * Fetches the Tinder being used by this flux.
         * @return The Tinder.
         */
        public Tinder<P> tinder() {
            return this.tinder;
        }

        public void close() throws Exception {
            if(this.resolvable == null) return;
            if(this.resolvable.isDone()) {
                this.resolvable.get().close();
                return;
            }
            this.resolvable.completeExceptionally(new InterruptedException("Particle boot was interrupted by Hypervisor closing!"));
        }

        /**
         * Capacitor is a collection of Flux.
         * As long as the keys are unique, you can store as much Flux as you want here.
         */
        public static class Capacitor implements AutoCloseable {
            private final Map<String, Flux<? extends Particle>> flux = new ConcurrentHashMap<>();

            public Capacitor() {}

            /**
             * Stores the passed tinder in the flux capacitor.
             * This method creates a Particle Flux backed by the passed tinder.
             * @param key A unique key that can reference this Flux.
             * @param tinder The tinder used to back the Flux.
             */
            public void store(String key, Particle.Tinder<? extends Particle> tinder) {
                this.flux.put(key, tinder.flux());
            }

            public Optional<Flux<? extends Particle>> fetch(String key) {
                return Optional.ofNullable(this.flux.get(key));
            }

            @Override
            public void close() throws Exception {
                this.flux.values().forEach(f -> {
                    try {
                        f.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                this.flux.clear();
            }
        }
    }
}