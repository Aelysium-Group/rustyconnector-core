package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.Module;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;

/**
 * Provides in-memory access to online players.
 * As long as the network is running, this registry will keep track of all online players as well as any players that were online and then disconnected.
 * It's impossible for this registry to cover anything outside in-memory tracking of players.
 * If you'd like to persistently track players, look at {@link PersistentPlayerRegistry}.
 */
public class OnlinePlayersRegistry implements PlayerRegistry {
    private final Map<String, Player> playersID = new ConcurrentHashMap<>();
    private final Map<String, Player> playersUsername = new ConcurrentHashMap<>();
    
    public OnlinePlayersRegistry() {
        RC.P.Adapter().onlinePlayers().forEach(p -> {
            playersID.put(p.id, p);
            playersUsername.put(p.username, p);
        });
    }

    @Override
    public void signedIn(@NotNull Player player) {
        this.playersID.put(player.id(), player);
        this.playersUsername.put(player.username(), player);
    }
    
    @Override
    public void signedOut(@NotNull Player player) {
        this.playersID.remove(player.id());
        this.playersUsername.remove(player.username());
    }
    
    @Override
    public Optional<Player> fetchByID(@NotNull String id) {
        return Optional.ofNullable(this.playersID.get(id));
    }
    
    @Override
    public Optional<Player> fetchByUsername(@NotNull String username) {
        return Optional.ofNullable(this.playersUsername.get(username));
    }

    @Override
    public @NotNull Set<Player> onlinePlayers() {
        Set<Player> players = RC.P.Adapter().onlinePlayers();
        
        if(players.size() != this.playersID.size())
            players.forEach(p -> {
                playersID.put(p.id, p);
                playersUsername.put(p.username, p);
            });
        
        return Collections.unmodifiableSet(players);
    }

    @Override
    public void close() {
        this.playersID.clear();
        this.playersUsername.clear();
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
