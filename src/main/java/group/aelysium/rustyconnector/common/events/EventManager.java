package group.aelysium.rustyconnector.common.events;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.load_balancing.ISortable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class EventManager implements Particle {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Class<? extends Event>, Vector<SortableListener>> listeners = new ConcurrentHashMap<>();

    protected EventManager() {}

    /**
     * Registers the provided listen.
     * If the listen method is static you can set this to be the class of your listen. (Object.class)
     * Or if you want an instance method, you can pass a listen instance. (new Object())
     * @param listener The listen to use.
     */
    public void listen(Object listener) {
        boolean isInstance = !(listener instanceof Class<?>);
        Class<?> objectClass = listener instanceof Class<?> ? (Class<?>) listener : listener.getClass();

        for (Method method : objectClass.getMethods()) {
            if(isInstance && Modifier.isStatic(method.getModifiers())) continue;
            if(!isInstance && !Modifier.isStatic(method.getModifiers())) continue;
            if(!method.isAnnotationPresent(PacketListener.class)) continue;
            EventListener annotation = method.getAnnotation((EventListener.class));
            if(annotation == null) continue;

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters[0] == null) return;
            try {
                Class<? extends Event> eventType = (Class<? extends Event>) parameters[0];
                this.listeners.computeIfAbsent(eventType, k -> new Vector<>()).add(new SortableListener(
                        annotation.order().getSlot(),
                        annotation.ignoreCanceled(),
                        event -> {
                            try {
                                try {
                                    method.invoke(isInstance ? listener : null, event);
                                } catch (IllegalArgumentException ignore) {
                                    method.invoke(isInstance ? listener : null);
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                RC.Error(Error.from(e));
                            }
                        }
                ));
            } catch (Exception e) {
                RC.Error(Error.from(e));
            }
        }
        this.listeners.forEach((k, v) -> QuickSort.sort(v));
    }

    /**
     * Fires the specified event.
     * @param event The event to fire.
     */
    public void fireEvent(Event event) {
        List<SortableListener> listeners = this.listeners.get(event.getClass());
        if (listeners == null) return;

        executor.execute(() -> listeners.forEach(listener -> {
            try {
                listener.consumer().accept(event);
            } catch (Exception e) {
                RC.Error(Error.from(e));
            }
        }));
    }

    /**
     * Fires the specified event.
     * @param event The event to fire.
     * @return A completable future which resolves to whether or not the event has been canceled.
     *         If `true` the caller should cancel whatever process the event was intended to signify.
     *         If `false` the caller can continue with the process.
     */
    public CompletableFuture<Boolean> fireEvent(Event.Cancelable event) {
        List<SortableListener> listeners = this.listeners.get(event.getClass());
        if (listeners == null) return CompletableFuture.completedFuture(false);
        if (listeners.isEmpty()) return CompletableFuture.completedFuture(false);

        try {
            executor.execute(() -> listeners.forEach(listener -> {
                if(event.canceled() && !listener.ignoreCanceled()) return;
                try {
                    listener.consumer().accept(event);
                } catch (Exception e) {
                    RC.Error(Error.from(e));
                }
            }));
            return CompletableFuture.completedFuture(event.canceled());
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        return CompletableFuture.completedFuture(false);
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
