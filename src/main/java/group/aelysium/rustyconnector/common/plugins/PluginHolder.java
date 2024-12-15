package group.aelysium.rustyconnector.common.plugins;

import group.aelysium.ara.Particle;

import java.util.Map;

/**
 * Provides access to lists of plugins.
 * This interface is specifically used by wrappers to allow the console the ability to target specific plugins.
 */
public interface PluginHolder {
    /**
     * Returns a map of plugins that this holder contains.
     * The map is immutable.
     */
    Map<String, Particle.Flux<? extends Particle>> plugins();
}
