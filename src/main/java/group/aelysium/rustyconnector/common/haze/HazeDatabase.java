package group.aelysium.rustyconnector.common.haze;

import group.aelysium.haze.Database;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import org.jetbrains.annotations.NotNull;

public abstract class HazeDatabase extends Database implements ModuleParticle {
    public HazeDatabase(@NotNull String name, @NotNull Type type) {
        super(name, type);
    }
}
