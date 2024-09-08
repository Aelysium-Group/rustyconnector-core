package group.aelysium.rustyconnector.common;

import group.aelysium.rustyconnector.common.crypt.Snowflake;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;

import java.io.Closeable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FailCapture implements Closure {
    protected final Map<Long, Instant> fails;
    protected int numberOfFails;
    protected LiquidTimestamp period;
    protected Snowflake snowflake = new Snowflake();
    protected ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Define a new fail capture.
     * @param numberOfFails The number of times that this {@link FailCapture} is allowed to be triggered within the provided period before it fails.
     * @param period The amount of time to elapse before this {@link FailCapture} is allowed to be triggered more.
     */
    public FailCapture(int numberOfFails, LiquidTimestamp period) {
        super();
        this.numberOfFails = numberOfFails;
        this.period = period;
        this.fails = Collections.synchronizedMap(new LinkedHashMap<>(numberOfFails * 2){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > (numberOfFails * 2);
            }
        });

        handleExpired();
    }

    private void handleExpired() {
        if(this.fails.isEmpty()) {
            this.executor.schedule(this::handleExpired, this.period.value() * 2L, this.period.unit());
            return;
        }

        List<Long> remove = new ArrayList<>();
        Instant now = Instant.now();
        this.fails.forEach((k, v)->{
            if(v.plus(this.period.value(), this.period.chronoUnit()).isAfter(now)) return;
            remove.add(k);
        });
        remove.forEach(this.fails::remove);

        this.executor.schedule(this::handleExpired, this.period.value(), this.period.unit());
    }

    /**
     * Triggers the fail capture, telling it to register a new failure.
     * @param message The error message to throw if this trigger fails.
     * @throws RuntimeException When the {@link FailCapture} has failed to many times and can't anymore.
     */
    public void trigger(String message) throws RuntimeException {
        if(this.fails.size() > this.numberOfFails) throw new RuntimeException(message);

        this.fails.put(snowflake.nextId(), Instant.now());

        if(this.fails.size() > this.numberOfFails) throw new RuntimeException(message);
    }

    /**
     * Returns whether the fail capture will fail or not next time {@link #trigger(String)} is called.
     * @return `true` is it will fail. `false` otherwise.
     */
    public boolean willFail() {
        return this.fails.size() > this.numberOfFails;
    }

    /**
     * Resets the fail service
     */
    public void reset() {
        this.fails.clear();
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
        this.fails.clear();
    }
}