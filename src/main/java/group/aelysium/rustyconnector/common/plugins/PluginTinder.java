package group.aelysium.rustyconnector.common.plugins;

import group.aelysium.ara.Particle;

import java.util.Map;

public abstract class PluginTinder<P extends Particle> extends Particle.Tinder<P> {
    private PluginTinder() {
        super();
    }
    public PluginTinder(
            String name,
            String description,
            String details
    ) {
        super(Map.of(
                "name", name,
                "description", description,
                "details", details
        ));
    }
}