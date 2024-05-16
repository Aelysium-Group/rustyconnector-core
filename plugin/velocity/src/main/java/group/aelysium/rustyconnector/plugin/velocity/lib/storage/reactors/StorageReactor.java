package group.aelysium.rustyconnector.plugin.velocity.lib.storage.reactors;

import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IServerResidence;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IRankResolver;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IVelocityPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Module containing valid Storage queries that can be used to access database information.
 */
public interface StorageReactor extends Particle {
    void initializeDatabase();

    void updateExpirations(String familyId, Long newExpirationEpoch);
    void purgeExpiredServerResidences();
    Optional<IServerResidence.MCLoaderEntry> fetchServerResidence(String familyId, UUID player);
    void deleteServerResidence(String familyId, UUID player);
    void deleteServerResidences(String familyId);
    void saveServerResidence(String familyId, UUID mcloader, UUID player, Long expirationEpoch);
    void deleteFriendLink(PlayerPair pair);
    Optional<Boolean> areFriends(PlayerPair pair);
    Optional<List<IPlayer>> fetchFriends(UUID player);
    void saveFriendLink(PlayerPair pair);
    Optional<IPlayer> fetchPlayer(UUID uuid);
    Optional<IPlayer> fetchPlayer(String username);
    void savePlayer(UUID uuid, String username);
    void deleteGame(String gameId);
    void deleteRank(UUID player);
    void deleteRank(UUID player, String gameId);
    void saveRank(UUID player, String gameId, JsonObject rank);
    void purgeInvalidSchemas(String gameId, String validSchema);
    Optional<IVelocityPlayerRank> fetchRank(UUID player, String gameId, IRankResolver resolver);
    abstract class Holder {
        protected final StorageReactor reactor;
        public Holder(StorageReactor reactor) {
            this.reactor = reactor;
        }
    }
}