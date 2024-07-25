package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Players implements Particle {
    private final Map<UUID, Player> players = new ConcurrentHashMap<>();

    /**
     * Adds the player to the record.
     * @param player The player to add.
     */
    public void add(@NotNull Player player) {
        this.players.put(player.uuid(), player);
    }

    public Optional<Player> fetch(@NotNull UUID uuid) {
        return Optional.ofNullable(this.players.get(uuid));
    }

    /**
     * Removes a player.
     * @param uuid The uuid of the player to remove.
     */
    public void remove(@NotNull UUID uuid) {
        this.players.remove(uuid);
    }

    /**
     * Removes a player.
     * @param player The player to remove.
     */
    public void remove(@NotNull Player player) {
        this.remove(player.uuid());
    }

    public List<Player> dump() {
        return new ArrayList<>(this.players.values());
    }

    @Override
    public void close() throws Exception {
        this.players.clear();
    }

    public static class Tinder extends Particle.Tinder<Players> {
        @Override
        public @NotNull Players ignite() throws Exception {
            return new Players();
        }

        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
