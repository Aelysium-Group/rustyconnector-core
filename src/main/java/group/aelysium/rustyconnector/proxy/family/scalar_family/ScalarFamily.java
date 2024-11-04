package group.aelysium.rustyconnector.proxy.family.scalar_family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.events.FamilyPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
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
    protected ScalarFamily(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent,
            @NotNull Particle.Flux<LoadBalancer> loadBalancer
    ) throws Exception {
        super(id, displayName, parent);
        this.installPlugin(loadBalancer);
    }

    public Particle.Flux<LoadBalancer> loadBalancer() {
        return (Flux<LoadBalancer>) this.fetchPlugin("LoadBalancer").orElseThrow();
    }

    public void addServer(@NotNull Server server) {
        this.loadBalancer().executeNow(l -> l.addServer(server));
    }

    public void removeServer(@NotNull Server server) {
        this.loadBalancer().executeNow(l -> l.removeServer(server));
    }

    @Override
    public Optional<Server> fetchServer(@NotNull UUID uuid) {
        AtomicReference<Optional<Server>> server = new AtomicReference<>();
        this.loadBalancer().executeNow(l -> server.set(l.fetchServer(uuid)));
        return server.get();
    }

    @Override
    public boolean containsServer(@NotNull UUID uuid) {
        AtomicBoolean value = new AtomicBoolean(false);
        this.loadBalancer().executeNow(l -> value.set(l.containsServer(uuid)));
        return value.get();
    }

    @Override
    public void lockServer(@NotNull Server server) {
        this.loadBalancer().executeNow(l -> l.lockServer(server));
    }

    @Override
    public void unlockServer(@NotNull Server server) {
        this.loadBalancer().executeNow(l -> l.unlockServer(server));
    }

    @Override
    public List<Server> lockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer().executeNow(l -> value.set(l.lockedServers()));
        return value.get();
    }

    @Override
    public List<Server> unlockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer().executeNow(l -> value.set(l.unlockedServers()));
        return value.get();
    }

    public long players() {
        AtomicLong value = new AtomicLong(0);
        this.loadBalancer().executeNow(l -> {
                l.unlockedServers().forEach(s -> value.addAndGet(s.players()));
                l.unlockedServers().forEach(s -> value.addAndGet(s.players()));
            }
        );

        return value.get();
    }

    @Override
    public List<Server> servers() {
        AtomicReference<List<Server>> servers = new AtomicReference<>(new ArrayList<>());

        this.loadBalancer().executeNow(l -> servers.set(l.servers()));

        return servers.get();
    }

    @Override
    public boolean isLocked(@NotNull Server server) {
        AtomicBoolean valid = new AtomicBoolean(false);
        this.loadBalancer().executeNow(l -> valid.set(l.isLocked(server)));
        return valid.get();
    }

    @Override
    public Player.Connection.Request connect(Player player) {
        try {
            FamilyPreJoinEvent event = new FamilyPreJoinEvent(RC.P.Families().find(this.id).orElseThrow(), player);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
            if(canceled) return Player.Connection.Request.failedRequest(player, Component.text(event.canceledMessage()));
        } catch (Exception ignore) {}

        try {
            return this.loadBalancer().access().get(20, TimeUnit.SECONDS).current().orElseThrow().connect(player);
        } catch (Exception ignore) {
            return Player.Connection.Request.failedRequest(player, Component.text("The server you're attempting to access isn't available! Try again later."));
        }
    }

    @Override
    public void close() {
        this.plugins.forEach((k, v) -> v.close());
    }

    @Override
    public @NotNull String description() {
        return "Provides state-less server collection services.";
    }

    public static class Tinder extends Particle.Tinder<ScalarFamily> {
        private final String id;
        private final String displayName;
        private final String parent;
        private final Particle.Tinder<LoadBalancer> loadBalancer;

        public Tinder(
                @NotNull String id,
                @Nullable String displayName,
                @Nullable String parent,
                Particle.Tinder<LoadBalancer> loadBalancer
        ) {
            this.id = id;
            this.displayName = displayName;
            this.parent = parent;
            this.loadBalancer = loadBalancer;
        }

        @Override
        public @NotNull ScalarFamily ignite() throws Exception {
            return new ScalarFamily(
                    this.id,
                    this.displayName,
                    this.parent,
                    this.loadBalancer.flux()
            );
        }
    }
}
