package group.aelysium.rustyconnector.plugin.velocity;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import group.aelysium.rustyconnector.common.exception.NoOutputException;
import group.aelysium.rustyconnector.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.proxy.players.Player;
import group.aelysium.rustyconnector.toolkit.velocity.central.ProxyAdapter;
import group.aelysium.rustyconnector.toolkit.velocity.connection.ConnectionResult;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VelocityProxyAdapter extends ProxyAdapter {
    private final ProxyServer velocity;

    public VelocityProxyAdapter(ProxyServer velocity) {
        this.velocity = velocity;
    }

    @Override
    public @Nullable Object convertToProxyPlayer(@NotNull IPlayer player) {
        return this.velocity.getPlayer(player.uuid()).orElse(null);
    }

    @Override
    public @NotNull IPlayer convertToRCPlayer(@NotNull Object player) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) player;
        return new Player(velocityPlayer.getUniqueId(), velocityPlayer.getUsername());
    }

    @Override
    public @NotNull String extractHostname(@NotNull Object player) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) player;
        return velocityPlayer.getVirtualHost().map(InetSocketAddress::getHostString).orElse("").toLowerCase(Locale.ROOT);
    }

    @Override
    public void handleInitialCommit(@NotNull Object player, @NotNull IMCLoader mcloader) {

    }

    @Override
    public void registerMCLoader(@NotNull IMCLoader mcloader) {
        this.velocity.registerServer(new ServerInfo(mcloader.uuid().toString(), mcloader.address()));
    }

    @Override
    public void unregisterMCLoader(@NotNull IMCLoader mcloader) {
        this.velocity.unregisterServer(new ServerInfo(mcloader.uuid().toString(), mcloader.address()));
    }

    @Override
    public void logComponent(@NotNull Component component) {

    }

    @Override
    public void messagePlayer(@NotNull IPlayer player, @NotNull Component message) {
        ((com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player)).sendMessage(message);
    }

    @Override
    public void disconnect(@NotNull IPlayer player, @NotNull Component reason) {
        ((com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player)).disconnect(reason);
    }

    @Override
    public boolean checkPermission(@NotNull IPlayer player, @NotNull String permission) {
        return ((com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player)).hasPermission(permission);
    }

    @Override
    public IPlayerConnectable.Request connectServer(@NotNull IMCLoader mcloader, @NotNull IPlayer player) {
        CompletableFuture<ConnectionResult> result = new CompletableFuture<>();
        IPlayerConnectable.Request request = new IPlayerConnectable.Request(player, result);

        try {
            if (!player.online()) {
                result.complete(ConnectionResult.failed(Component.text(player.username() + " isn't online.")));
                return request;
            }

            com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);
            if (velocityPlayer == null) {
                result.complete(ConnectionResult.failed(Component.text(player.username() + " couldn't be found.")));
                return request;
            }

            if (!((MCLoader) mcloader).validatePlayerLimits(player)) {
                result.complete(ConnectionResult.failed(Component.text("The server is currently full. Try again later.")));
                return request;
            }

            ConnectionRequestBuilder connection = velocityPlayer.createConnectionRequest((RegisteredServer) mcloader.raw());
            try {
                ConnectionRequestBuilder.Result connectionResult = connection.connect().orTimeout(5, TimeUnit.SECONDS).get();

                if (!connectionResult.isSuccessful()) throw new NoOutputException();

                mcloader.setPlayerCount(mcloader.playerCount() + 1);
                result.complete(ConnectionResult.success(Component.text("You successfully connected to the server!"), mcloader));
                return request;
            } catch (Exception ignore) {}
        } catch (Exception ignore) {}

        result.complete(ConnectionResult.failed(Component.text("Unable to connect you to the server!")));
        return request;
    }
}
