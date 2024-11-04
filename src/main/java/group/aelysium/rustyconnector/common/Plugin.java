package group.aelysium.rustyconnector.common;

import group.aelysium.ara.Particle;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides declarative access for tiered plugin structures in RustyConnector.
 */
public interface Plugin extends Particle {
    /**
     * @return The name of the plugin.
     *         This value is used as the id for this plugin and should be unique.
     */
    @NotNull String name();

    /**
     * @return A description of the plugin and what it does.
     */
    @NotNull String description();

    /**
     * @return Details for the plugins.
     *         If there are any commands that let you interface with the plugin, this is a good spot to provide those.
     */
    @NotNull Component details();

    /**
     * @return Does this plugin have children plugins?
     */
    boolean hasPlugins();

    /**
     * @return A list of the plugins that reside inside of this one.
     */
    @NotNull List<Flux<? extends Plugin>> plugins();
}
