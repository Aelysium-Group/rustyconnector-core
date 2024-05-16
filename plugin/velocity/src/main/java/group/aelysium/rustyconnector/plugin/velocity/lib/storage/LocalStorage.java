package group.aelysium.rustyconnector.plugin.velocity.lib.storage;


import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage implements ILocalStorage {
    private final ILocalStorage.MCLoaders mcLoaders = new MCLoaders();
    private final ILocalStorage.Players players = new Players();

    @Override
    public ILocalStorage.MCLoaders mcloaders() {
        return null;
    }

    @Override
    public ILocalStorage.Players players() {
        return null;
    }

    public static class Players implements ILocalStorage.Players {
        private final Map<UUID, IPlayer> players = new ConcurrentHashMap<>();

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
    }
}
