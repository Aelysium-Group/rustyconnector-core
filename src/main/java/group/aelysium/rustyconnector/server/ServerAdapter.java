package group.aelysium.rustyconnector.server;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The Server adapter exists to take loader specific actions and adapt them so that RustyConnector
 * can properly execute them regardless of disparate data types between the wrapper and RustyConnector.
 */
public abstract class ServerAdapter {
    /**
     * Set the maximum number of players that this server will allow to connect to it.
     * @param max The max number of players that can join this server.
     */
    public abstract void setMaxPlayers(int max);

    /**
     * @return The number of online players.
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
     * @param uuid The uuid.
     * @return An optional containing the user's username if there is one, if not, returns an Empty optional.
     */
    public abstract String playerUsername(@NotNull UUID uuid);

    /**
     * Checks if a player is online.
     * @param uuid The uuid of the player to check for.
     * @return `true` of the player is online. `false` otherwise.
     */
    public abstract boolean isOnline(@NotNull UUID uuid);

    /**
     * Sends the component as a message to the specified player.
     * @param uuid The uuid of the player to send the message to.
     * @param component The component to send.
     */
    public abstract void sendMessage(UUID uuid, Component component);

    /**
     * Logs the components as a message into the console.
     * @param component The component to log.
     */
    public abstract void log(Component component);
}
