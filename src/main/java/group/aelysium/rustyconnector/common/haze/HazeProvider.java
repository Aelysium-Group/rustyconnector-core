package group.aelysium.rustyconnector.common.haze;

import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class HazeProvider implements ModuleParticle, ModuleHolder {
    protected ModuleCollection databases = new ModuleCollection();

    /**
     * Fetches a database.
     * @param name The name of the database to fetch.
     * @return An optional containing the database flux if it exists.
     */
    public @NotNull Optional<Flux<? extends HazeDatabase>> fetchDatabase(@NotNull String name) {
        return Optional.ofNullable(this.databases.fetchModule(name));
    }

    /**
     * Unregisters the database.
     * This method also closes the database and releases all it's resources.
     * @param name The name of the database to close.
     */
    public void unregisterDatabase(@NotNull String name) {
        this.databases.unregisterModule(name);
    }

    /**
     * Registers a new database.
     * @param database The database tinder to load.
     * @throws IllegalStateException If a database with the specific name already exists.
     * @throws Exception If there's an issue initializing the database.
     */
    public void registerDatabase(@NotNull ModuleTinder<? extends HazeDatabase> database) throws Exception {
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
    public Map<String, Flux<? extends ModuleParticle>> modules() {
        return Map.of();
    }

    public static abstract class Tinder extends ModuleTinder<HazeProvider> {
        public Tinder() {
            super(
                "Haze",
                "Provides abstracted database connections."
            );
        }
    }
}
