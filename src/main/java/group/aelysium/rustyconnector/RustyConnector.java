package group.aelysium.rustyconnector;

import group.aelysium.ara.Closure;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RustyConnector {
    private static final List<Consumer<Flux<? extends RCKernel<?>>>> onStartHandlers = new Vector<>();
    protected static final AtomicReference<Flux<? extends RCKernel<?>>> kernel = new AtomicReference<>(null);

    /**
     * Registers a consumer which will run whenever the kernel's flux is available.
     * If the kernel is already available, the consumer will run immediately.
     * Any exceptions thrown by the consumer will be ignored, make sure you handle exceptions yourself.
     * @param consumer The consumer to handle the kernel.
     */
    public static void Kernel(Consumer<Flux<? extends RCKernel<?>>> consumer) {
        onStartHandlers.add(consumer);

        if(kernel.get() == null) return;
        try {
            consumer.accept(kernel.get());
        } catch (Exception ignore) {}
    }

    /**
     * Attempts to fetch the kernel flux immediately.
     * @param <K> The type of the flux.
     * @throws NoSuchElementException If no flux has been registered.
     */
    public static <K extends RCKernel<?>> Flux<K> Kernel() throws NoSuchElementException {
        return (Flux<K>) Optional.ofNullable(kernel.get()).orElseThrow();
    }

    public static void registerAndIgnite(@NotNull Flux<? extends RCKernel<?>> flux) throws IllegalAccessError, Exception {
        Flux<? extends RCKernel<?>> kernelInstance = kernel.get();
        if (kernelInstance != null) throw new IllegalAccessError("The RustyConnector kernel has already been established.");
        kernel.set(flux);
        flux.build();
        onStartHandlers.forEach(t->{
            try {
                t.accept(flux);
            } catch (Exception ignore) {}
        });
    }

    public static void unregister() throws Exception {
        Flux<? extends RCKernel<?>> kernelInstance = kernel.get();
        if(kernelInstance == null) return;

        kernelInstance.close();
        kernel.set(null);
    }

    public static Class<?> getGenericType(Flux<ModuleParticle> flux) {
        if (!(flux.getClass().getGenericSuperclass() instanceof ParameterizedType type))
            return null;
        return (Class<?>) type.getActualTypeArguments()[0];
    }
}