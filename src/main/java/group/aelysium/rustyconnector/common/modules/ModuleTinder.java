package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Particle;

import java.util.Map;

public abstract class ModuleTinder<P extends ModuleParticle> extends Particle.Tinder<P> {
    private ModuleTinder() {
        super();
    }
    public ModuleTinder(
            String name,
            String description
    ) {
        super();
        this.metadata("name", name);
        this.metadata("description", description);
    }
}