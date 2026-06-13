package group.aelysium.rustyconnector.proxy;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.events.*;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static net.kyori.adventure.text.Component.text;

/**
 * The Proxy adapter exists to take proxy specific actions and adapt them so that RustyConnector
 * can properly execute them regardless of disparate data types between the wrapper and RustyConnector.
 */
public abstract class ProxyAdapter extends RCAdapter {
    /**
     * @return A set containing all players that are currently online.
     */
    public abstract @NotNull Set<Player> onlinePlayers();
    
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
     * If you're reading this, you probably want to use {@link ProxyKernel#registerServer(Flux, Server.Configuration)}.
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
        Family newFamily = newServer.family().orElseThrow();

        // Check if the player just joined the proxy.
        if(oldServer == null) {
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newFamily, newServer, player));
            RC.P.EventManager().fireEvent(new ServerPostJoinEvent(newServer, player));
            RC.P.EventManager().fireEvent(new NetworkPostJoinEvent(newFamily, newServer, player));
            return;
        }

        Family oldFamily = oldServer.family().orElseThrow();
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
     * @param finalizeConnection A consumer which, assuming previous computations are successful, will take the chosen server and connect the player to it.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final @NotNull Player.Connection.Request onInitialConnect(@NotNull Player player, @NotNull Function<Server, Player.Connection.Request> finalizeConnection) throws RuntimeException {
        try {
            RC.P.Players().signedIn(player);
        } catch (Exception ignore) {}
        
        try {
            NetworkPreJoinEvent event = new NetworkPreJoinEvent(player);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(10, TimeUnit.SECONDS);
            if (canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
        } catch (Exception ignore) {}

        try {
            Family family = RC.P.Family(RC.P.Families().rootFamily()).orElse(null);
            if(family == null)
                return Player.Connection.Request.failedRequest(player, "There are no available servers for you to connect to right now.");
            
            try {
                FamilyPreJoinEvent event = new FamilyPreJoinEvent(family, player, Player.Connection.Power.MINIMAL);
                boolean canceled = RC.P.EventManager().fireEvent(event).get(10, TimeUnit.SECONDS);
                if (canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
            } catch (Exception ignore) {}
            
            Server server = family.availableServer().orElse(null);
            if(server == null)
                return Player.Connection.Request.failedRequest(player, "There are no available servers for you to connect to right now.");
            
            try {
                ServerPreJoinEvent event = new ServerPreJoinEvent(server, player, Player.Connection.Power.MINIMAL);
                boolean canceled = RC.P.EventManager().fireEvent(event).get(10, TimeUnit.SECONDS);
                if (canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
            } catch (Exception ignore) {}
            
            return finalizeConnection.apply(server);
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Player.Connection.Request.failedRequest(player, "There was an internal error preventing this connection");
        }
    }

    public final void onDisconnect(@NotNull Player player) {
        try {
            RC.P.Players().signedOut(player);
        } catch (Exception ignore) {}
        
        RC.P.EventManager().fireEvent(new NetworkLeaveEvent(player));

        Server server = player.server().orElse(null);
        if(server == null) return;
        Family family = server.family().orElse(null);
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
        String rootFamilyID = RC.P.Families().rootFamily();
        Server server = null;
        Family family = null;

        try {
            server = player.server().orElseThrow();
            family = server.family().orElseThrow();

            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(family, server, player, true));
            RC.P.EventManager().fireEvent(new ServerLeaveEvent(server, player, true));

            if(rootFamilyID.equals(family.id())) {
                return new PlayerKickedResponse(Objects.requireNonNullElse(reason, text("Kicked by server.")), null);
            }
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To determine what family " + player.username() + " is from."));
        }

        try {
            Family targetFamily = null;
            if (family != null) {
                Flux<Family> parent = family.parent().orElse(null);
                if(parent != null) targetFamily = parent.get(1, TimeUnit.MINUTES);
            }

            if (targetFamily == null) {
                targetFamily = RC.P.Family(rootFamilyID).orElse(null);
            }

            if(targetFamily == null) {
                return new PlayerKickedResponse(Objects.requireNonNullElse(reason, text("Kicked by server.")), null);
            }

            Server fallbackServer = targetFamily.availableServer().orElse(null);

            if (fallbackServer != null) {
                return new PlayerKickedResponse(Objects.requireNonNullElse(reason, text("No fallback servers available.")), fallbackServer);
            } else {
                return new PlayerKickedResponse(Objects.requireNonNullElse(reason, text("No fallback servers available.")), null);
            }
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To catch a player into a fallback family."));
            return new PlayerKickedResponse(Objects.requireNonNullElse(reason, text("Kicked by server.")), null);
        }
    }

    /**
     * The response which is given when {@link #onKicked(Player, Component)} is called.
     * @param reason The reason for the player being kicked originally.
     * @param redirect The Server that the player was be redirected to. If the server is null, this indicates the player is not eligible for redirection and should be disconnected.
     */
    public record PlayerKickedResponse(@NotNull Component reason, @Nullable Server redirect) {}
}
