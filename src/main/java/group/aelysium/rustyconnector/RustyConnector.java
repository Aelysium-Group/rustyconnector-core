package group.aelysium.rustyconnector;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RustyConnector {
    public static class Toolkit {
        private static Particle.Flux<? extends RCKernel<?>> kernel = null;

        /**
         * Fetches the Server API for RustyConnector.
         * @return {@link ServerKernel}
         */
        public static Optional<Particle.Flux<? extends ServerKernel>> Server() {
            if(kernel == null) return Optional.empty();
            try {
                return Optional.of((Particle.Flux<? extends ServerKernel>) kernel);
            } catch (ClassCastException e) {
                throw new NoSuchElementException("There is no registered RustyConnector Server Kernel.");
            }
        }

        /**
         * Fetches the Proxy API for RustyConnector.
         * @return {@link ProxyKernel}
         */
        public static Optional<Particle.Flux<? extends ProxyKernel>> Proxy() throws IllegalAccessError {
            if(kernel == null) return Optional.empty();
            try {
                return Optional.of((Particle.Flux<? extends ProxyKernel>) kernel);
            } catch (ClassCastException e) {
                throw new NoSuchElementException("There is no registered RustyConnector Proxy Kernel.");
            }
        }

        public static RCKernel<? extends RCAdapter> registerAndIgnite(@NotNull Particle.Flux<? extends RCKernel<?>> flux) throws IllegalAccessError, Exception {
            if (kernel != null) throw new IllegalAccessError("The RustyConnector kernel has already been established.");
            kernel = flux;
            return flux.observe();
        }

        public static void unregister() throws Exception {
            if(kernel == null) return;

            kernel.close();
            kernel = null;
        }

        public static Class<?> getGenericType(Particle.Flux<? extends Particle> flux) {
            if (!(flux.getClass().getGenericSuperclass() instanceof ParameterizedType type))
                return null;
            return (Class<?>) type.getActualTypeArguments()[0];
        }
    }
}