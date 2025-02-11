package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Particle;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface ModuleParticle extends Particle {
    /**
     * Returns a {@link Component} which describes the internal details of this module.
     * If there's no details to show, can just return null.
     * @return A Component describing additional details about the module.
     */
    @Nullable Component details();
}
