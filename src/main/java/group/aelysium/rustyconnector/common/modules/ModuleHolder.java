package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Flux;

import java.util.Map;

/**
 * Provides access to lists of modules.
 * This interface is specifically used by wrappers to allow the console the ability to target specific modules.
 */
public interface ModuleHolder<P extends Module> {
    /**
     * Returns a map of modules that this holder contains.
     * The map is immutable.
     */
    Map<String, Flux<P>> modules();
}
