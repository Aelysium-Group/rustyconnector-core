package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.proxy.ProxyFlame;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class RustyConnector {
    public static class Toolkit {
        private static Particle.Flux<ServerFlame> serverKernel = null;
        private static Particle.Flux<ProxyFlame> proxyKernel = null;

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
            return Optional.ofNullable(proxyKernel);
        }

        public static void registerServerKernel(@NotNull Particle.Flux<ServerFlame> kernel) throws IllegalAccessError {
            if(serverKernel != null) throw new IllegalAccessError("The server kernel has already been established.");
            if(proxyKernel != null) throw new IllegalAccessError("A proxy kernel has already been established.");
            serverKernel = kernel;
        }

        public static void registerProxyKernel(@NotNull Particle.Flux<ProxyFlame> kernel) throws IllegalAccessError {
            if(proxyKernel != null) throw new IllegalAccessError("The proxy kernel has already been established.");
            if(serverKernel != null) throw new IllegalAccessError("A server kernel has already been established.");
            proxyKernel = kernel;
        }

        public static void unregister() throws Exception {
            if(serverKernel != null) {
                serverKernel.close();
                serverKernel = null;
            }
            if(proxyKernel != null) {
                proxyKernel.close();
                proxyKernel = null;
            }
        }
    }
}