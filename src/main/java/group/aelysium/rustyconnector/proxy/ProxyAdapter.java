package group.aelysium.rustyconnector.proxy;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.errors.Error;
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

import static net.kyori.adventure.text.Component.text;

/**
 * The Proxy adapter exists to take proxy specific actions and adapt them so that RustyConnector
 * can properly execute them regardless of disparate data types between the wrapper and RustyConnector.
 */
public abstract class ProxyAdapter extends RCAdapter {
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
     * Registers the Server to the Proxy.
     * RustyConnector will already handle the important registration code.
     * This method only exists to ensure the server is registered to the actual proxy software being used.
     * If you're reading this, you probably want to use {@link ProxyKernel#registerServer(Particle.Flux, Server.Configuration)}.
     * This method only exists for people that know exactly what they're doing.
     * @param server The server to register.
     * @return `true` if the server successfully registered. `false` otherwise.
     */
    public abstract boolean registerServer(@NotNull Server server);

    /**
     * Unregisters the Server from the Proxy.
     * RustyConnector will already handle the important unregistration code.
     * This method only exists to ensure the server is unregistered from the actual proxy software being used.<br/>
     * If you're reading this, you probably want to use {@link ProxyKernel#unregisterServer(Server)}.
     * This method only exists for people that know exactly what they're doing.
     */
    public abstract void unregisterServer(@NotNull Server server);

    /**
     * Whether the server exists on the actual proxy software itself.
     * @param server The server to check for.
     * @return `true` if the server is successfully registered on the actual proxy software. `false` otherwise.
     */
    public abstract boolean serverExists(@NotNull Server server);

    /**
     * Fetches the Server for the player.
     * @param player The player to fetch the Server for.
     */
    public abstract Optional<Server> fetchServer(@NotNull Player player);

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
     * All you need to do is connect to the underlying server that this Server is backed by.
     * You can use {@link Server#raw()} to fetch the underlying server.
     * @param server The server.
     * @param player The player. Specifically, the object returned by {@link #convertToObject(Player)}.
     * @return A connection request.
     */
    public abstract Player.Connection.Request connectServer(@NotNull Server server, @NotNull Player player);

    /**
     * This method contains all the RustyConnector logic for handling a player changing servers.
     * @param player The player.
     * @param oldServer The Server that the player is disconnecting from. If this is null, it signifies that the player just joined the proxy.
     * @param newServer The Server that the player is connecting to.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final void onServerSwitch(
        @NotNull Player player,
        @Nullable Server oldServer,
        @NotNull Server newServer
    ) throws RuntimeException {
        Particle.Flux<? extends Family> newFamily = newServer.family().orElseThrow();

        // Check if the player just joined the proxy.
        if(oldServer == null) {
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newFamily, newServer, player));
            RC.P.EventManager().fireEvent(new ServerPostJoinEvent(newServer, player));
            RC.P.EventManager().fireEvent(new NetworkPostJoinEvent(newFamily, newServer, player));
            return;
        }

        Particle.Flux<? extends Family> oldFamily = oldServer.family().orElseThrow();
        boolean isTheSameFamily = newFamily.equals(oldFamily);

        if(isTheSameFamily) {
            RC.P.EventManager().fireEvent(new FamilyInternalSwitchEvent(newFamily, oldServer, newServer, player));
        } else {
            RC.P.EventManager().fireEvent(new FamilySwitchEvent(oldFamily, newFamily, oldServer, newServer, player));
        }

        RC.P.EventManager().fireEvent(new FamilyLeaveEvent(oldFamily, oldServer, player, false));
        RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newFamily, newServer, player));
        RC.P.EventManager().fireEvent(new ServerLeaveEvent(oldServer, player, false));
        RC.P.EventManager().fireEvent(new ServerPostJoinEvent(newServer, player));
        RC.P.EventManager().fireEvent(new ServerSwitchEvent(oldServer, newServer, player));
    }

    /**
     * Handle's the playerRegistry initial connection to the proxy, before they connect to a server.
     * @param player The player.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final @NotNull Player.Connection.Request onInitialConnect(@NotNull Player player) throws RuntimeException {
        try {
            RC.P.Players().add(player);
        } catch (Exception ignore) {}

        try {
            NetworkPreJoinEvent event = new NetworkPreJoinEvent(player);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
            if (canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
        } catch (Exception ignore) {}

        try {
            return RC.P.Families().rootFamily().access().get(10, TimeUnit.SECONDS).connect(player);
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Player.Connection.Request.failedRequest(player, "There was an internal error preventing this connection");
        }
    }

    public final void onDisconnect(@NotNull Player player) {
        RC.P.EventManager().fireEvent(new NetworkLeaveEvent(player));

        try {
            RC.P.Players().remove(player);
        } catch (Exception ignore) {}

        Server server = player.server().orElse(null);
        if(server == null) return;
        Particle.Flux<? extends Family> family = server.family().orElse(null);
        if(family == null) return;

        RC.P.EventManager().fireEvent(new FamilyLeaveEvent(family, server, player, true));
        RC.P.EventManager().fireEvent(new ServerLeaveEvent(server, player, true));
    }

    /**
     * Decides what should happen to the kicked player.
     * Based on the returned {@link PlayerKickedResponse} you should handle the player's connection appropriately.
     * @param player The player that was kicked.
     * @param reason The reason they were kicked.
     * @return A {@link PlayerKickedResponse}. The caller should properly handle the response so that the desired operations are performed.
     */
    public final @NotNull PlayerKickedResponse onKicked(@NotNull Player player, @Nullable Component reason) {
        boolean isFromRootFamily = false;

        try {
            Server oldServer = player.server().orElseThrow();
            Particle.Flux<? extends Family> family = oldServer.family().orElseThrow();

            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(family, oldServer, player, true));
            RC.P.EventManager().fireEvent(new ServerLeaveEvent(oldServer, player, true));

            isFromRootFamily = RC.P.Families().rootFamily().equals(oldServer.family());
        } catch (Exception ignore) {}

        // Handle root family catching
        try {
            if(isFromRootFamily) return new PlayerKickedResponse(true, Objects.requireNonNullElse(reason, text("Kicked by server.")), null);

            Family family = RC.P.Families().rootFamily().access().get(2, TimeUnit.SECONDS);

            Server server = family.availableServer().orElseThrow();

            return new PlayerKickedResponse(false, reason, server);
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To catch a player into the root family."));
            return new PlayerKickedResponse(false, Objects.requireNonNullElse(reason, text("Kicked by server.")), null);
        }
    }

    /**
     * The response which is given when {@link #onKicked(Player, Component)} is called.
     * @param shouldDisconnect If `true`, the player should ultimately be disconnected from the network.
     *                         `reason` will not be null if this is true.
     *                         `server` will always be null if this is true.
     * @param reason The reason for the player being kicked. Reason will not be null if: `shouldDisconnect` is true, or in some cases when redirect is not null.
     * @param redirect The Server that the player should be redirected to.
     */
    public record PlayerKickedResponse(boolean shouldDisconnect, @Nullable Component reason, @Nullable Server redirect) {}
}
