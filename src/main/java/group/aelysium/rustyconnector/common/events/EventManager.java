package group.aelysium.rustyconnector.common.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class EventManager implements Particle {
    private final Map<Class<? extends Event>, Vector<Consumer<Event>>> listeners = new ConcurrentHashMap<>();

    private final ExecutorService executor = ForkJoinPool.commonPool();

    // Constructor
    public EventManager() {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(""))
                        .setScanners(Scanners.MethodsAnnotated)
        );

        Set<Method> endpoints = reflections.getMethodsAnnotatedWith(EventListener.class);

        endpoints.forEach(method -> {
            EventListener annotation = method.getAnnotation((EventListener.class));
            this.listeners.computeIfAbsent(annotation.value(), k -> new Vector<>()).add(event -> {
                try {
                    try {
                        method.invoke(null, event);
                    } catch (IllegalArgumentException ignore) {
                        method.invoke(null);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    /**
     * Fires the event.
     */
    public void fireEvent(Event event) {
        // Get the listener for the event type
        Vector<Consumer<Event>> listeners = this.listeners.get(event.getClass());

        // If the listener exists, submit a task to the executor
        if (listeners == null) return;

        try {
            executor.execute(() -> listeners.forEach(listener -> listener.accept(event)));
        } catch (Exception ignore) {}
    }


    public void close() {
        this.listeners.clear();
        this.executor.shutdown();
    }

    public static class Tinder extends Particle.Tinder<EventManager> {
        @Override
        public @NotNull EventManager ignite() throws Exception {
            return new EventManager();
        }

        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
