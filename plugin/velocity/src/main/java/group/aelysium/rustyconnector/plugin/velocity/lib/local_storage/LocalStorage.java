package group.aelysium.rustyconnector.plugin.velocity.lib.local_storage;


import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage implements ILocalStorage {
    private final ILocalStorage.MCLoaders mcloaders = new MCLoaders();
    private final ILocalStorage.Players players = new Players();

    @Override
    public ILocalStorage.MCLoaders mcloaders() {
        return this.mcloaders;
    }

    @Override
    public ILocalStorage.Players players() {
        return this.players;
    }

    @Override
    public void close() throws Exception {
        this.mcloaders.close();
        this.players.close();
    }

    public static class Players implements ILocalStorage.Players {
        private final Map<UUID, IPlayer> players = new LinkedHashMap<>(100){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > 100;
            }
        };

        @Override
        public void store(UUID uuid, IPlayer player) {
            this.players.put(uuid, player);
        }

        @Override
        public Optional<IPlayer> fetch(UUID uuid) {
            return Optional.ofNullable(this.players.get(uuid));
        }

        @Override
        public void remove(UUID uuid) {
            this.players.remove(uuid);
        }

        @Override
        public void close() throws Exception {
            this.players.clear();
        }
    }

    public static class MCLoaders implements ILocalStorage.MCLoaders {
        private final Map<UUID, IMCLoader> mcloaders = new ConcurrentHashMap<>();

        @Override
        public void store(UUID uuid, IMCLoader mcloader) {
            this.mcloaders.put(uuid, mcloader);
        }

        @Override
        public Optional<IMCLoader> fetch(UUID uuid) {
            return Optional.ofNullable(this.mcloaders.get(uuid));
        }

        @Override
        public void remove(UUID uuid) {
            this.mcloaders.remove(uuid);
        }

        @Override
        public void close() throws Exception {
            this.mcloaders.clear();
        }
    }
}
