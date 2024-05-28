package group.aelysium.rustyconnector.plugin.velocity.lib.players;

import group.aelysium.rustyconnector.proxy.family.matchmaking.rank.DefaultRankResolver;
import group.aelysium.rustyconnector.proxy.family.mcloader.MCLoader;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IVelocityPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Player implements IPlayer {
    protected UUID uuid;
    protected String username;

    public Player(@NotNull UUID uuid, @NotNull String username) {
        this.uuid = uuid;
        this.username = username;
    }
    public Player(@NotNull com.velocitypowered.api.proxy.Player velocityPlayer) {
        this.uuid = velocityPlayer.getUniqueId();
        this.username = velocityPlayer.getUsername();
    }

    public UUID uuid() { return this.uuid; }
    public String username() { return this.username; }

    public void sendMessage(Component message) {
        try {
            this.resolve().orElseThrow().sendMessage(message);
        } catch (Exception ignore) {}
    }

    public void disconnect(Component reason) {
        try {
            this.resolve().orElseThrow().disconnect(reason);
        } catch (Exception ignore) {}
    }

    public Optional<com.velocitypowered.api.proxy.Player> resolve() {
        return Tinder.get().velocityServer().getPlayer(this.uuid);
    }

    @Override
    public boolean online() {
        return resolve().isPresent();
    }

    public Optional<IVelocityPlayerRank> rank(String gameId) {
        return RC.P.RemoteStorage().ranks().get(this, gameId, DefaultRankResolver.New());
    }

    public Optional<IMCLoader> server() {
        try {
            com.velocitypowered.api.proxy.Player resolvedPlayer = this.resolve().orElseThrow();
            UUID mcLoaderUUID = UUID.fromString(resolvedPlayer.getCurrentServer().orElseThrow().getServerInfo().getName());

            MCLoader mcLoader = new MCLoader.Reference(mcLoaderUUID).get();

            return Optional.of(mcLoader);
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Player that = (Player) object;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public String toString() {
        return "<Player uuid="+this.uuid.toString()+" username="+this.username+">";
    }

    /**
     * Anytime a new player joins the proxy, RustyConnector automatically stores them to your database.
     * Running this method is rarely necessary for you.
     */
    public void store() {
        RC.P.RemoteStorage().players().set(this);
    }
}