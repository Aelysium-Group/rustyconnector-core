package group.aelysium.rustyconnector.toolkit.velocity.storage;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * Remote storage is the external memory solution used to long-term storage of RustyConnector data.
 * Remote storage will persist between software restarts.
 */
public interface IRemoteStorage extends Particle {
    Players players();

    interface Players {
        /**
         * Sets the player in this database.
         * If the player hasn't been stored before, it will be stored.
         * If the player already exists, their data will be updated.
         * @param player The player to set.
         */
        void set(IPlayer player);

        /**
         * Gets a player from the database.
         * @param uuid The uuid to search for.
         * @return The player if they exist. Empty otherwise.
         */
        Optional<IPlayer> get(UUID uuid);

        /**
         * Gets a player from the database.
         * @param username The username to search for.
         * @return The player if they exist. Empty otherwise.
         */
        Optional<IPlayer> get(String username);
    }
}