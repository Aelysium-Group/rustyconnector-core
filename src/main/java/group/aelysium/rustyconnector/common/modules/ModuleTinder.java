package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Particle;

import java.util.Map;

public abstract class ModuleTinder<P extends Particle> extends Particle.Tinder<P> {
    private ModuleTinder() {
        super();
    }
    public ModuleTinder(
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