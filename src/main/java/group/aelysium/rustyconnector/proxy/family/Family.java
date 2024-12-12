package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.PluginCollection;
import group.aelysium.rustyconnector.common.plugins.PluginHolder;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class Family implements Player.Connectable, Server.Container, PluginHolder, Particle {
    protected final PluginCollection plugins = new PluginCollection();
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    protected final String id;
    protected final String displayName;
    protected final String parent;

    protected Family(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent
    ) {
        if(id.length() > 16) throw new IllegalArgumentException("Family ID must be no longer than 16 characters. If you want a longer name for the family, use display name.");
        this.id = id;
        this.displayName = displayName;
        this.parent = parent;
    }

    /**
     * Stores a property in the Server.
     * @param propertyName The name of the property to store.
     * @param property The property to store.
     * @return `true` if the property could be stored. `false` if the name of the property is already in use.
     */
    public boolean property(String propertyName, Object property) {
        if(this.properties.containsKey(propertyName)) return false;
        this.properties.put(propertyName, property);
        return true;
    }

    /**
     * Fetches a property from the server.
     * @param propertyName The name of the property to fetch.
     * @return An optional containing the property, or an empty property if no property could be found.
     * @param <T> The type of the property that's being fetched.
     */
    public <T> Optional<T> property(String propertyName) {
        return Optional.ofNullable((T) this.properties.get(propertyName));
    }

    /**
     * Removes a property from the server.
     * @param propertyName The name of the property to remove.
     */
    public void dropProperty(String propertyName) {
        this.properties.remove(propertyName);
    }

    /**
     * @return A map containing all of this server's properties.
     */
    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(this.properties);
    }

    public @NotNull String id() {
        return this.id;
    }
    public @Nullable String displayName() {
        return this.displayName;
    }

    public abstract long players();

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     */
    public @NotNull Optional<Particle.Flux<? extends Family>> parent() {
        if(this.parent == null) return Optional.empty();
        try {
            return RC.P.Families().find(this.parent);
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    /**
     * Installs the provided tinder as a plugin on this family.
     * This method will immediately create a Flux from the tinder and attempt to ignite it.
     * @param flux The flux to ignite the plugin from.
     * @throws Exception If there was an issue igniting the plugin's tinder. Or if a plugin already exists with the defined name.
     */
    public void installPlugin(Particle.Flux<?> flux) throws Exception {
        flux.observe(10, TimeUnit.MINUTES);
        this.plugins.registerPlugin(flux);
    }

    /**
     * Checks if a specific plugin exists on the family.
     * @param pluginName The name of the plugin to check for.
     * @return `true` if the plugin exists. `false` otherwise.
     */
    public boolean hasPlugin(String pluginName) {
        return this.plugins.contains(pluginName);
    }

    /**
     * Checks if a specific plugin exists on the family.
     * @param pluginName The name of the plugin to check for.
     * @return An optional containing the flux of the plugin if it exists. Otherwise, an empty optional.
     */
    public <F extends Particle> Optional<Particle.Flux<F>> fetchPlugin(String pluginName) {
        return Optional.ofNullable(this.plugins.fetchPlugin(pluginName));
    }

    /**
     * Uninstalls the specified plugin.
     * This method will attempt to shut down the plugin before uninstalling it.
     * @param pluginName The name of the plugin to uninstall.
     * @return `true` if the plugin was uninstalled. `false` if there was no plugin to uninstall.
     * @throws Exception If there was an issue uninstalling the plugin.
     */
    public boolean uninstallPlugin(String pluginName) throws Exception {
        if(!this.plugins.contains(pluginName)) return false;
        this.plugins.unregister(pluginName);
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Family that = (Family) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public Map<String, Particle.Flux<?>> plugins() {
        return this.plugins.plugins();
    }
}
