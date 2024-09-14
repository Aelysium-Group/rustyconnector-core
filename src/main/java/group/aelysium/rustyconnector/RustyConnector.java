package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RustyConnector {
    public static class Toolkit {
        private static Particle.Flux<ServerKernel> serverKernel = null;
        private static Particle.Flux<ProxyKernel> proxyKernel = null;

        /**
         * Fetches the Server API for RustyConnector.
         * @return {@link ServerKernel}
         */
        public static Optional<Particle.Flux<ServerKernel>> Server() throws IllegalAccessError {
            return Optional.ofNullable(serverKernel);
        }

        /**
         * Fetches the Proxy API for RustyConnector.
         * @return {@link ProxyKernel}
         */
        public static Optional<Particle.Flux<ProxyKernel>> Proxy() throws IllegalAccessError {
            return Optional.ofNullable(proxyKernel);
        }

        public static ServerKernel registerAndIgnite(@NotNull ServerKernel.Tinder tinder) throws IllegalAccessError, ExecutionException, InterruptedException {
            if(serverKernel != null) throw new IllegalAccessError("The server kernel has already been established.");
            if(proxyKernel != null) throw new IllegalAccessError("A proxy kernel has already been established.");
            serverKernel = tinder.flux();
            return serverKernel.access().get();
        }

        public static ProxyKernel registerAndIgnite(@NotNull ProxyKernel.Tinder tinder) throws IllegalAccessError, ExecutionException, InterruptedException {
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