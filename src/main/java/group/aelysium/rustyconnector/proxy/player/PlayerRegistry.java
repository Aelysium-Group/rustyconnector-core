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

public class PlayerRegistry implements Module {
    private final Map<String, Player> playersID = new ConcurrentHashMap<>();
    private final Map<String, Player> playersUsername = new ConcurrentHashMap<>();

    /**
     * Adds the player to the record.
     * @param player The player to add.
     */
    public void add(@NotNull Player player) {
        this.playersID.put(player.id(), player);
        this.playersUsername.put(player.username(), player);
    }

    public Optional<Player> fetchByID(@NotNull String id) {
        return Optional.ofNullable(this.playersID.get(id));
    }
    public Optional<Player> fetchByUsername(@NotNull String username) {
        return Optional.ofNullable(this.playersUsername.get(username));
    }

    /**
     * Removes a player.
     * @param player The player to remove.
     */
    public void remove(@NotNull Player player) {
        this.playersID.remove(player.id());
        this.playersUsername.remove(player.username());
    }

    public List<Player> dump() {
        return new ArrayList<>(this.playersID.values());
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
