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
     * Resolves a player's username into that player's corresponding ID.
     * @param username The username.
     * @return An optional containing the user's ID if there is one, if not, returns an Empty optional.
     */
    public abstract Optional<String> playerID(@NotNull String username);

    /**
     * Resolves a player's ID into that player's corresponding username.
     * @param id The id.
     * @return An optional containing the user's username if there is one, if not, returns an Empty optional.
     */
    public abstract Optional<String> playerUsername(@NotNull String id);

    /**
     * Checks if a player is online.
     * @param playerID The id of the player to check for.
     * @return `true` of the player is online. `false` otherwise.
     */
    public abstract boolean isOnline(@NotNull String playerID);
    
    /**
     * Teleports one player to another.
     * @param fromPlayer The player to teleport.
     * @param toPlayer The player whose location will be teleported to by 'from'
     */
    public abstract void teleport(@NotNull String fromPlayer, @NotNull String toPlayer);
    
    /**
     * Teleports a player to a location.
     * @param player The player to teleport.
     * @param x The x coordinate to teleport to. If `null`, the player's x coordinate will not change.
     * @param y The y coordinate to teleport to. If `null`, the player's y coordinate will not change.
     * @param z The z coordinate to teleport to. If `null`, the player's z coordinate will not change.
     */
    public void teleport(@NotNull UUID player, @Nullable Double x, @Nullable Double y, @Nullable Double z) {
        this.teleport(player, null, x, y, z, null, null);
    }
    
    /**
     * Teleports a player to a location.
     * @param player The player to teleport.
     * @param world The world to teleport to. If `null`, the player's current world will not change.
     * @param x The x coordinate to teleport to. If `null`, the player's x coordinate will not change.
     * @param y The y coordinate to teleport to. If `null`, the player's y coordinate will not change.
     * @param z The z coordinate to teleport to. If `null`, the player's z coordinate will not change.
     */
    public void teleport(@NotNull UUID player, @Nullable String world, @Nullable Double x, @Nullable Double y, @Nullable Double z) {
        this.teleport(player, world, x, y, z, null, null);
    }
    
    /**
     * Teleports a player to a location and also rotates them to match the specific pitch and yaw.
     * @param player The player to teleport.
     * @param world The world to teleport to. If `null`, the player's current world will not change.
     * @param x The x coordinate to teleport to. If `null`, the player's x coordinate will not change.
     * @param y The y coordinate to teleport to. If `null`, the player's y coordinate will not change.
     * @param z The z coordinate to teleport to. If `null`, the player's z coordinate will not change.
     * @param pitch The players pitch. If `null`, the player's pitch will not change.
     * @param yaw The players yaw. If `null`, the player's yaw will not change.
     */
    public abstract void teleport(@NotNull UUID player, @Nullable String world, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float pitch, @Nullable Float yaw);
}
