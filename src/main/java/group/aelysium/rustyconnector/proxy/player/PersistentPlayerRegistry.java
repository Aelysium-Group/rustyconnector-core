package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.ara.Flux;
import group.aelysium.haze.exceptions.HazeException;
import group.aelysium.haze.lib.DataHolder;
import group.aelysium.haze.lib.Filter;
import group.aelysium.haze.lib.Orderable;
import group.aelysium.haze.lib.Type;
import group.aelysium.haze.requests.ReadRequest;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;

public class PersistentPlayerRegistry implements PlayerRegistry {
    private static final String PLAYERS_TABLE = "player_uuid_username_mappings";
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    private final Map<String, Player> offlineIDs = new LinkedHashMap<>(512) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            offlineUsernames.remove(((Player) eldest.getValue()).username);
            return this.size() > 512;
        }
    };
    private final Map<String, Player> offlineUsernames = new ConcurrentHashMap<>();
    
    private final Map<String, Player> onlineIDs = new ConcurrentHashMap<>();
    private final Map<String, Player> onlineUsernames = new ConcurrentHashMap<>();
    
    private final String databaseName;
    private final Flux<HazeDatabase> database;
    
    public PersistentPlayerRegistry(@NotNull String databaseName, @NotNull LiquidTimestamp cacheTimeout) throws Exception {
        this.databaseName = databaseName;
        
        this.database = RC.P.Haze().fetchDatabase(this.databaseName);
        if(this.database == null) throw new NoSuchElementException("No database exists on the haze provider with the name '"+this.databaseName+"'.");
        HazeDatabase db = this.database.get(1, TimeUnit.MINUTES);
        
        
        RC.P.Adapter().onlinePlayers().forEach(p -> {
            onlineIDs.put(p.id, p);
            onlineUsernames.put(p.username, p);
        });
        
        
        if(db.doesDataHolderExist(PLAYERS_TABLE)) return;
        
        DataHolder table = new DataHolder(PLAYERS_TABLE);
        Map<String, Type> columns = Map.of(
            "id", Type.STRING(36).nullable(false).primaryKey(true),
            "username", Type.STRING(64).nullable(false).unique(true),
            "last_online", Type.DATETIME().nullable(false)
        );
        columns.forEach(table::addKey);
        db.createDataHolder(table);
        
        try {
            ReadRequest sp = db.newReadRequest(PLAYERS_TABLE);
            
            sp.orderBy("list_online", Orderable.Ordering.DESCENDING);
            sp.limit(512);
            
            sp.execute(PlayerDTO.class).forEach(e->{
                Player player = new Player(e.id, e.username);
                
                this.cache(player);
            });
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To fill the internal database cache with recently logged-in players."));
        }
    }

    public void signedIn(@NotNull Player player) {
        if(this.onlineIDs.containsKey(player.id) || this.offlineIDs.containsKey(player.id)) return;
        
        this.executor.execute(()->{
            try {
                HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
                db.newUpsertRequest(PLAYERS_TABLE)
                    .parameter("id", player.id)
                    .parameter("username", player.username)
                    .parameter("last_online", LocalDateTime.now())
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
        this.cache(player);
    }
    
    public void cache(@NotNull Player player) {
        if(player.online()) {
            this.onlineIDs.put(player.id, player);
            this.onlineUsernames.put(player.username, player);
            
            this.offlineIDs.remove(player.id);
            this.offlineUsernames.remove(player.username);
            return;
        }
        
        this.offlineIDs.put(player.id, player);
        this.offlineUsernames.put(player.username, player);
        
        this.onlineIDs.remove(player.id);
        this.onlineUsernames.remove(player.username);
    }
    
    @Override
    public void signedOut(@NotNull Player player) {
        this.onlineIDs.remove(player.id);
        this.onlineUsernames.remove(player.username);
        this.offlineIDs.remove(player.id);
        this.offlineUsernames.remove(player.username);
        
        this.executor.execute(()->{
            try {
                HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
                db.newUpsertRequest(PLAYERS_TABLE)
                    .parameter("id", player.id)
                    .parameter("username", player.username)
                    .parameter("last_online", LocalDateTime.now())
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
        Player found = Optional.ofNullable(this.onlineIDs.get(id)).orElse(this.offlineIDs.get(id));
        if(found == null) {
            try {
                HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
                Set<Player> result = db.newReadRequest(PLAYERS_TABLE)
                    .withFilter(Filter.by("id", new Filter.Value(id, Filter.Qualifier.EQUALS)))
                    .limit(1)
                    .execute(Player.class);
                
                found = result.stream().findAny().orElse(null);
                
                if(found == null) return Optional.empty();
                
                this.cache(found);
            } catch (HazeException e) {
                RC.Error(
                    Error.from(e)
                        .whileAttempting("to fetch the player's data from the Haze provider.")
                        .detail("Player ID", id)
                );
            } catch (Exception ignore) {}
        }
        return Optional.ofNullable(found);
    }
    @Override
    public Optional<Player> fetchByUsername(@NotNull String username) {
        Player found = Optional.ofNullable(this.onlineUsernames.get(username)).orElse(this.offlineUsernames.get(username));
        if(found == null) {
            try {
                HazeDatabase db = this.database.get(5, TimeUnit.SECONDS);
                Set<Player> result = db.newReadRequest(PLAYERS_TABLE)
                    .withFilter(Filter.by("username", new Filter.Value(username, Filter.Qualifier.EQUALS)))
                    .limit(1)
                    .execute(Player.class);
                
                found = result.stream().findAny().orElse(null);
                
                if(found == null) return Optional.empty();
                
                this.cache(found);
            } catch (HazeException e) {
                RC.Error(
                    Error.from(e)
                        .whileAttempting("to fetch the player's data from the Haze provider.")
                        .detail("Username", username)
                );
            } catch (Exception ignore) {}
        }
        return Optional.ofNullable(found);
    }
    
    @Override
    public @NotNull Set<Player> onlinePlayers() {
        return Set.copyOf(this.onlineIDs.values());
    }
    
    @Override
    public void close() {
        this.executor.close();
        
        this.offlineIDs.clear();
        this.offlineUsernames.clear();
        
        this.onlineIDs.clear();
        this.onlineUsernames.clear();
    }

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Online Players", this.onlineIDs.size()),
                        RC.Lang("rustyconnector-keyValue").generate("Players", text(
                        String.join(", ", this.onlineUsernames.keySet().stream().toList()),
                        BLUE
                ))
        );
    }
    
    private record PlayerDTO(
        String id,
        String username,
        LocalDateTime last_online
    ) {}
}
