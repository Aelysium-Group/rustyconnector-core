package group.aelysium.rustyconnector.toolkit.proxy.player;

import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.MCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Player {
    protected UUID uuid;
    protected String username;

    public Player(@NotNull UUID uuid, @NotNull String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID uuid() { return this.uuid; }
    public String username() { return this.username; }

    /**
     * Check whether the Player is online.
     * @return `true` if the player is online. `false` otherwise.
     */
    public boolean online() {
        return RC.P.Adapter().fetchMCLoader(this).isPresent();
    }

    /**
     * Convenience method that will resolve the player and then send a message to them if the resolution was successful.
     * If the resolution was not successful, nothing will happen.
     * @param message The message to send.
     */
    public void sendMessage(Component message) {
        try {
            RC.P.Adapter().messagePlayer(this, message);
        } catch (Exception ignore) {}
    }

    /**
     * Convenience method that will resolve the player and then disconnect them if the resolution was successful.
     * If the resolution was not successful, nothing will happen.
     * @param reason The message to send as the reason for the disconnection.
     */
    public void disconnect(Component reason) {
        try {
            RC.P.Adapter().disconnect(this, reason);
        } catch (Exception ignore) {}
    }

    /**
     * Convenience method that will resolve the player and then return their MCLoader if there is one.
     */
    public Optional<MCLoader> server() {
        return RC.P.Adapter().fetchMCLoader(this);
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Player that = (Player) object;
        return Objects.equals(uuid, that.uuid);
    }

    public String toString() {
        return "<Player uuid="+this.uuid.toString()+" username="+this.username+">";
    }

    public interface Connectable {
        /**
         * Connects the player to the specified resource.
         * This method will never return anything to the player.
         * It is the caller's job to handle outputs.
         * This method should never throw any exceptions.
         * @param player The player to connect.
         * @return A {@link Connection.Request} for the player's attempt.
         */
        Connection.Request connect(Player player);

        /**
         * Gets the number of players connected to this connectable.
         */
        long players();
    }

    public interface Connection {
        record Request(@NotNull Player player, Future<Result> result) {
            public static Request failedRequest(@NotNull Player player, @NotNull Component message) {
                return new Request(
                        player,
                        CompletableFuture.completedFuture(
                                Result.failed(message)
                        )
                );
            }
            public static Request successfulRequest(@NotNull Player player, @NotNull Component message, @Nullable MCLoader mcloader) {
                return new Request(
                        player,
                        CompletableFuture.completedFuture(
                                Result.success(message, mcloader)
                        )
                );
            }
        }

        /**
         * The result of the connection request.
         * The returned message is always safe to send directly to the player.
         * @param status The status of this connection result.
         * @param message The player-friendly message of this connection result. This message should always be player friendly.
         * @param mcloader The MCLoader that this result resolved from.
         */
        record Result(
                Status status,
                Component message,
                Optional<MCLoader> mcloader
        ) {
            public boolean connected() {
                return this.status == Status.SUCCESS;
            }
            public static Result failed(Component message) {
                return new Result(Status.FAILED, message, Optional.empty());
            }
            public static Result success(Component message, MCLoader server) {
                if(server == null) return new Result(Status.SUCCESS, message, Optional.empty());
                return new Result(Status.SUCCESS, message, Optional.of(server));
            }

            public enum Status {
                FAILED,
                SUCCESS
            }
        }
    }


}