package group.aelysium.rustyconnector.common;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.proxy.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RCKernel<A extends RCAdapter> implements Plugin {
    protected final UUID uuid;
    protected final Version version;
    protected final A adapter;
    protected final Map<String, Flux<? extends Plugin>> plugins = new ConcurrentHashMap<>();

    protected RCKernel(UUID uuid, Version version, A adapter, List<? extends Flux<? extends Plugin>> plugins) {
        this.uuid = uuid;
        this.version = version;
        this.adapter = adapter;
        plugins.forEach(p -> {
            try {
                Plugin plugin = p.observe();
                this.plugins.put(plugin.name(), p);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T extends Plugin> Flux<T> fetchPlugin(String name) {
        return (Flux<T>) this.plugins.get(name);
    }
    public <T extends Plugin> Flux<T> fetchPlugin(Class<T> clazz) {
        return (Flux<T>) this.plugins.get(clazz.getSimpleName());
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

    public @NotNull String name() {
        return "Kernel";
    }

    public @NotNull String description() {
        return "The RustyConnector Kernel";
    }

    public @NotNull Component details() {
        return RC.Lang("rustyconnector-kernelDetails").generate();
    }

    public boolean hasPlugins() {
        return !this.plugins.isEmpty();
    }

    public @NotNull Map<String, Flux<? extends Plugin>> plugins() {
         return Map.copyOf(this.plugins);
    }

    @Override
    public void close() {
        this.plugins.forEach((k, v)->v.close());
    }
}
