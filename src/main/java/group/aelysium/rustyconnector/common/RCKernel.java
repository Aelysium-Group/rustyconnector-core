package group.aelysium.rustyconnector.common;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.proxy.util.Version;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RCKernel<A extends RCAdapter> implements Particle {
    protected final UUID uuid;
    protected final Version version;
    protected final A adapter;
    protected final Map<String, Flux<? extends Particle>> plugins = new ConcurrentHashMap<>();

    protected RCKernel(UUID uuid, Version version, A adapter, List<? extends Flux<?>> plugins) {
        this.uuid = uuid;
        this.version = version;
        this.adapter = adapter;
        plugins.forEach(p -> {
            try {
                Particle particle = p.observe();
                this.plugins.putIfAbsent(particle.getClass().getSimpleName(), p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T extends Particle> Flux<? extends T> fetchPlugin(Class<T> key) {
        return (Flux<? extends T>) this.plugins.get(key.getSimpleName());
    }

    public Map<String, Flux<? extends Particle>> allPlugins() {
        return Map.copyOf(this.plugins);
    }

    /**
     * Gets the uuid of this kernel.
     * The uuid shouldn't change between re-boots.
     * @return {@link UUID}
     */
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * Gets the current version of RustyConnector
     * @return {@link Version}
     */
    public Version version() {
        return this.version;
    }

    public A Adapter() {
        return this.adapter;
    }

    @Override
    public void close() {
        this.plugins.forEach((k, v)->v.close());
    }
}
