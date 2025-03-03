package group.aelysium.rustyconnector.common.haze;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class HazeProvider implements Module, ModuleHolder<HazeDatabase> {
    protected ModuleCollection<HazeDatabase> databases = new ModuleCollection<>();

    /**
     * Fetches a database.
     * @param name The name of the database to fetch.
     * @return An optional containing the database flux if it exists.
     */
    public @Nullable Flux<HazeDatabase> fetchDatabase(@NotNull String name) {
        return this.databases.fetchModule(name);
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
    public void registerDatabase(@NotNull Module.Builder<HazeDatabase> database) throws Exception {
        this.databases.registerModule(database);
    }
    public boolean containsDatabase(@NotNull String name) {
        return this.databases.containsModule(name);
    }

    @Override
    public Map<String, Flux<HazeDatabase>> modules() {
        return Map.of();
    }
    
    @Override
    public void close() throws Exception {
        this.databases.close();
    }
}
