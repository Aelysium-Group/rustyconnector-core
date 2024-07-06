package group.aelysium.rustyconnector.plugin.velocity;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import group.aelysium.rustyconnector.common.exception.NoOutputException;
import group.aelysium.rustyconnector.toolkit.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.toolkit.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class VelocityProxyAdapter extends ProxyAdapter {
    private final ProxyServer velocity;

    public VelocityProxyAdapter(ProxyServer velocity) {
        this.velocity = velocity;
    }

    @Override
    public @Nullable Object convertToProxyPlayer(@NotNull Player player) {
        return this.velocity.getPlayer(player.uuid()).orElse(null);
    }

    @Override
    public @NotNull Player convertToRCPlayer(@NotNull Object player) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) player;

        return new Player(velocityPlayer.getUniqueId(), velocityPlayer.getUsername());
    }

    @Override
    public @NotNull String extractHostname(@NotNull Player player) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);
        return velocityPlayer.getVirtualHost().map(InetSocketAddress::getHostString).orElse("").toLowerCase(Locale.ROOT);
    }

    @Override
    public void registerMCLoader(@NotNull MCLoader mcloader) {
        this.velocity.registerServer(new ServerInfo(mcloader.uuidOrDisplayName(), mcloader.address()));
    }

    @Override
    public void unregisterMCLoader(@NotNull MCLoader mcloader) {
        this.velocity.unregisterServer(new ServerInfo(mcloader.uuidOrDisplayName(), mcloader.address()));
    }

    @Override
    public void logComponent(@NotNull Component component) {

    }

    @Override
    public void messagePlayer(@NotNull Player player, @NotNull Component message) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);

        velocityPlayer.sendMessage(message);
    }

    @Override
    public Optional<MCLoader> fetchMCLoader(@NotNull Player player) {
        return Optional.empty();
    }

    @Override
    public void disconnect(@NotNull Player player, @NotNull Component reason) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);

        velocityPlayer.disconnect(reason);
    }

    @Override
    public boolean checkPermission(@NotNull Player player, @NotNull String permission) {
        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);

        return velocityPlayer.hasPermission(permission);
    }

    @Override
    public Player.Connection.Request connectServer(@NotNull MCLoader mcloader, @NotNull Player player) {
        RegisteredServer server = (RegisteredServer) mcloader.raw();

        com.velocitypowered.api.proxy.Player velocityPlayer = (com.velocitypowered.api.proxy.Player) this.convertToProxyPlayer(player);
        if(velocityPlayer == null) {
            return Player.Connection.Request.failedRequest(player, Component.text("No player could be found!"));
        }

        ConnectionRequestBuilder connection = velocityPlayer.createConnectionRequest(server);
        try {
            ConnectionRequestBuilder.Result connectionResult = connection.connect().orTimeout(5, TimeUnit.SECONDS).get();

            if (!connectionResult.isSuccessful()) throw new NoOutputException();

            mcloader.setPlayerCount((int) (mcloader.players() + 1));
            return Player.Connection.Request.successfulRequest(player, Component.text("You successfully connected to the server!"), mcloader);
        } catch (Exception ignore) {}
        return Player.Connection.Request.failedRequest(player, Component.text("There was an issue connecting!"));
    }
}
