package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.ara.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry implements Particle {
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

    public static class Tinder extends Particle.Tinder<PlayerRegistry> {
        @Override
        public @NotNull PlayerRegistry ignite() throws Exception {
            return new PlayerRegistry();
        }

        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
