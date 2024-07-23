package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.mc_loader.ServerFlame;
import group.aelysium.rustyconnector.proxy.ProxyFlame;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RustyConnector {
    public static class Toolkit {
        private static Particle.Flux<ServerFlame> serverKernel = null;
        private static Particle.Flux<ProxyFlame> velocityKernel = null;

        /**
         * Fetches the Server API for RustyConnector.
         * @return {@link ServerFlame}
         */
        public static Optional<Particle.Flux<ServerFlame>> Server() throws IllegalAccessError {
            return Optional.ofNullable(serverKernel);
        }

        /**
         * Fetches the Proxy API for RustyConnector.
         * @return {@link ProxyFlame}
         */
        public static Optional<Particle.Flux<ProxyFlame>> Proxy() throws IllegalAccessError {
            return Optional.ofNullable(velocityKernel);
        }

        public static void registerServerKernel(@NotNull Particle.Flux<ServerFlame> kernel) {
            serverKernel = kernel;
        }
        public static void registerProxyKernel(@NotNull Particle.Flux<ProxyFlame> kernel) {
            velocityKernel = kernel;
        }

        public static void unregister() {
            serverKernel = null;
            velocityKernel = null;
        }
    }
}