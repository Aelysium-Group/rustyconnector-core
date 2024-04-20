package group.aelysium.rustyconnector.toolkit.core.absolute_redundancy;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Particles are the backbone of the Absolute Redundency Architecture.
 * A single particle is equivalent to a Particle.
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
    public static abstract class Flux<P extends Particle> implements AutoCloseable {
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private @Nullable CompletableFuture<P> resolvable = null;
        private final Particle.Tinder<P> initializer;

        public Flux(Particle.Tinder<P> initializer) {
            this.initializer = initializer;
        }

        /**
         * Access the underlying Particle.
         * Particles exist in a state of super-position, there's no way to know if a microservice is currently active until you observe it.
         * @return A future that will resolve to the finished Particle if it's able to boot. If the microservice wasn't able to boot, the future will complete exceptionally.
         */
        public CompletableFuture<P> access() {
            if(this.resolvable != null)
                return this.resolvable;

            CompletableFuture<P> future = new CompletableFuture<>();

            executor.submit(() -> {
                try {
                    P microservice = initializer.ignite();
                    future.complete(microservice);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

            this.resolvable = future;

            return this.resolvable;
        }

        /**
         * Checks if the microservice exists.
         * If this returns true, you should be able to instantly access the microservice via {@link #access()}
         * @return `true` if the microservice exists. `false` otherwise.
         */
        public boolean exists() {
            if(this.resolvable == null) return false;
            return this.resolvable.isDone() && !this.resolvable.isCancelled() && !this.resolvable.isCompletedExceptionally();
        }

        public void close() throws Exception {
            if(this.resolvable == null) return;
            if(this.resolvable.isDone()) {
                this.resolvable.get().close();
                return;
            }
            this.resolvable.completeExceptionally(new InterruptedException("Particle boot was interrupted by Hypervisor closing!"));
        }
    }
}