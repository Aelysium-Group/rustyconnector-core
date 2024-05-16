package group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;

import java.util.Optional;
import java.util.UUID;

public interface IMatchmaker extends IPlayerConnectable, Particle, ILoadBalancer {
    Settings settings();

    /**
     * Gets the game id used by this matchmaker to handle player ranks.
     */
    String gameId();

    IRemoteStorage.PlayerRanks storage();

    /**
     * Gets the matched player for this matchmaker.
     * If no rank exists for this player, it returns an Empty
     */
    Optional<IMatchPlayer> matchPlayer(IPlayer player);

    /**
     * Checks if a player is currently waiting in the matchmaker.
     * @param player The player to look for.
     * @return `true` if the player is waiting in the matchmaker. `false` otherwise.
     */
    boolean contains(IPlayer player);

    /**
     * Fetches a session based on a player's UUID.
     * @param uuid The uuid to search for.
     * @return A session if it exists. Otherwise, an empty Optional.
     */
    Optional<ISession> fetchPlayersSession(UUID uuid);

    /**
     * Checks if the player is currently waiting in the queue.
     */
    boolean isQueued(IPlayer player);

    /**
     * Fetches a session based on a UUID.
     * @param uuid The uuid to search for.
     * @return A session if it exists. Otherwise, an empty Optional.
     */
    Optional<ISession> fetch(UUID uuid);

    /**
     * Returns the total number of players in the Matchmaker.
     */
    int playerCount();

    /**
     * Returns the number of players waiting in queue.
     */
    int queuedPlayerCount();

    /**
     * Returns the number of players in an active session.
     */
    int activePlayerCount();

    /**
     * Returns the total number of sessions in the Matchmaker.
     */
    int sessionCount();

    /**
     * Returns the number of sessions waiting in queue.
     */
    int queuedSessionCount();

    /**
     * Returns the number of sessions that are active.
     */
    int activeSessionCount();

    record Settings (
            Class<? extends IVelocityPlayerRank> schema,
            int min,
            int max,
            double variance,
            boolean reconnect,
            double varianceExpansionCoefficient,
            int requiredExpansionsForAccept,
            LiquidTimestamp sessionDispatchInterval,
            boolean freezeActiveSessions,
            int closingThreshold,
            boolean quittersLose,
            boolean stayersWin,
            boolean leaveCommand,
            boolean parentFamilyOnLeave,
            boolean showInfo,
            ELOSettings elo
    ) {}
    record ELOSettings(double initialRank, double eloFactor, double kFactor) {}
}