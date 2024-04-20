package group.aelysium.rustyconnector.plugin.velocity.lib.players;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayerService;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerService extends IPlayerService {
    private final Map<UUID, Boolean> recentPlayers;

    protected PlayerService(Kernel.Particle kernel) {
        this.recentPlayers = new LinkedHashMap<>(100){
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > 100;
            }
        };
    }

    public void store(IPlayer player) {
        if(this.recentPlayers.containsKey(player.uuid())) return;
        this.storage.database().players().set(player);
        this.recentPlayers.put(player.uuid(), false);
    }

    public Optional<IPlayer> fetch(UUID uuid) {
        return this.storage.database().players().get(uuid);
    }

    public Optional<IPlayer> fetch(String username) {
        return this.storage.database().players().get(username);
    }

    @Override
    public void close() throws Exception {
        // this.storage.kill();  -  Storage is cleaned up in a different process.
    }

    public static class Tinder extends Particle.Tinder<PlayerService> {
        @Override
        public @NotNull PlayerService ignite() throws Exception {
            return new PlayerService();
        }
    }
}
