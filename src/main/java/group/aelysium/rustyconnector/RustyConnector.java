package group.aelysium.rustyconnector;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class RustyConnector {
    private static final List<Consumer<Particle.Flux<RCKernel<?>>>> onStartHandlers = new Vector<>();
    protected static final AtomicReference<Particle.Flux<? extends RCKernel<?>>> kernel = new AtomicReference<>(null);

    /**
     * Registers a consumer which will run whenever the kernel's flux is available.
     * If the kernel is already available, the consumer will run immediately.
     * Any exceptions thrown by the consumer will be ignored, make sure you handle exceptions yourself.
     * @param consumer The consumer to handle the kernel.
     */
    public static void Kernel(Consumer<Particle.Flux<RCKernel<?>>> consumer) {
        onStartHandlers.add(consumer);
        try {
            consumer.accept((Particle.Flux<RCKernel<?>>) kernel.get());
        } catch (Exception ignore) {}
    }

    /**
     * Attempts to fetch the kernel flux immediately.
     * @param <K> The type of the flux.
     * @throws NoSuchElementException If no flux has been registered.
     */
    public static <K extends RCKernel<?>> Particle.Flux<K> Kernel() throws NoSuchElementException {
        return (Particle.Flux<K>) Optional.ofNullable(kernel.get()).orElseThrow();
    }

    public static RCKernel<? extends RCAdapter> registerAndIgnite(@NotNull Particle.Flux<? extends RCKernel<?>> flux) throws IllegalAccessError, Exception {
        Particle.Flux<? extends RCKernel<?>> kernelInstance = kernel.get();
        if (kernelInstance != null) throw new IllegalAccessError("The RustyConnector kernel has already been established.");
        kernel.set(flux);
        RCKernel<?> kernel = flux.observe();
        onStartHandlers.forEach(t->{
            try {
                t.accept((Particle.Flux<RCKernel<?>>) flux);
            } catch (Exception ignore) {}
        });
        return kernel;
    }

    public static void unregister() throws Exception {
        Particle.Flux<? extends RCKernel<?>> kernelInstance = kernel.get();
        if(kernelInstance == null) return;

        kernelInstance.close();
        kernel.set(null);
    }

    public static Class<?> getGenericType(Particle.Flux<? extends Particle> flux) {
        if (!(flux.getClass().getGenericSuperclass() instanceof ParameterizedType type))
            return null;
        return (Class<?>) type.getActualTypeArguments()[0];
    }
}