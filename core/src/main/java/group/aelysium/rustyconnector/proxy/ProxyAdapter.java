package group.aelysium.rustyconnector.proxy;

import group.aelysium.rustyconnector.proxy.events.*;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The Proxy adapter exists to take proxy specific actions and adapt them so that RustyConnector
 * can properly execute them regardless of disparate data types between the wrapper and RustyConnector.
 */
public abstract class ProxyAdapter {
    /**
     * Converts the RustyConnector player object to the Proxy's version.
     * @param player The RustyConnector player.
     * @return The Proxy's version of the player object.
     */
    public abstract @Nullable Object convertToObject(@NotNull Player player);
    /**
     * Converts the Proxy player to RustyConnector's version of the player.
     * @param player The Proxy player.
     * @return The RustyConnector version of the player object.
     */
    public abstract @NotNull Player convertToRCPlayer(@NotNull Object player);

    /**
     * Extracts the player's connection hostname form the player.
     * This method is used in the Forced Hosts/Family Injectors part of RC.
     * @param player The player.
     * @return The extracted hostname from the player's connection.
     */
    public abstract @NotNull String extractHostname(@NotNull Player player);

    /**
     * Registers the MCLoader to the Proxy.
     * RustyConnector will already handle the important registration code.
     * This method only exists to ensure the server is registered to the actual proxy software being used.
     */
    public abstract void registerMCLoader(@NotNull Server mcloader);

    /**
     * Unregisters the MCLoader from the Proxy.
     * RustyConnector will already handle the important unregistration code.
     * This method only exists to ensure the server is unregistered from the actual proxy software being used.
     */
    public abstract void unregisterMCLoader(@NotNull Server mcloader);

    public abstract void logComponent(@NotNull Component component);

    /**
     * Logs the specified component into the console.
     * @param component The component to log.
     */
    public abstract void messagePlayer(@NotNull Player player, @NotNull Component component);

    /**
     * Fetches the MCLoader for the player.
     * @param player The player to fetch the MCLoader for.
     */
    public abstract Optional<Server> fetchMCLoader(@NotNull Player player);

    /**
     * Logs the specified component into the console.
     * @param player The player.
     * @param reason The reason for the disconnect.
     */
    public abstract void disconnect(@NotNull Player player, @NotNull Component reason);

    /**
     * Checks if the player has the specified permission.
     * @param player The player.
     * @param permission The permission to check for.
     * @return `true` if they have the permission. `false` otherwise.
     */
    public abstract boolean checkPermission(@NotNull Player player, @NotNull String permission);

    /**
     * Connects the player to the specified server.
     * By the time this method runs, stuff such as whitelist and player limits have already been addressed.
     * All you need to do is connect to the underlying server that this MCLoader is backed by.
     * You can use {@link Server#raw()} to fetch the underlying server.
     * @param mcloader The mcloader.
     * @param player The player. Specifically, the object returned by {@link #convertToObject(Player)}.
     * @return A connection request.
     */
    public abstract Player.Connection.Request connectServer(@NotNull Server mcloader, @NotNull Player player);

    /**
     * This method contains all the RustyConnector logic for handling a player changing servers.
     * @param player The player.
     * @param oldServer The MCLoader that the player is disconnecting from. If this is null, it signifies that the player just joined the proxy.
     * @param newServer The MCLoader that the player is connecting to.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final void onMCLoaderSwitch(
            @NotNull Player player,
            @Nullable Server oldServer,
            @NotNull Server newServer
            ) throws RuntimeException {
        // Check if the player just joined the proxy.
        if(oldServer == null) {
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newServer.family(), newServer, player));
            RC.P.EventManager().fireEvent(new NetworkJoinEvent(newServer.family(), newServer, player));
            return;
        }

        boolean isTheSameFamily = newServer.family().equals(oldServer.family());

        // Handle an inner-family switch
        if(!isTheSameFamily) {
            RC.P.EventManager().fireEvent(new FamilySwitchEvent(oldServer.family(), newServer.family(), oldServer, newServer, player));
            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(oldServer.family(), oldServer, player, false));
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newServer.family(), newServer, player));
        }

        RC.P.EventManager().fireEvent(new FamilyInternalSwitchEvent(newServer.family(), oldServer, newServer, player));
        RC.P.EventManager().fireEvent(new ServerLeaveEvent(oldServer, player, false));
        RC.P.EventManager().fireEvent(new ServerJoinEvent(newServer, player));
        RC.P.EventManager().fireEvent(new ServerSwitchEvent(oldServer, newServer, player));
    }

    /**
     * Handle's the players initial connection to the proxy, before they connect to a server.
     * @param player The player.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final @NotNull Player.Connection.Request onInitialConnect(@NotNull Player player) throws RuntimeException {
        // Store player
        try {
            if(RC.P.LocalStorage().players().fetch(player.uuid()).isEmpty())
                RC.P.RemoteStorage().players().set(player);
        } catch (Exception ignore) {}

        try {
            return RC.P.Families().rootFamily().access().get(10, TimeUnit.SECONDS).connect(player);
        } catch (Exception e) {
            e.printStackTrace();
            return Player.Connection.Request.failedRequest(player, Component.text(RC.P.Lang().lang().internal_error()));
        }
    }

    public final void onDisconnect(@NotNull Player player) {
        RC.P.EventManager().fireEvent(new NetworkLeaveEvent(player));

        Server mcloader = player.server().orElse(null);
        if(mcloader == null) return;

        RC.P.EventManager().fireEvent(new FamilyLeaveEvent(mcloader.family(), mcloader, player, true));
        RC.P.EventManager().fireEvent(new ServerLeaveEvent(mcloader, player, true));
    }

    /**
     * Decides what should happen to the kicked player.
     * Based on the returned {@link PlayerKickedResponse} you should handle the player's connection appropriately.
     * @param player The player that was kicked.
     * @param reason The reason they were kicked.
     * @return A {@link PlayerKickedResponse}. The caller should properly handle the response so that the desired operations are performed.
     */
    public final @NotNull PlayerKickedResponse onKicked(@NotNull Player player, @Nullable String reason) {
        boolean isFromRootFamily = false;

        try {
            Server oldServer = player.server().orElseThrow();

            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(oldServer.family(), oldServer, player, true));
            RC.P.EventManager().fireEvent(new ServerLeaveEvent(oldServer, player, true));

            isFromRootFamily = RC.P.Families().rootFamily().equals(oldServer.family());
        } catch (Exception ignore) {}

        // Handle root family catching
        try {
            // if (!api.services().family().shouldCatchDisconnectingPlayers()) throw new NoOutputException();

            if(isFromRootFamily) return new PlayerKickedResponse(true, Objects.requireNonNullElse(reason, "Kicked by server."), null);

            Family family = RC.P.Families().rootFamily().access().get(2, TimeUnit.SECONDS);

            Server mcloader = ((ScalarFamily) family).loadBalancer().orElseThrow().staticFetch().orElseThrow();

            return new PlayerKickedResponse(false, reason, mcloader);
        } catch (Exception e) {
            return new PlayerKickedResponse(false, Objects.requireNonNullElse(reason, "Kicked by server. "+e.getMessage()), null);
        }
    }

    /**
     * The response which is given when {@link #onKicked(Player, String)} is called.
     * @param shouldDisconnect If `true`, the player should ultimately be disconnected from the network.
     *                         `reason` will not be null if this is true.
     *                         `mcloader` will always be null if this is true.
     * @param reason The reason for the player being kicked. Reason will not be null if: `shouldDisconnect` is true, or in some cases when redirect is not null.
     * @param redirect The MCLoader that the player should be redirected to.
     */
    public record PlayerKickedResponse(boolean shouldDisconnect, @Nullable String reason, @Nullable Server redirect) {}
}
