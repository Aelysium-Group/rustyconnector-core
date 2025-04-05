package group.aelysium.rustyconnector.common.haze;

import group.aelysium.haze.Database;
import group.aelysium.rustyconnector.common.modules.Module;
import org.jetbrains.annotations.NotNull;

public abstract class HazeDatabase extends Database implements Module {
    public HazeDatabase(@NotNull String name, @NotNull Type type) {
        super(name, type);
    }
}
