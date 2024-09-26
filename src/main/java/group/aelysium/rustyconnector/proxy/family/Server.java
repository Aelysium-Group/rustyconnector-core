package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.Permission;
import group.aelysium.rustyconnector.proxy.events.ServerPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.load_balancing.ISortable;
import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Server implements ISortable, Player.Connectable {
    private final UUID uuid;
    private String registration;
    private final String displayName;
    private final String podName;
    private final InetSocketAddress address;
    private Object raw = null;
    private final AtomicLong playerCount = new AtomicLong(0);
    private int weight;
    private int softPlayerCap;
    private int hardPlayerCap;
    private AtomicInteger timeout;

    public Server(
            @NotNull UUID uuid,
            @NotNull InetSocketAddress address,
            @Nullable String podName,
            @Nullable String displayName,
            int softPlayerCap,
            int hardPlayerCap,
            int weight,
            int timeout
    ) {
        this.uuid = uuid;
        this.address = address;
        this.podName = podName;
        this.displayName = displayName;

        this.weight = Math.max(weight, 0);

        this.softPlayerCap = softPlayerCap;
        this.hardPlayerCap = hardPlayerCap;

        // Soft player cap MUST be at most the same value as hard player cap.
        if(this.softPlayerCap > this.hardPlayerCap) this.softPlayerCap = this.hardPlayerCap;

        this.timeout = new AtomicInteger(timeout);
    }

    /**
     * The registration of this server. Can be null if this server isn't registered anymore.
     * @param registration The registration for this server.
     */
    public void registration(String registration) {
        if(registration.length() > 32) throw new IllegalArgumentException("The server registration must not be longer than 32 characters.");
        this.registration = registration;
    }

    /**
     * Checks if the server is stale.
     * @return {@link Boolean}
     */
    public boolean stale() {
        return this.timeout.get() <= 0;
    }

    /**
     * Set's the server's new timeout.
     * @param newTimeout The new timeout.
     */
    public void setTimeout(int newTimeout) {
        if(newTimeout < 0) throw new IndexOutOfBoundsException("New timeout must be at least 0!");
        this.timeout.set(newTimeout);
    }

    /**
     * The {@link UUID} of this server.
     * This {@link UUID} will always be different between servers.
     * If this server unregisters and then re-registers into the proxy, this ID will be different.
     * @return {@link UUID}
     */
    public @NotNull UUID uuid() {
        return this.uuid;
    }

    /**
     * The registration of the server.
     * This is a unique combination of the server's family's name and a NanoID.<br/>
     * Ex. `familyName-nf3nFX3965`.<br/><br/>
     * While the server uuid exists for the lifetime of the server, the registration name will change
     * any time the server unregisters and re-registers.
     * This is also the value used by certain proxy pieces of software for referencing the server as it's
     * more user-friendly than using an uuid.
     * @return The server registration.
     */
    public @NotNull String registration() {
        return this.registration;
    }

    /**
     * Convenience method to return the server's display name if it exists.
     * If none exists, it will return the server's UUID in string format.
     */
    public @NotNull String uuidOrDisplayName() {
        if(displayName == null) return this.uuid.toString();
        return this.displayName;
    }

    /**
     * Gets this server's pod name if it exists.
     * If your RC network isn't a part of a Kubernetes cluster, this will always return an empty optional.
     * @return {@link Optional<String>}
     */
    public @NotNull Optional<String> podName() {
        if(this.podName == null) return Optional.empty();
        return Optional.of(this.podName);
    }

    /**
     * Decrease this server's timeout by 1.
     * Once this value equals 0, this server will become stale and player's won't be able to join it anymore.
     * @param amount The amount to decrease by.
     * @return The new timeout value.
     */
    public int decreaseTimeout(int amount) {
        if(amount > 0) amount = amount * -1;
        this.timeout.addAndGet(amount);
        if(this.timeout.get() < 0) this.timeout.set(0);

        return this.timeout.get();
    }

    /**
     * This server's address.
     */
    public @NotNull InetSocketAddress address() {
        return this.address;
    }

    /**
     * Gets the raw server that backs this server.
     */
    public @NotNull Object raw() {
        return this.raw;
    }

    /**
     * Is the server full? Will return `true` if and only if `soft-player-cap` has been reached or surpassed.
     * @return `true` if the server is full. `false` otherwise.
     */
    public boolean full() {
        return this.playerCount.get() >= softPlayerCap;
    }

    /**
     * Is the server maxed out? Will return `true` if and only if `hard-player-cap` has been reached or surpassed.
     * @return `true` if the server is maxed out. `false` otherwise.
     */
    public boolean maxed() {
        return this.playerCount.get() >= hardPlayerCap;
    }

    /**
     * Set the player count for this server.
     * This number will directly impact whether new playerRegistry can join this server based on server soft and hard caps.
     * The number set here will be overwritten the next time this server syncs with the proxy.
     * @param playerCount The player count.
     */
    public void setPlayerCount(long playerCount) {
        this.playerCount.set(playerCount);
    }

    /**
     * The soft player cap of this server.
     * If this value is reached by {@link Server#players()}, {@link Server#full()} will evaluate to true.
     * The only way for new playerRegistry to continue to join this server once it's full is by giving them the soft cap bypass permission.
     * @return {@link Integer}
     */
    public int softPlayerCap() {
        return this.softPlayerCap;
    }

    /**
     * The hard player cap of this server.
     * If this value is reached by {@link Server#players()}, {@link Server#maxed()} will evaluate to true.
     * The only way for new playerRegistry to continue to join this server once it's maxed is by giving them the hard cap bypass permission.
     *
     * If this value is reached by {@link Server#players()}, it can be assumed that {@link Server#full()} is also true, because this value cannot be less than {@link Server#softPlayerCap()}.
     * @return {@link Integer}
     */
    public int hardPlayerCap() {
        return this.hardPlayerCap;
    }

    /**
     * Is the server registered.
     * If the server is fully registered, then players should be able to connect to it.
     * @return `true` if the server is fully registered. `false` otherwise.
     */
    public boolean registered() {
        if(!RC.P.Adapter().serverExists(this)) return false;
        if(RC.P.ServerRegistry().find(this.uuid()).isEmpty()) return false;
        if(RC.P.Family(this).isEmpty()) return false;
        return true;
    }

    /**
     * Get the family this server is associated with.
     * @return An optional containing the Family in a Flux state if it exists. If this server wasn't assigned a family, this will return an empty optional.
     */
    public Optional<Particle.Flux<? extends Family>> family() {
        return RC.P.Family(this);
    }

    /**
     * Locks the specific server in its respective family so that the load balancer won't return it for playerRegistry to connect to.
     * If the server is already locked, or doesn't exist in the load balancer, nothing will happen.
     * <br/>
     * This is a convenience method that will fetch this server's family and run {@link Family#lockServer(Server)}.
     * @return `true` if completed successfully. `false` otherwise.
     */
    public boolean lock() {
        try {
            this.family().orElseThrow().executeNow(f -> f.lockServer(this));
            return true;
        } catch(Exception ignore) {}
        return false;
    }

    /**
     * Unlocks the specific server in its respective family so that the load balancer can return it for playerRegistry to connect to.
     * If the server is already unlocked, or doesn't exist in the load balancer, nothing will happen.
     * <br/>
     * This is a convenience method that will fetch this server's family and run {@link Family#lockServer(Server)}.
     * @return `true` if completed successfully. `false` otherwise.
     */
    public boolean unlock() {
        try {
            this.family().orElseThrow().executeNow(f -> f.unlockServer(this));
            return true;
        } catch(Exception ignore) {}
        return false;
    }

    @Override
    public double sortIndex() {
        return this.playerCount.get();
    }

    @Override
    public int weight() {
        return this.weight;
    }


    private boolean validatePlayerLimits(Player player) throws ExecutionException, InterruptedException, TimeoutException {
        Family family = this.family().orElseThrow().access().get(10, TimeUnit.SECONDS);

        if(Permission.validate(
                player,
                "rustyconnector.hardCapBypass",
                Permission.constructNode("rustyconnector.<family id>.hardCapBypass",family.id())
        )) return true; // If the player has permission to bypass hard-player-cap, let them in.

        if(this.maxed()) return false; // If the player count is at hard-player-cap. Boot the player.

        if(Permission.validate(
                player,
                "rustyconnector.softCapBypass",
                Permission.constructNode("rustyconnector.<family id>.softCapBypass",family.id())
        )) return true; // If the player has permission to bypass soft-player-cap, let them in.

        return !this.full();
    }

    @Override
    public Player.Connection.Request connect(Player player) {
        try {
            ServerPreJoinEvent event = new ServerPreJoinEvent(this, player);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
            if(canceled) return Player.Connection.Request.failedRequest(player, Component.text(event.canceledMessage()));
        } catch (Exception ignore) {
            return Player.Connection.Request.failedRequest(player, Component.text("Connection attempt timed out."));
        }
        try {
            if (!player.online())
                return Player.Connection.Request.failedRequest(player, Component.text(player.username() + " isn't online."));

            if (!this.validatePlayerLimits(player))
                return Player.Connection.Request.failedRequest(player, Component.text("The server is currently full. Try again later."));

            return RC.P.Adapter().connectServer(this, player);
        } catch (Exception ignore) {}

        return Player.Connection.Request.failedRequest(player, Component.text("Unable to connect you to the server!"));
    }

    @Override
    public long players() {
        return 0;
    }

    @Override
    public String toString() {
        return "["+this.uuidOrDisplayName()+"]("+this.address()+")";
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(uuid, server.uuid());
    }

    public static @NotNull Server generateServer(Configuration configuration) {
        return new Server(
                configuration.uuid,
                configuration.address,
                configuration.podName,
                configuration.displayName,
                configuration.softPlayerCap,
                configuration.hardPlayerCap,
                configuration.weight,
                configuration.timeout
        );
    }

    /**
     * Responsible for holding multiple servers.
     */
    public interface Container {
        boolean containsServer(@NotNull Server server);

        void addServer(@NotNull Server server);
        void removeServer(@NotNull Server server);

        List<Server> servers();
        List<Server> lockedServers();
        List<Server> unlockedServers();

        /**
         * Locks the specific server.
         * If a server is locked, player's shouldn't be able to connect to it directly.
         * @param server The server to lock. If the server isn't a member of this factory, nothing will happen.
         */
        void lockServer(@NotNull Server server);

        /**
         * Unlocks the specific server.
         * If a server is unlocked, player's should be able to connect to it directly.
         * @param server The server to unlock. If the server isn't a member of this factory, nothing will happen.
         */
        void unlockServer(@NotNull Server server);

        /**
         * Checks if the specified server is locked or not.
         * @param server The server to check.
         * @return `true` if the server is locked. `false` if the server is unlocked. This method also returns `false` if the server simply doesn't exist.
         */
        boolean isLocked(@NotNull Server server);
    }

    public interface Packets {
        class Lock extends Packet.Wrapper {
            public Lock(Packet packet) {
                super(packet);
            }
        }
        class Unlock extends Packet.Wrapper {
            public Unlock(Packet packet) {
                super(packet);
            }
        }
    }

    /**
     * A Server Configuration.
     * Used alongside {@link group.aelysium.rustyconnector.proxy.ProxyKernel#registerServer(Particle.Flux, Configuration)} to register new server instances.
     * @param uuid The unique UUID for this server this value must be unique between servers.
     * @param address The connection address for the server.
     * @param podName The name of the pod that this server resides in.
     * @param displayName The display name of this server.
     * @param softPlayerCap The soft player cap for this server.
     * @param hardPlayerCap The hard player cap for this server.
     * @param weight The weight of this server in load balancing.
     * @param timeout The number of seconds that this server needs to refresh or else it'll timeout.
     */
    public record Configuration(
            @NotNull UUID uuid,
            @NotNull InetSocketAddress address,
            @Nullable String podName,
            @Nullable String displayName,
            int softPlayerCap,
            int hardPlayerCap,
            int weight,
            int timeout
    ) {}
}