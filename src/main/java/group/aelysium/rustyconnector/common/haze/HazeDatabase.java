package group.aelysium.rustyconnector.common.haze;

import group.aelysium.ara.Particle;
import group.aelysium.haze.Database;
import org.jetbrains.annotations.NotNull;

public abstract class HazeDatabase extends Database implements Particle {
    public HazeDatabase(@NotNull String name, @NotNull Type type) {
        super(name, type);
    }
}
