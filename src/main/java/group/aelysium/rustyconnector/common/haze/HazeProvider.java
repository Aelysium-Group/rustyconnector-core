package group.aelysium.rustyconnector.common.haze;

import group.aelysium.haze.Database;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HazeProvider implements Particle, ModuleHolder {
    private final ModuleCollection databases = new ModuleCollection();

    public @NotNull Optional<Flux<? extends Database>> fetchDatabase(@NotNull String name) {
        return Optional.ofNullable(this.databases.fetchModule(name));
    }
    public void removeDatabase(@NotNull String name) {
        this.databases.unregisterModule(name);
    }
    public void registerDatabase(@NotNull HazeDatabase.Tinder database) throws Exception {
        this.databases.registerModule(database);
    }
    public boolean containsDatabase(@NotNull String name) {
        return this.databases.containsModule(name);
    }

    @Override
    public void close() throws Exception {
        this.databases.close();
    }

    @Override
    public Map<String, Flux<? extends Particle>> modules() {
        return this.databases.modules();
    }

    public static abstract class Tinder extends ModuleTinder<HazeProvider> {
        public Tinder() {
            super(
                "Haze",
                "Provides abstracted database connections.",
                "rustyconnector-hazeDetails"
            );
        }
    }
}
