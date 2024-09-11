package group.aelysium.rustyconnector.common.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.proxy.family.load_balancing.ISortable;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EventManager implements Particle {
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Class<? extends Event>, Vector<SortableListener>> listeners = new ConcurrentHashMap<>();

    protected EventManager() {
        System.out.println("constructed!");
    }

    /**
     * Register all listeners within your Module.
     * @param packageName The package name that the EventManager should scan for listeners.
     */
    public void registerModule(String packageName) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage(packageName))
                        .setScanners(Scanners.MethodsAnnotated)
        );

        Set<Method> endpoints = reflections.getMethodsAnnotatedWith(EventListener.class);

        endpoints.forEach(method -> {
            EventListener annotation = method.getAnnotation((EventListener.class));
            Class<?>[] parameters = method.getParameterTypes();
            if(parameters[0] == null) return;
            try {
                Class<? extends Event> eventType = (Class<? extends Event>) parameters[0];
                this.listeners.computeIfAbsent(eventType, k -> new Vector<>()).add(new SortableListener(
                        annotation.order().getSlot(),
                        annotation.ignoreCanceled(),
                        event -> {
                            try {
                                try {
                                    method.invoke(null, event);
                                } catch (IllegalArgumentException ignore) {
                                    method.invoke(null);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        this.listeners.forEach((k, v) -> QuickSort.sort(v));
    }

    /**
     * Fires the event.
     */
    public void fireEvent(Event event) {
        // Get the listener for the event type
        List<SortableListener> listeners = this.listeners.get(event.getClass());

        // If the listener exists, submit a task to the executor
        if (listeners == null) return;

        try {
            executor.execute(() -> listeners.forEach(listener -> {
                if(event.canceled() && !listener.ignoreCanceled()) return;
                try {
                    listener.consumer().accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        } catch (Exception e) {e.printStackTrace();}
    }


    public void close() {
        this.listeners.clear();
        //this.executor.shutdown();
    }

    public static class Tinder extends Particle.Tinder<EventManager> {
        @Override
        public @NotNull EventManager ignite() throws Exception {
            System.out.println("ignition!");
            return new EventManager();
        }

        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }

    protected static class SortableListener implements ISortable {
        private final int index;
        private final boolean ignoreCanceled;
        private final Consumer<Event> consumer;

        public SortableListener(int index, boolean ignoreCanceled, Consumer<Event> consumer) {
            this.index = index;
            this.ignoreCanceled = ignoreCanceled;
            this.consumer = consumer;
        }

        public Consumer<Event> consumer() {
            return this.consumer;
        }

        public boolean ignoreCanceled() {
            return this.ignoreCanceled;
        }

        @Override
        public double sortIndex() {
            return this.index;
        }

        @Override
        public int weight() {
            return 0;
        }
    }
}
