package group.aelysium.rustyconnector.common;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.plugins.PluginCollection;
import group.aelysium.rustyconnector.common.plugins.PluginHolder;
import group.aelysium.rustyconnector.proxy.util.Version;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class RCKernel<A extends RCAdapter> implements Particle, PluginHolder {
    protected final String id;
    protected final Version version;
    protected final A adapter;
    protected final PluginCollection plugins = new PluginCollection();

    protected RCKernel(
            @NotNull String id,
            @NotNull Version version,
            @NotNull A adapter,
            @NotNull List<? extends Particle.Flux<? extends Particle>> plugins
    ) {
        this.id = id;
        this.version = version;
        this.adapter = adapter;
        plugins.forEach(f -> {
            try {
                if(f.metadata("name") == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
                if(f.metadata("description") == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
                if(f.metadata("details") == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
                f.observe();
                this.plugins.registerPlugin(f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Registers a new plugin to the RustyConnector Kernel.
     * This method does not care if the flux provided has been ignited or not, it's the callers job to ignite the flux.
     * @param pluginName The name of the plugin to register.
     * @param plugin The plugin flux to register.
     * @throws IllegalStateException If a plugin already exists with the pluginName provided.
     */
    public void registerPlugin(@NotNull String pluginName, Particle.Flux<? extends RC.Plugin> plugin) throws IllegalStateException {
        this.plugins.registerPlugin(pluginName, plugin);
    }

    /**
     * Registers a new plugin to the RustyConnector Kernel.
     * This method does not care if the flux provided has been ignited or not, it's the callers job to ignite the flux.
     * @param plugin The plugin flux to register.
     * @throws IllegalStateException If a plugin already exists with the pluginName provided.
     * @throws IllegalArgumentException If the provided flux doesn't contain a "name" metadata entry.
     */
    public void registerPlugin(Particle.Flux<? extends RC.Plugin> plugin) throws IllegalStateException, IllegalArgumentException {
        String name = plugin.metadata("name");
        if(name == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        if(plugin.metadata("description") == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        if(plugin.metadata("details") == null) throw new IllegalArgumentException("Plugins must have the metadata for `name`, `description`, and `details` added to their flux before they can be registered.");
        this.registerPlugin(name, plugin);
    }

    public <T extends Particle> Particle.Flux<T> fetchPlugin(@NotNull String name) {
        return this.plugins.fetchPlugin(name);
    }

    @Override
    public Map<String, Flux<? extends Particle>> plugins() {
        return this.plugins.plugins();
    }

    /**
     * @return The id of this kernel.
     *         The id shouldn't change between re-boots.
     */
    public String id() {
        return this.id;
    }

    /**
     * @return The current version of RustyConnector
     */
    public Version version() {
        return this.version;
    }

    public A Adapter() {
        return this.adapter;
    }

    @Override
    public void close() throws Exception {
        this.plugins.close();
    }

    public static abstract class Tinder<B extends RCAdapter,T extends RCKernel<B>> extends RC.Plugin.Tinder<T> {
        protected final String id;
        protected final B adapter;
        protected RC.Plugin.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        protected RC.Plugin.Tinder<? extends ErrorRegistry> errors = ErrorRegistry.Tinder.DEFAULT_CONFIGURATION;
        protected RC.Plugin.Tinder<? extends LangLibrary> lang = LangLibrary.Tinder.DEFAULT_LANG_LIBRARY;

        public Tinder(
                @NotNull String id,
                @NotNull B adapter
        ) {
            super(
                    "Kernel",
                    "The RustyConnector Kernel",
                    "rustyconnector-kernelDetails"
            );

            this.id = id;
            this.adapter = adapter;
        }
    }
}
