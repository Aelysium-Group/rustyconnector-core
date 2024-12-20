package group.aelysium.rustyconnector.server;

import group.aelysium.rustyconnector.common.RCAdapter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The Server adapter exists to take loader specific actions and adapt them so that RustyConnector
 * can properly execute them regardless of disparate data types between the wrapper and RustyConnector.
 */
public abstract class ServerAdapter extends RCAdapter {
    /**
     * Set the maximum number of playerRegistry that this server will allow to connect to it.
     * @param max The max number of playerRegistry that can join this server.
     */
    public abstract void setMaxPlayers(int max);

    /**
     * @return The number of online playerRegistry.
     */
    public abstract int onlinePlayerCount();

    /**
     * Resolves a player's username into that player's corresponding UUID.
     * @param username The username.
     * @return An optional containing the user's UUID if there is one, if not, returns an Empty optional.
     */
    public abstract Optional<UUID> playerUUID(@NotNull String username);

    /**
     * Resolves a player's UUID into that player's corresponding username.
     * @param uuid The id.
     * @return An optional containing the user's username if there is one, if not, returns an Empty optional.
     */
    public abstract Optional<String> playerUsername(@NotNull UUID uuid);

    /**
     * Checks if a player is online.
     * @param uuid The id of the player to check for.
     * @return `true` of the player is online. `false` otherwise.
     */
    public abstract boolean isOnline(@NotNull UUID uuid);
}
