package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.ara.Flux;
import group.aelysium.haze.lib.DataHolder;
import group.aelysium.haze.lib.Filter;
import group.aelysium.haze.lib.Type;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.cache.TimeoutCache;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;

public class PersistentPlayerRegistry extends PlayerRegistry {
    private static final String PLAYERS_TABLE = "player_uuid_username_mappings";
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final TimeoutCache<String, Player> playersID;
    private final Map<String, Player> playersUsername = new ConcurrentHashMap<>();
    private final String databaseName;
    private final Flux<HazeDatabase> database;
    
    public PersistentPlayerRegistry(@NotNull String databaseName, @NotNull LiquidTimestamp cacheTimeout) throws Exception {
        this.databaseName = databaseName;
        this.playersID = new TimeoutCache<>(cacheTimeout);
        this.playersID.onTimeout(p -> playersUsername.remove(p.username()));
        
        this.database = RC.P.Haze().fetchDatabase(this.databaseName);
        if(this.database == null) throw new NoSuchElementException("No database exists on the haze provider with the name '"+this.databaseName+"'.");
        HazeDatabase db = this.database.get(1, TimeUnit.MINUTES);
        
        if(db.doesDataHolderExist(PLAYERS_TABLE)) return;
        
        DataHolder table = new DataHolder(PLAYERS_TABLE);
        Map<String, Type> columns = Map.of(
            "id", Type.STRING(36).nullable(false).primaryKey(true),
            "username", Type.STRING(64).nullable(false).unique(true)
        );
        columns.forEach(table::addKey);
        db.createDataHolder(table);
    }

    public void add(@NotNull Player player) {
        this.playersUsername.put(player.username(), player);
        this.playersID.put(player.id(), player);
        
        this.executor.execute(()->{
            try {
                HazeDatabase db = this.database.get(10, TimeUnit.SECONDS);
                db.newUpsertRequest(PLAYERS_TABLE)
                    .parameter("id", player.id)
                    .parameter("username", player.username)
                    .withFilter(Filter.by("id", new Filter.Value(player.id, Filter.Qualifier.EQUALS)))
                    .execute();
            } catch (Exception e) {
                RC.Error(
                    Error.from(e)
                        .whileAttempting("to upsert a player into the Haze provider.")
                        .detail("Player ID", player.id)
                        .detail("Username", player.username)
                );
            }
        });
    }

    @Override
    public Optional<Player> fetchByID(@NotNull String id) {
        Player found = this.playersID.get(id);
        if(found == null) {
            try {
                HazeDatabase db = this.database.get(10, TimeUnit.SECONDS);
                Set<Player> result = db.newReadRequest(PLAYERS_TABLE)
                    .withFilter(Filter.by("id", new Filter.Value(id, Filter.Qualifier.EQUALS)))
                    .limit(1)
                    .execute(Player.class);
                
                found = result.stream().findAny().orElse(null);
                
                if(found == null) return Optional.empty();
                
                this.playersID.put(found.id, found);
                this.playersUsername.put(found.username, found);
            } catch (Exception e) {
                RC.Error(
                    Error.from(e)
                        .whileAttempting("to fetch the player's data from the Haze provider.")
                        .detail("Player ID", id)
                );
            }
        } else this.playersID.refresh(id);
        return Optional.ofNullable(found);
    }
    @Override
    public Optional<Player> fetchByUsername(@NotNull String username) {
        Player found = this.playersUsername.get(username);
        if(found == null) {
            try {
                HazeDatabase db = this.database.get(10, TimeUnit.SECONDS);
                Set<Player> result = db.newReadRequest(PLAYERS_TABLE)
                    .withFilter(Filter.by("username", new Filter.Value(username, Filter.Qualifier.EQUALS)))
                    .limit(1)
                    .execute(Player.class);
                
                found = result.stream().findAny().orElse(null);
                
                if(found == null) return Optional.empty();
                
                this.playersID.put(found.id, found);
                this.playersUsername.put(found.username, found);
            } catch (Exception e) {
                RC.Error(
                    Error.from(e)
                        .whileAttempting("to fetch the player's data from the Haze provider.")
                        .detail("Username", username)
                );
            }
        } else this.playersID.refresh(found.id);
        return Optional.ofNullable(found);
    }

    /**
     * Removes a player.
     * This method only removes the player from the cache.
     * If you intend to delete the player from the persistent storage, you'll have to do that manually.
     * @param player The player to remove.
     */
    @Override
    public void remove(@NotNull Player player) {
        this.playersUsername.remove(player.username());
        this.playersID.remove(player.id());
    }

    public List<Player> dump() {
        return new ArrayList<>(this.playersUsername.values());
    }

    @Override
    public void close() {
        this.playersID.clear();
        this.playersUsername.clear();
        // Database instance is managed by HazeProvider. Not us.
        this.executor.close();
    }

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Total Players", this.playersID.size()),
                        RC.Lang("rustyconnector-keyValue").generate("Players", text(
                        String.join(", ", this.playersUsername.keySet().stream().toList()),
                        BLUE
                ))
        );
    }
}
