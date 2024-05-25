package group.aelysium.rustyconnector.plugin.velocity.lib.remote_storage;

import group.aelysium.rustyconnector.core.proxy.family.matchmaking.rank.*;
import group.aelysium.rustyconnector.plugin.velocity.lib.remote_storage.reactors.MySQLReactor;
import group.aelysium.rustyconnector.plugin.velocity.lib.remote_storage.reactors.StorageReactor;
import group.aelysium.rustyconnector.toolkit.core.UserPass;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IServerResidence;
import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IStaticFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchmaker;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IRankResolver;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IVelocityPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.*;

public class RemoteStorage extends StorageReactor.Holder implements IRemoteStorage, AutoCloseable {
    private final Players players;
    private final FriendLinks friends;
    private final ServerResidences residences;
    private final PlayerRanks ranks;

    private RemoteStorage(StorageReactor reactor) {
        super(reactor);
        this.players = new Players(reactor);
        this.friends = new FriendLinks(reactor);
        this.residences = new ServerResidences(reactor);
        this.ranks = new PlayerRanks(reactor);
    }

    public Players players() {
        return this.players;
    }
    public FriendLinks friends() {
        return this.friends;
    }
    public ServerResidences residences() {
        return this.residences;
    }
    public PlayerRanks ranks() {
        return this.ranks;
    }

    public void close() throws Exception {
        this.reactor.close();
    }

    public static class Players extends StorageReactor.Holder implements IRemoteStorage.Players {
        public Players(StorageReactor reactor) {
            super(reactor);
        }

        public void set(IPlayer player) {
            this.reactor.savePlayer(player.uuid(), player.username());
        }

        public Optional<IPlayer> get(UUID uuid) {
            return this.reactor.fetchPlayer(uuid);
        }
        public Optional<IPlayer> get(String username) {
            return this.reactor.fetchPlayer(username);
        }

        @Override
        public void close() throws Exception {}
    }
    public static class FriendLinks extends StorageReactor.Holder implements IRemoteStorage.FriendLinks {
        public FriendLinks(StorageReactor reactor) {
            super(reactor);
        }

        public void set(IPlayer player1, IPlayer player2) {
            this.reactor.saveFriendLink(PlayerPair.from(player1, player2));
        }

        public Optional<List<IPlayer>> get(IPlayer player) {
            return this.reactor.fetchFriends(player.uuid());
        }

        public void delete(IPlayer player1, IPlayer player2) {
            this.reactor.deleteFriendLink(PlayerPair.from(player1, player2));
        }

        public Optional<Boolean> contains(IPlayer player1, IPlayer player2) {
            return this.reactor.areFriends(PlayerPair.from(player1, player2));
        }

        @Override
        public void close() throws Exception {}
    }
    public static class ServerResidences extends StorageReactor.Holder implements IRemoteStorage.ServerResidences {
        public ServerResidences(StorageReactor reactor) {
            super(reactor);
        }

        public void set(IStaticFamily family, IMCLoader mcLoader, IPlayer player) {
            if(family.homeServerExpiration() == null)
                this.reactor.saveServerResidence(family.id(), mcLoader.uuid(), player.uuid(), null);
            else
                this.reactor.saveServerResidence(family.id(), mcLoader.uuid(), player.uuid(), family.homeServerExpiration().epochFromNow());
        }

        public void delete(String familyId) {
            this.reactor.deleteServerResidences(familyId);
        }

        public void delete(String familyId, IPlayer player) {
            this.reactor.deleteServerResidence(familyId, player.uuid());
        }

        public Optional<IServerResidence.MCLoaderEntry> get(IStaticFamily family, IPlayer player) {
            return this.reactor.fetchServerResidence(family.id(), player.uuid());
        }

        public void purgeExpired() {
            this.reactor.purgeExpiredServerResidences();
        }

        public void refreshExpirations(IStaticFamily family) {
            if(family.homeServerExpiration() == null)
                this.reactor.updateExpirations(family.id(), null);
            else
                this.reactor.updateExpirations(family.id(), family.homeServerExpiration().epochFromNow());
        }

        @Override
        public void close() throws Exception {}
    }
    public static class PlayerRanks extends StorageReactor.Holder implements IRemoteStorage.PlayerRanks {
        public PlayerRanks(StorageReactor reactor) {
            super(reactor);
        }

        public void deleteGame(String gameId) {
            this.reactor.deleteGame(gameId);
        }

        public void delete(IPlayer player) {
            this.reactor.deleteRank(player.uuid());
        }

        public void delete(IPlayer player, String gameId) {
            this.reactor.deleteRank(player.uuid(), gameId);
        }

        public void set(IMatchPlayer player) {
            // Storing randomized player ranks is a literal waste of space.
            if(player.gameRank() instanceof RandomizedPlayerRank) return;
            if(player.gameRank().schemaName().equals(RandomizedPlayerRank.New().schemaName())) return;

            this.reactor.saveRank(player.player().uuid(), player.gameId(), player.gameRank().toJSON());
        }

        public Optional<IVelocityPlayerRank> get(IPlayer player, String gameId, IRankResolver resolver) {
            return this.reactor.fetchRank(player.uuid(), gameId, resolver);
        }

        public void purgeSchemas(IMatchmaker matchmaker) {
            String schema = null;
            if(matchmaker.settings().schema().equals(WinLossPlayerRank.class))    schema = WinLossPlayerRank.schema();
            if(matchmaker.settings().schema().equals(WinRatePlayerRank.class))    schema = WinRatePlayerRank.schema();
            if(matchmaker.settings().schema().equals(ELOPlayerRank.class))        schema = ELOPlayerRank.schema();
            if(matchmaker.settings().schema().equals(OpenSkillPlayerRank.class))  schema = OpenSkillPlayerRank.schema();
            if(schema == null) return;
            this.reactor.purgeInvalidSchemas(matchmaker.gameId(), schema);
        }

        @Override
        public void close() throws Exception {}
    }

    public static class Tinder extends Particle.Tinder<RemoteStorage> {
        private final Configuration configuration;

        public Tinder(@NotNull Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public @NotNull RemoteStorage ignite() throws Exception {
            return new RemoteStorage(this.configuration.reactor());
        }
    }

    public enum StorageType {
        SQLITE,
        MYSQL
    }

    public static abstract class Configuration {
        protected final StorageType type;

        protected Configuration(StorageType type) {
            this.type = type;
        }

        public StorageType type() {
            return this.type;
        }
        public abstract StorageReactor reactor();

        public static class MySQL extends Configuration {
            private final MySQLReactor.Core.Settings settings;

            public MySQL(InetSocketAddress address, UserPass userPass, String database) {
                super(StorageType.MYSQL);
                this.settings = new MySQLReactor.Core.Settings(address, userPass, database);
            }

            public StorageReactor reactor() {
                return new MySQLReactor(settings);
            }

            public InetSocketAddress address() {
                return this.settings.address();
            }
            public UserPass userPass() {
                return this.settings.userPass();
            }
            public String database() {
                return this.settings.database();
            }
        }
    }
}