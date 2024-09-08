package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.proxy.ProxyFlame;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

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

        public static ServerFlame registerAndIgnite(@NotNull ServerFlame.Tinder tinder) throws IllegalAccessError, ExecutionException, InterruptedException {
            if(serverKernel != null) throw new IllegalAccessError("The server kernel has already been established.");
            if(proxyKernel != null) throw new IllegalAccessError("A proxy kernel has already been established.");
            serverKernel = tinder.flux();
            return serverKernel.access().get();
        }

        public static ProxyFlame registerAndIgnite(@NotNull ProxyFlame.Tinder tinder) throws IllegalAccessError, ExecutionException, InterruptedException {
            if(proxyKernel != null) throw new IllegalAccessError("The proxy kernel has already been established.");
            if(serverKernel != null) throw new IllegalAccessError("A server kernel has already been established.");
            proxyKernel = tinder.flux();
            return proxyKernel.access().get();
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