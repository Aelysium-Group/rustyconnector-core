package group.aelysium.rustyconnector.common.haze;

import group.aelysium.ara.Particle;
import group.aelysium.haze.Database;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import org.jetbrains.annotations.NotNull;

public abstract class HazeDatabase extends Database implements Particle {
    public HazeDatabase(@NotNull String name, @NotNull Type type) {
        super(name, type);
    }

    public static abstract class Tinder extends ModuleTinder<HazeDatabase> {
        public Tinder(String name, String description, String details) {
            super(name, description, details);
        }
    }
}
