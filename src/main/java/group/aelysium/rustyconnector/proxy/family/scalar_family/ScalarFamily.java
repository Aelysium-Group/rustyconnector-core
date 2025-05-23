package group.aelysium.rustyconnector.proxy.family.scalar_family;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.proxy.events.FamilyPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scalar FamilyRegistry are the built-in stateless Family example.
 * They provide an example for how familyRegistry should be implemented.
 */
public class ScalarFamily extends Family {
    public ScalarFamily(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent,
            @NotNull Map<String, Object> metadata,
            @NotNull Module.Builder<LoadBalancer> loadBalancer
    ) throws Exception {
        super(id, displayName, parent, metadata);
        this.registerModule(loadBalancer);
    }

    public Flux<LoadBalancer> loadBalancer() {
        return this.fetchModule("LoadBalancer");
    }

    public void addServer(@NotNull Server server) {
        this.loadBalancer().ifPresent(l -> l.addServer(server));
    }

    public void removeServer(@NotNull Server server) {
        this.loadBalancer().ifPresent(l -> l.removeServer(server));
    }

    @Override
    public Optional<Server> fetchServer(@NotNull String id) {
        AtomicReference<Optional<Server>> server = new AtomicReference<>();
        this.loadBalancer().ifPresent(l -> server.set(l.fetchServer(id)));
        return server.get();
    }

    @Override
    public boolean containsServer(@NotNull String id) {
        AtomicBoolean value = new AtomicBoolean(false);
        this.loadBalancer().ifPresent(l -> value.set(l.containsServer(id)));
        return value.get();
    }

    @Override
    public void lockServer(@NotNull Server server) {
        this.loadBalancer().ifPresent(l -> l.lockServer(server));
    }

    @Override
    public void unlockServer(@NotNull Server server) {
        this.loadBalancer().ifPresent(l -> l.unlockServer(server));
    }

    @Override
    public List<Server> lockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer().ifPresent(l -> value.set(l.lockedServers()));
        return value.get();
    }

    @Override
    public List<Server> unlockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer().ifPresent(l -> value.set(l.unlockedServers()));
        return value.get();
    }

    public long players() {
        AtomicLong value = new AtomicLong(0);
        this.loadBalancer().ifPresent(l -> {
                l.lockedServers().forEach(s -> value.addAndGet(s.players()));
                l.unlockedServers().forEach(s -> value.addAndGet(s.players()));
            }
        );

        return value.get();
    }

    @Override
    public List<Server> servers() {
        AtomicReference<List<Server>> servers = new AtomicReference<>(new ArrayList<>());

        this.loadBalancer().ifPresent(l -> servers.set(l.servers()));

        return servers.get();
    }

    @Override
    public Optional<Server> availableServer() {
        AtomicReference<Server> server = new AtomicReference<>(null);

        this.loadBalancer().ifPresent(l -> server.set(l.availableServer().orElse(null)));

        return Optional.ofNullable(server.get());
    }

    @Override
    public boolean isLocked(@NotNull Server server) {
        AtomicBoolean valid = new AtomicBoolean(false);
        this.loadBalancer().ifPresent(l -> valid.set(l.isLocked(server)));
        return valid.get();
    }

    @Override
    public Player.Connection.Request connect(Player player, Player.Connection.Power power) {
        if(this.unlockedServers().isEmpty()) return Player.Connection.Request.failedRequest(player, "Unable to connect you to your server. Please try again later.");

        try {
            FamilyPreJoinEvent event = new FamilyPreJoinEvent(this, player, power);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(10, TimeUnit.SECONDS);
            if(canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
        } catch (Exception ignore) {}

        try {
            return this.loadBalancer().get(20, TimeUnit.SECONDS).current().orElseThrow().connect(player, power);
        } catch (Exception ignore) {
            return Player.Connection.Request.failedRequest(player, "Unable to connect you to your server. Please try again later.");
        }
    }
    @Override
    public Player.Connection.Request connect(Player player) {
        return this.connect(player, Player.Connection.Power.MINIMAL);
    }

    @Override
    public void close() throws Exception {
        super.close();
    }
}
