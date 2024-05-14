package group.aelysium.rustyconnector.toolkit.velocity.connection;

import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface IPlayerConnectable {
    /**
     * Connects the player to the specified resource.
     * This method will never return anything to the player.
     * It is the caller's job to handle outputs.
     * This method should never throw any exceptions.
     * @param player The player to connect.
     * @return A {@link Request} for the player's attempt.
     */
    Request connect(IPlayer player);

    /**
     * Handles logic when a player leaves this connectable.
     * @param player The player that left.
     */
    void leave(IPlayer player);

    record Request(@NotNull IPlayer player, Future<ConnectionResult> result) {}

    static Request failedRequest(@NotNull IPlayer player, @NotNull Component message) {
        return new Request(
                player,
                CompletableFuture.completedFuture(
                        ConnectionResult.failed(message)
                )
        );
    }
    static Request successfulRequest(@NotNull IPlayer player, @NotNull Component message, @Nullable IMCLoader mcloader) {
        return new Request(
                player,
                CompletableFuture.completedFuture(
                        ConnectionResult.success(message, mcloader)
                )
        );
    }
}
