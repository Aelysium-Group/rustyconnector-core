package group.aelysium.rustyconnector.server;

import group.aelysium.rustyconnector.common.RCAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    
    /**
     * Teleports one player to another.
     * @param from The player to teleport.
     * @param to The player whose location will be teleported to by 'from'
     */
    public abstract void teleport(@NotNull UUID from, @NotNull UUID to);
    
    /**
     * Teleports a player to a location.
     * @param player The player to teleport.
     * @param x The x coordinate to teleport to. If `null`, the player's x coordinate will not change.
     * @param y The y coordinate to teleport to. If `null`, the player's y coordinate will not change.
     * @param z The z coordinate to teleport to. If `null`, the player's z coordinate will not change.
     */
    public void teleport(@NotNull UUID player, @Nullable Integer x, @Nullable Integer y, @Nullable Integer z) {
        this.teleport(player, x, y, z, null, null);
    }
    
    /**
     * Teleports a player to a location and also rotates them to match the specific pitch and yaw.
     * @param player The player to teleport.
     * @param x The x coordinate to teleport to. If `null`, the player's x coordinate will not change.
     * @param y The y coordinate to teleport to. If `null`, the player's y coordinate will not change.
     * @param z The z coordinate to teleport to. If `null`, the player's z coordinate will not change.
     * @param pitch The players pitch. If `null`, the player's pitch will not change.
     * @param yaw The players yaw. If `null`, the player's yaw will not change.
     */
    public abstract void teleport(@NotNull UUID player, @Nullable Integer x, @Nullable Integer y, @Nullable Integer z, @Nullable Integer pitch, @Nullable Integer yaw);
}
