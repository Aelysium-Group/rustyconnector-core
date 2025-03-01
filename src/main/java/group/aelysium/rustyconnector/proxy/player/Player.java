package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.RC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static net.kyori.adventure.text.Component.text;

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
        return RC.P.Adapter().fetchServer(this).isPresent();
    }

    /**
     * Sends a message to the player.
     * If there's an issue messaging the player, nothing will happen.
     * @param message The message to send.
     */
    public void message(@NotNull Component message) {
        try {
            RC.P.Adapter().messagePlayer(this.uuid(), message);
        } catch (Exception ignore) {}
    }

    /**
     * Disconnects the player with a message
     * @param reason The message to send as the reason for the disconnection.
     */
    public void disconnect(@Nullable Component reason) {
        try {
            RC.P.Adapter().disconnect(this, reason == null ? text("Disconnected from server") : reason);
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
    }
    
    /**
     * Fetches the player's server if they're connected to one.
     */
    public Optional<Server> server() {
        return RC.P.Adapter().fetchServer(this);
    }
    
    /**
     * Fetches the player's family if they're connected to one.
     */
    public Optional<Family> family() {
        try {
            return Optional.of(this.server().orElseThrow().family().orElseThrow());
        } catch (Exception ignore) {
            return Optional.empty();
        }
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
         * @param power The power level to use for the connection.
         * @return A {@link Connection.Request} for the player's attempt.
         */
        Connection.Request connect(Player player, Connection.Power power);

        /**
         * Uses {@link #connect(Player, Connection.Power)} using {@link Connection.Power#MINIMAL}.
         * @param player The player to connect.
         * @return A {@link Connection.Request} for the player's attempt.
         */
        Connection.Request connect(Player player);

        /**
         * Gets the number of playerRegistry connected to this connectable.
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
            public static Request failedRequest(@NotNull Player player, @NotNull String message) {
                return new Request(
                        player,
                        CompletableFuture.completedFuture(
                                Result.failed(text(message, NamedTextColor.RED))
                        )
                );
            }
            public static Request successfulRequest(@NotNull Player player, @NotNull Component message, @Nullable Server server) {
                return new Request(
                        player,
                        CompletableFuture.completedFuture(
                                Result.success(message, server)
                        )
                );
            }
            public static Request successfulRequest(@NotNull Player player, @NotNull String message, @Nullable Server server) {
                return new Request(
                        player,
                        CompletableFuture.completedFuture(
                                Result.success(text(message, NamedTextColor.GREEN), server)
                        )
                );
            }
        }

        /**
         * The result of the connection request.
         * The returned message is always safe to send directly to the player.
         * @param connected Did the player successfully connect.
         * @param message The player-friendly message of this connection result. This message should always be player friendly.
         * @param server The Server that this result resolved from.
         */
        record Result(
                boolean connected,
                @NotNull Component message,
                @Nullable Server server
        ) {
            public static Result failed(@NotNull Component message) {
                return new Result(false, message, null);
            }
            public static Result success(@NotNull Component message, Server server) {
                if(server == null) return new Result(true, message, null);
                return new Result(true, message, server);
            }
        }

        /**
         * Connection Power dictates certain criteria to follow when connecting a player to servers.
         * Depending on the state of the server and the connection power used, connections may fail.<br/>
         * If the player has the proper bypass permissions, this power level will be ignored.
         */
        enum Power {
            /**
             * Default connection power used.
             * If the server has reached it's softCap, the connection will fail.
             */
            MINIMAL,
            /**
             * If the server has reached it's softCap the player will still be able to join.
             * If the server has reached it's hardCap, the connection will fail.
             */
            MODERATE,
            /**
             * Regardless of if the server has reached its soft or hard cap, the connection should never fail as a result of player count.
             */
            AGGRESSIVE
        }
    }
}