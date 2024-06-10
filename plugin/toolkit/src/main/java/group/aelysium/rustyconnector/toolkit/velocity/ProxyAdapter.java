package group.aelysium.rustyconnector.toolkit.velocity;

import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.connection.ConnectionResult;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.*;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IScalarFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
    public abstract @NotNull Object convertToProxyPlayer(@NotNull IPlayer player);
    /**
     * Converts the Proxy player to RustyConnector's version of the player.
     * @param player The Proxy player.
     * @return The RustyConnector version of the player object.
     */
    public abstract @NotNull IPlayer convertToRCPlayer(@NotNull Object player);

    /**
     * Extracts the player's connection hostname form the player.
     * This method is used in the Forced Hosts/Family Injectors part of RC.
     * @param player The player.
     * @return The extracted hostname from the player's connection.
     */
    public abstract @NotNull String extractHostname(@NotNull Object player);

    public abstract void handleInitialCommit(@NotNull Object player, @NotNull IMCLoader mcloader);

    /**
     * Registers the MCLoader to the Proxy.
     * RustyConnector will already handle the important registration code.
     * This method only exists to ensure the server is registered to the actual proxy software being used.
     */
    public abstract void registerMCLoader(@NotNull IMCLoader mcloader);

    /**
     * Unregisters the MCLoader from the Proxy.
     * RustyConnector will already handle the important unregistration code.
     * This method only exists to ensure the server is unregistered from the actual proxy software being used.
     */
    public abstract void unregisterMCLoader(@NotNull IMCLoader mcloader);

    /**
     * Logs the specified component into the console.
     * @param component The component to log.
     */
    public abstract void messagePlayer(@NotNull IPlayer player, @NotNull Component component);

    /**
     * Fetches the MCLoader for the player.
     * @param player The player to fetch the MCLoader for.
     */
    public abstract Optional<IMCLoader> fetchMCLoader(@NotNull IPlayer player);

    /**
     * Logs the specified component into the console.
     * @param player The player.
     * @param reason The reason for the disconnect.
     */
    public abstract void disconnect(@NotNull IPlayer player, @NotNull Component reason);

    /**
     * Checks if the player has the specified permission.
     * @param player The player.
     * @param permission The permission to check for.
     * @return `true` if they have the permission. `false` otherwise.
     */
    public abstract boolean checkPermission(@NotNull IPlayer player, @NotNull String permission);

    /**
     * Connects the player to the specified server.
     * `server` will be of whatever type you've defined as {@link IMCLoader#raw()} when registering MCLoaders in {@link #registerMCLoader(IMCLoader)}.
     * @param mcloader The server.
     * @param player The player. Specifically, the object returned by {@link #convertToProxyPlayer(IPlayer)}.
     * @return A connection request.
     */
    public abstract IPlayerConnectable.Request connectServer(@NotNull IMCLoader mcloader, @NotNull IPlayer player);

    /**
     * This method contains all the RustyConnector logic for handling a player changing servers.
     * @param player The player.
     * @param oldMCLoader The MCLoader that the player is disconnecting from. If this is null, it signifies that the player just joined the proxy.
     * @param newMCLoader The MCLoader that the player is connecting to.
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final void onMCLoaderSwitch(
            @NotNull IPlayer player,
            @Nullable IMCLoader oldMCLoader,
            @NotNull IMCLoader newMCLoader
            ) throws RuntimeException {
        // Check if the player just joined the proxy.
        if(oldMCLoader == null) {
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newMCLoader.family(), newMCLoader, player));
            RC.P.EventManager().fireEvent(new NetworkJoinEvent(newMCLoader.family(), newMCLoader, player));
            return;
        }

        boolean isTheSameFamily = newMCLoader.family().equals(oldMCLoader.family());

        // Handle an inner-family switch
        if(!isTheSameFamily) {
            RC.P.EventManager().fireEvent(new FamilySwitchEvent(oldMCLoader.family(), newMCLoader.family(), oldMCLoader, newMCLoader, player));
            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(oldMCLoader.family(), oldMCLoader, player, false));
            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(newMCLoader.family(), newMCLoader, player));
        }

        RC.P.EventManager().fireEvent(new FamilyInternalSwitchEvent(newMCLoader.family(), oldMCLoader, newMCLoader, player));
        RC.P.EventManager().fireEvent(new MCLoaderLeaveEvent(oldMCLoader, player, false));
        RC.P.EventManager().fireEvent(new MCLoaderJoinEvent(newMCLoader, player));
        RC.P.EventManager().fireEvent(new MCLoaderSwitchEvent(oldMCLoader, newMCLoader, player));
    }

    /**
     * Handle's the players intial connection to the proxy, before they connect to a server.
     * @param player The player.
     * @return
     * @throws RuntimeException If there's a fatal error at any point.
     */
    public final @NotNull IPlayerConnectable.Request onInitialConnect(@NotNull IPlayer player) throws RuntimeException {
        CompletableFuture<ConnectionResult> result = new CompletableFuture<>();
        IPlayerConnectable.Request request = new IPlayerConnectable.Request(player, result);

        // Check for network whitelist
        try {
            RC.P.Families()
            IWhitelist whitelist = api.services().whitelist().proxyWhitelist();
            if (whitelist == null) throw new Exception();
            if (!whitelist.validate(player)) {
                result.complete(ConnectionResult.failed(Component.text(whitelist.message())));
                return request;
            }
        } catch (Exception ignore) {}

        try {
            // Handle family injectors if they exist
            try {
                InjectorService injectors = api.services().dynamicTeleport().orElseThrow().services().injector().orElseThrow();

                Family family = api.services().family().rootFamily();
                if(family == null) throw new RuntimeException("Unable to fetch a server to connect to.");

                String host = event.getPlayer().getVirtualHost().map(InetSocketAddress::getHostString).orElse("").toLowerCase(Locale.ROOT);

                family = injectors.familyOf(host).orElseThrow();
                IMCLoader server = family.smartFetch().orElseThrow();

                RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(family, server, player));
                event.setInitialServer(server.registeredServer());
                return;
            } catch (Exception ignore) {}

            Particle.Flux<IFamily> familyFlux = RC.P.Families().rootFamily();
            IFamily family = familyFlux.access().get(10, TimeUnit.SECONDS);
            IMCLoader mcloader = ((IScalarFamily.Connector) family.connector()).loadBalancer().access().get(10, TimeUnit.SECONDS).staticFetch();

            RC.P.EventManager().fireEvent(new FamilyPostJoinEvent(familyFlux, mcloader, player));
            result.complete(ConnectionResult.success(Component.text("Successfully found an MCLoader for the player!"), mcloader));
        } catch (Exception e) {
            result.complete(ConnectionResult.failed(Component.text("We were unable to connect you!")));
        }

        // Store player
        try {
            if(RC.P.LocalStorage().players().fetch(player.uuid()).isPresent()) return request;
            RC.P.RemoteStorage().players().set(player);
        } catch (Exception ignore) {}

        return request;
    }

    public final void onDisconnect(@NotNull IPlayer player) {
        RC.P.EventManager().fireEvent(new NetworkLeaveEvent(player));

        IMCLoader mcloader = player.server().orElse(null);
        if(mcloader == null) return;

        RC.P.EventManager().fireEvent(new FamilyLeaveEvent(mcloader.family(), mcloader, player, true));
        RC.P.EventManager().fireEvent(new MCLoaderLeaveEvent(mcloader, player, true));
    }

    /**
     * Decides what should happen to the kicked player.
     * Based on the returned {@link PlayerKickedResponse} you should handle the player's connection appropriately.
     * @param player The player that was kicked.
     * @param reason The reason they were kicked.
     * @return A {@link PlayerKickedResponse}. The caller should properly handle the response so that the desired operations are performed.
     */
    public final @NotNull PlayerKickedResponse onKicked(@NotNull IPlayer player, @Nullable String reason) {
        boolean isFromRootFamily = false;

        try {
            IMCLoader oldServer = player.server().orElseThrow();

            RC.P.EventManager().fireEvent(new FamilyLeaveEvent(oldServer.family(), oldServer, player, true));
            RC.P.EventManager().fireEvent(new MCLoaderLeaveEvent(oldServer, player, true));

            isFromRootFamily = RC.P.Families().rootFamily().equals(oldServer.family());
        } catch (Exception ignore) {}

        // Handle root family catching
        try {
            // if (!api.services().family().shouldCatchDisconnectingPlayers()) throw new NoOutputException();

            if(isFromRootFamily) return new PlayerKickedResponse(true, Objects.requireNonNullElse(reason, "Kicked by server."), null);

            IFamily family = RC.P.Families().rootFamily().access().get(2, TimeUnit.SECONDS);

            IMCLoader mcloader = ((IScalarFamily.Connector) family.connector()).loadBalancer().orElseThrow().staticFetch().orElseThrow();

            return new PlayerKickedResponse(false, reason, mcloader);
        } catch (Exception e) {
            return new PlayerKickedResponse(false, Objects.requireNonNullElse(reason, "Kicked by server. "+e.getMessage()), null);
        }
    }

    /**
     * The response which is given when {@link #onKicked(IPlayer, String)} is called.
     * @param shouldDisconnect If `true`, the player should ultimately be disconnected from the network.
     *                         `reason` will not be null if this is true.
     *                         `mcloader` will always be null if this is true.
     * @param reason The reason for the player being kicked. Reason will not be null if: `shouldDisconnect` is true, or in some cases when redirect is not null.
     * @param redirect The MCLoader that the player should be redirected to.
     */
    public record PlayerKickedResponse(boolean shouldDisconnect, @Nullable String reason, @Nullable IMCLoader redirect) {}
}
