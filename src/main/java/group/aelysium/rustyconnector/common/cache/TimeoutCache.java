package group.aelysium.rustyconnector.common.cache;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import group.aelysium.ara.Closure;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;

public class TimeoutCache<K, V> implements Closure, Map<K, V> {
    private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor();
    private final Map<K, TimedValue<V>> map = new ConcurrentHashMap<>();
    private final List<Consumer<V>> onTimeout = new Vector<>();
    private final LiquidTimestamp expiration;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public TimeoutCache(LiquidTimestamp expiration) {
        this.expiration = expiration;
        this.clock.execute(this::evaluateThenRunAgain);
    }

    public LiquidTimestamp expiration() {
        return this.expiration;
    }

    private void evaluateThenRunAgain() {
        if(shutdown.get()) return;
        
        long now = Instant.now().getEpochSecond();
        this.map.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().expiration() < now;
            if(shouldRemove) this.onTimeout.forEach(c -> c.accept(entry.getValue().value()));
            return shouldRemove;
        });

        if(shutdown.get()) return;
        this.clock.schedule(this::evaluateThenRunAgain, this.expiration.value(), this.expiration.unit());
    }

    public void onTimeout(Consumer<V> consumer) {
        this.onTimeout.add(consumer);
    }

    @Override
    public void close() {
        this.shutdown.set(true);
        this.onTimeout.clear();
        this.map.clear();
        this.clock.shutdownNow();
    }

    @Override
    public V put(K key, V value) {
        this.map.put(key, new TimedValue<>(value, this.expiration));
        return value;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(new TimedValue<>(value, LiquidTimestamp.from(0, TimeUnit.SECONDS)));
    }

    @Override
    public V get(Object key) {
        TimedValue<V> timedValue = this.map.get(key);
        return timedValue == null ? null : timedValue.value();
    }

    @Override
    public V remove(Object key) {
        TimedValue<V> timedValue = this.map.remove(key);
        return timedValue == null ? null : timedValue.value();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach((k, v) -> this.map.put(k, new TimedValue<>(v, this.expiration)));
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.map.values().stream().map(TimedValue::value).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.map.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().value()))
                .collect(Collectors.toSet());
    }
    
    /**
     * Refreshes the timeout of the specified key, if it exists, to once again equal {@link #expiration()}.
     * @param key The key to refresh if it exists.
     * @return The value associated with the key if the key exists in the cache. Otherwise, returns null.
     */
    public V refresh(K key) {
        V value = this.get(key);
        if(value == null) return null;
        
        this.put(key, value);
        return value;
    }

    protected static class TimedValue<V> {
        private final V value;
        private final long expiration;

        public TimedValue(V value, LiquidTimestamp expireAfter) {
            this.value = value;
            this.expiration = expireAfter.epochFromNow();
        }

        public V value() {
            return value;
        }

        public long expiration() {
            return expiration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TimedValue<?> that = (TimedValue<?>) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}