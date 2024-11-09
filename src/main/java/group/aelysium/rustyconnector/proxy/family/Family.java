package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class Family implements Player.Connectable, Server.Container, Plugin {
    protected final String id;
    protected final String displayName;
    protected final String parent;
    protected final Map<String, Flux<? extends Plugin>> plugins = new ConcurrentHashMap<>();

    protected Family(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent
    ) {
        if(id.length() > 24) throw new IllegalArgumentException("Family ID must be no longer than 24 characters. If you want a longer name for the family, use display name.");
        this.id = id;
        this.displayName = displayName;
        this.parent = parent;
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
    public @NotNull Optional<Flux<? extends Family>> parent() {
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
    public void installPlugin(Particle.Flux<? extends Plugin> flux) throws Exception {
        Plugin plugin = flux.observe(5, TimeUnit.MINUTES);
        if(this.plugins.containsKey(plugin.name().toLowerCase())) throw new RuntimeException("A plugin with the name "+plugin.name()+" already exists on the family "+this.id);
        this.plugins.put(plugin.name().toLowerCase(), flux);
    }

    /**
     * Checks if a specific plugin exists on the family.
     * @param pluginName The name of the plugin to check for.
     * @return `true` if the plugin exists. `false` otherwise.
     */
    public boolean hasPlugin(String pluginName) {
        return this.plugins.containsKey(pluginName.toLowerCase());
    }

    /**
     * Checks if a specific plugin exists on the family.
     * @param pluginName The name of the plugin to check for.
     * @return An optional containing the flux of the plugin if it exists. Otherwise, an empty optional.
     */
    public <F extends Flux<? extends Plugin>> Optional<F> fetchPlugin(String pluginName) {
        return Optional.ofNullable((F) this.plugins.get(pluginName.toLowerCase()));
    }

    /**
     * Uninstalls the specified plugin.
     * This method will attempt to shut down the plugin before uninstalling it.
     * @param pluginName The name of the plugin to uninstall.
     * @return `true` if the plugin was uninstalled. `false` if there was no plugin to uninstall.
     * @throws Exception If there was an issue uninstalling the plugin.
     */
    public boolean uninstallPlugin(String pluginName) throws Exception {
        String lowerCase = pluginName.toLowerCase();
        if(!this.plugins.containsKey(lowerCase)) return false;
        Flux<? extends Plugin> flux = this.plugins.get(lowerCase);

        if(flux.exists()) flux.close();

        this.plugins.remove(lowerCase);
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
    public @NotNull String name() {
        return this.id();
    }

    @Override
    public @NotNull String description() {
        return "Provides server categorization services.";
    }

    @Override
    public @NotNull Component details() {
        return RC.Lang("rustyconnector-familyDetails").generate(this);
    }

    @Override
    public boolean hasPlugins() {
        return true;
    }

    @Override
    public @NotNull Map<String, Flux<? extends Plugin>> plugins() {
        return Collections.unmodifiableMap(this.plugins);
    }
}
