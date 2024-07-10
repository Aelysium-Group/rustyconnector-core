package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.mc_loader.MCLoaderFlame;
import group.aelysium.rustyconnector.proxy.ProxyFlame;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RustyConnector {
    public static class Toolkit {
        private static Particle.Flux<MCLoaderFlame> mcLoaderKernel = null;
        private static Particle.Flux<ProxyFlame> velocityKernel = null;

        /**
         * Fetches the MCLoader API for RustyConnector.
         * @return {@link MCLoaderFlame}
         */
        public static Optional<Particle.Flux<MCLoaderFlame>> MCLoader() throws IllegalAccessError {
            return Optional.ofNullable(mcLoaderKernel);
        }

        /**
         * Fetches the Proxy API for RustyConnector.
         * @return {@link ProxyFlame}
         */
        public static Optional<Particle.Flux<ProxyFlame>> Proxy() throws IllegalAccessError {
            return Optional.ofNullable(velocityKernel);
        }

        public static void registerMCLoader(@NotNull Particle.Flux<MCLoaderFlame> kernel) {
            mcLoaderKernel = kernel;
        }
        public static void registerProxy(@NotNull Particle.Flux<ProxyFlame> kernel) {
            velocityKernel = kernel;
        }

        public static void unregister() {
            mcLoaderKernel = null;
            velocityKernel = null;
        }
    }
}