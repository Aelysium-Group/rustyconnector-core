package group.aelysium.rustyconnector.common.events;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.load_balancing.ISortable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EventManager implements Particle {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Class<? extends Event>, Vector<SortableListener>> listeners = new ConcurrentHashMap<>();

    protected EventManager() {
    }

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
                                if(isInstance)
                                    try {
                                        method.invoke(listener, event);
                                    } catch (IllegalArgumentException ignore) {
                                        method.invoke(listener);
                                    }
                                else
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
        }
        this.listeners.forEach((k, v) -> QuickSort.sort(v));
    }

    public void fireEvent(Event event) {
        List<SortableListener> listeners = this.listeners.get(event.getClass());
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
