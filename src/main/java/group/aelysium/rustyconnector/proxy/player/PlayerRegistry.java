package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE;

public class PlayerRegistry implements ModuleParticle {
    private final Map<UUID, Player> playersUUID = new ConcurrentHashMap<>();
    private final Map<String, Player> playersUsername = new ConcurrentHashMap<>();

    /**
     * Adds the player to the record.
     * @param player The player to add.
     */
    public void add(@NotNull Player player) {
        this.playersUUID.put(player.uuid(), player);
        this.playersUsername.put(player.username(), player);
    }

    public Optional<Player> fetch(@NotNull UUID uuid) {
        return Optional.ofNullable(this.playersUUID.get(uuid));
    }
    public Optional<Player> fetch(@NotNull String username) {
        return Optional.ofNullable(this.playersUsername.get(username));
    }

    /**
     * Removes a player.
     * @param player The player to remove.
     */
    public void remove(@NotNull Player player) {
        this.playersUUID.remove(player.uuid());
        this.playersUsername.remove(player.username());
    }

    public List<Player> dump() {
        return new ArrayList<>(this.playersUUID.values());
    }

    @Override
    public void close() {
        this.playersUUID.clear();
        this.playersUsername.clear();
    }

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Total Players", this.playersUUID.size()),
                        RC.Lang("rustyconnector-keyValue").generate("Players", text(
                        String.join(", ", this.playersUsername.keySet().stream().toList()),
                        BLUE
                ))
        );
    }

    public static class Tinder extends ModuleTinder<PlayerRegistry> {
        public Tinder() {
            super(
                "PlayerRegistry",
                "Provides player access services."
            );
        }

        @Override
        public @NotNull PlayerRegistry ignite() throws Exception {
            return new PlayerRegistry();
        }

        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
