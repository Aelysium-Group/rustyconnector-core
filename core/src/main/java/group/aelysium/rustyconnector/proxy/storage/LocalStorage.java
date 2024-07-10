package group.aelysium.rustyconnector.proxy.storage;

import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local storage is the internal memory solution used for short-term internal RustyConnector data.
 * Local storage will not persist between software restarts since it's stored in RAM.
 */
public class LocalStorage {
    private final Servers servers = new Servers();
    private final Players players = new Players();

    public Servers mcloaders() {
        return this.servers;
    }
    
    public Players players() {
        return this.players;
    }

    public void close() throws Exception {
        this.servers.close();
        this.players.close();
    }

    public static class Players {
        private final Map<UUID, Player> players = new LinkedHashMap<>(100){
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > 100;
            }
        };

        public void store(UUID uuid, Player player) {
            this.players.put(uuid, player);
        }

        public Optional<Player> fetch(UUID uuid) {
            return Optional.ofNullable(this.players.get(uuid));
        }

        public void remove(UUID uuid) {
            this.players.remove(uuid);
        }

        public void close() throws Exception {
            this.players.clear();
        }
    }

    public static class Servers {
        private final Map<UUID, Server> servers = new ConcurrentHashMap<>();

        public void store(UUID uuid, Server server) {
            this.servers.put(uuid, server);
        }

        public Optional<Server> fetch(UUID uuid) {
            return Optional.ofNullable(this.servers.get(uuid));
        }

        public void remove(UUID uuid) {
            this.servers.remove(uuid);
        }

        public void close() throws Exception {
            this.servers.clear();
        }
    }
}
