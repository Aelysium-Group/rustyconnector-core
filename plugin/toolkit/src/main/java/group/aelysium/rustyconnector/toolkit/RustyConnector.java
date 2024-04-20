package group.aelysium.rustyconnector.toolkit;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;

import java.util.Optional;

public class RustyConnector {
    public static class Toolkit {
        private static IMCLoaderTinder mcLoaderKernel = null;
        private static Particle.Flux<Kernel.Particle> velocityKernel = null;

        /**
         * Fetches the MCLoader API for RustyConnector.
         * @return {@link IMCLoaderTinder}
         */
        public static Optional<IMCLoaderTinder> mcLoader() throws IllegalAccessError {
            if(mcLoaderKernel == null) return Optional.empty();
            return Optional.of(mcLoaderKernel);
        }

        /**
         * Fetches the Proxy API for RustyConnector.
         * @return {@link Kernel}
         */
        public static Optional<Particle.Flux<Kernel.Particle>> proxy() throws IllegalAccessError {
            return Optional.ofNullable(velocityKernel);
        }

        public static void register(IMCLoaderTinder kernel) {
            mcLoaderKernel = kernel;
        }
        public static void register(Particle.Flux<Kernel.Particle> kernel) {
            velocityKernel = kernel;
        }

        public static void unregister() {
            mcLoaderKernel = null;
            velocityKernel = null;
        }
    }
}