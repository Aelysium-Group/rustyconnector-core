package group.aelysium.rustyconnector.proxy.family.scalar_family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.proxy.events.FamilyPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LeastConnection;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.proxy.family.load_balancing.MostConnection;
import group.aelysium.rustyconnector.proxy.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scalar Families are the built-in stateless Family example.
 * They provide an example for how families should be implemented.
 */
public class ScalarFamily extends Family {
    protected final Particle.Flux<LoadBalancer> loadBalancer;

    protected ScalarFamily(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent,
            @Nullable Particle.Flux<Whitelist> whitelist,
            @NotNull Particle.Flux<LoadBalancer> loadBalancer
    ) {
        super(id, displayName, parent, whitelist);
        this.loadBalancer = loadBalancer;
    }

    public Particle.Flux<LoadBalancer> loadBalancer() {
        return this.loadBalancer;
    }

    @Override
    public @NotNull Server generateServer(@NotNull UUID uuid, @NotNull InetSocketAddress address, @Nullable String podName, @Nullable String displayName, int softPlayerCap, int hardPlayerCap, int weight, int timeout) {
        AtomicReference<Server> server = new AtomicReference<>();
        this.loadBalancer.executeNow(l -> server.set(l.generateServer(uuid, address, podName, displayName, softPlayerCap, hardPlayerCap, weight, timeout)));
        return server.get();
    }

    @Override
    public void deleteServer(@NotNull Server server) {
        this.loadBalancer.executeNow(l -> l.deleteServer(server));
    }

    @Override
    public boolean containsServer(@NotNull Server server) {
        AtomicBoolean value = new AtomicBoolean(false);
        this.loadBalancer.executeNow(l -> value.set(l.containsServer(server)));
        return value.get();
    }

    @Override
    public void lockServer(@NotNull Server server) {
        this.loadBalancer.executeNow(l -> l.lockServer(server));
    }

    @Override
    public void unlockServer(@NotNull Server server) {
        this.loadBalancer.executeNow(l -> l.unlockServer(server));
    }

    @Override
    public List<Server> lockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer.executeNow(l -> value.set(l.lockedServers()));
        return value.get();
    }

    @Override
    public List<Server> unlockedServers() {
        AtomicReference<List<Server>> value = new AtomicReference<>(new ArrayList<>());
        this.loadBalancer.executeNow(l -> value.set(l.unlockedServers()));
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
    public @NotNull Component details() {
        return RC.P.Lang().lang().family(
                this.id(),
                this.parent,
                Map.of(
                        "Players", this.players() + "",
                        "Whitelist", this.whitelist().isPresent() ? this.whitelist().orElseThrow().orElseThrow().name() : "none",
                        "Load Balancer", this.loadBalancer().orElseThrow().getClass().getSimpleName()
                ),
                this.loadBalancer.orElseThrow().servers(),
                Component.text("All Servers.")
        );
    }

    @Override
    public List<Server> servers() {
        AtomicReference<List<Server>> servers = new AtomicReference<>(new ArrayList<>());

        this.loadBalancer.executeNow(l -> servers.set(l.servers()));

        return servers.get();
    }

    @Override
    public boolean isLocked(@NotNull Server server) {
        AtomicBoolean valid = new AtomicBoolean(false);
        this.loadBalancer.executeNow(l -> valid.set(l.isLocked(server)));
        return valid.get();
    }

    @Override
    public Player.Connection.Request connect(Player player) {
        try {
            RC.P.EventManager().fireEvent(new FamilyPreJoinEvent(RC.P.Families().find(this.id).orElseThrow(), player));
        } catch (Exception ignore) {}
        if(this.whitelist != null)
            try {
                Whitelist w = this.whitelist.access().get(10, TimeUnit.SECONDS);
                if(!w.validate(player))
                    return Player.Connection.Request.failedRequest(player, Component.text(w.message()));
            } catch (Exception ignore) {}

        try {
            return this.loadBalancer.access().get(20, TimeUnit.SECONDS).current().orElseThrow().connect(player);
        } catch (Exception ignore) {
            return Player.Connection.Request.failedRequest(player, Component.text("The server you're attempting to access isn't available! Try again later."));
        }
    }

    @Override
    public void close() {
        this.loadBalancer.close();
        try {
            assert this.whitelist != null;
            this.whitelist.close();
        } catch (Exception ignore) {}
    }

    public record Settings(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent,
            @Nullable Whitelist.Settings whitelist,
            @NotNull LoadBalancer.Settings loadBalancer
    ) {}

    public static class Tinder extends Particle.Tinder<ScalarFamily> {
        private final ScalarFamily.Settings settings;

        public Tinder(@NotNull Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull ScalarFamily ignite() throws Exception {
            Particle.Flux<Whitelist> whitelist = null;
            if (settings.whitelist() != null)
                whitelist = (new Whitelist.Tinder(settings.whitelist())).flux();

            Particle.Flux<LoadBalancer> loadBalancer = (switch (settings.loadBalancer().algorithm()) {
                case "ROUND_ROBIN" -> new RoundRobin.Tinder(settings.loadBalancer());
                case "LEAST_CONNECTION" -> new LeastConnection.Tinder(settings.loadBalancer());
                case "MOST_CONNECTION" -> new MostConnection.Tinder(settings.loadBalancer());
                default -> throw new RuntimeException("The id used for "+settings.id()+"'s load balancer is invalid!");
            }).flux();

            return new ScalarFamily(
                    this.settings.id(),
                    this.settings.displayName(),
                    this.settings.parent(),
                    whitelist,
                    loadBalancer
            );
        }
    }
}
