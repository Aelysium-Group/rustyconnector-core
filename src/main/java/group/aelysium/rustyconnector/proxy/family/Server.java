package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketType;
import group.aelysium.rustyconnector.common.util.MetadataHolder;
import group.aelysium.rustyconnector.proxy.Permission;
import group.aelysium.rustyconnector.proxy.events.ServerPreJoinEvent;
import group.aelysium.rustyconnector.proxy.family.load_balancing.ISortable;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Server implements MetadataHolder<Object>, ISortable, Player.Connectable {
    private final Map<String, Object> metadata = new ConcurrentHashMap<>(Map.of(
            "softCap", 30,
            "hardCap", 40
    ));
    private final String id;
    private final InetSocketAddress address;
    private final AtomicLong playerCount = new AtomicLong(0);
    private final AtomicInteger timeout = new AtomicInteger(15);

    public Server(
            @NotNull String id,
            @NotNull InetSocketAddress address,
            @NotNull Map<String, Object> metadata,
            int timeout
    ) {
        this.id = id;
        this.address = address;
        this.timeout.set(timeout);
        this.metadata.putAll(metadata);
    }
    
    @Override
    public boolean storeMetadata(String propertyName, Object property) {
        if(this.metadata.containsKey(propertyName)) return false;
        this.metadata.put(propertyName, property);
        return true;
    }

    @Override
    public <T> Optional<T> fetchMetadata(String propertyName) {
        return Optional.ofNullable((T) this.metadata.get(propertyName));
    }
    
    @Override
    public void removeMetadata(String propertyName) {
        this.metadata.remove(propertyName);
    }
    
    @Override
    public Map<String, Object> metadata() {
        return Collections.unmodifiableMap(this.metadata);
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
     * Decrease this server's timeout by 1.
     * Once this value equals 0, this server will become stale and player's won't be able to join it anymore.
     * @param amount The amount to decrease by.
     * @return The new timeout value.
     */
    public int decreaseTimeout(int amount) {
        if(amount > 0) amount = amount * -1;
        int newValue = this.timeout.addAndGet(amount);
        if(this.timeout.get() < 0) {
            this.timeout.set(0);
            return 0;
        }

        return newValue;
    }

    /**
     * @return The server's unique ID.
     */
    public @NotNull String id() {
        return this.id;
    }

    /**
     * Convenience method to return the server's display name if it exists.
     */
    public @Nullable String displayName() {
        try {
            return (String) this.metadata.get("displayName");
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * This server's address.
     */
    public @NotNull InetSocketAddress address() {
        return this.address;
    }

    /**
     * Is the server full? Will return `true` if and only if `soft-player-cap` has been reached or surpassed.
     * @return `true` if the server is full. `false` otherwise.
     */
    public boolean full() {
        return this.playerCount.get() >= this.softPlayerCap();
    }

    /**
     * Is the server maxed out? Will return `true` if and only if `hard-player-cap` has been reached or surpassed.
     * @return `true` if the server is maxed out. `false` otherwise.
     */
    public boolean maxed() {
        return this.playerCount.get() >= this.hardPlayerCap();
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
        return (int) Optional.ofNullable(this.metadata.get("softCap")).orElse(30);
    }

    /**
     * The hard player cap of this server.
     * If this value is reached by {@link Server#players()}, {@link Server#maxed()} will evaluate to true.
     * The only way for new playerRegistry to continue to join this server once it's maxed is by giving them the hard cap bypass permission.
     * If this value is reached by {@link Server#players()}, it can be assumed that {@link Server#full()} is also true, because this value cannot be less than {@link Server#softPlayerCap()}.
     * @return {@link Integer}
     */
    public int hardPlayerCap() {
        return (int) Optional.ofNullable(this.metadata.get("hardCap")).orElse(this.softPlayerCap() + 10);
    }

    /**
     * Is the server registered.
     * If the server is fully registered, then players should be able to connect to it.
     * @return `true` if the server is fully registered. `false` otherwise.
     */
    public boolean registered() {
        if(!RC.P.Adapter().serverExists(this)) return false;
        if(RC.P.Family(this).isEmpty()) return false;
        return true;
    }
    
    /**
     * Get the family this server is associated with.
     * @return An optional containing the Family in a Flux state if it exists. If this server wasn't assigned a family, this will return an empty optional.
     */
    public Optional<Family> family() {
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
            this.family().orElseThrow().lockServer(this);
            return true;
        } catch(Exception e) {
            RC.Error(Error.from(e).whileAttempting("To lock the server "+this.id()));
        }
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
            this.family().orElseThrow().unlockServer(this);
            return true;
        } catch(Exception e) {
            RC.Error(Error.from(e).whileAttempting("To unlock the server "+this.id()));
        }
        return false;
    }

    @Override
    public double sortIndex() {
        return this.playerCount.get();
    }

    @Override
    public int weight() {
        try {
            return (int) this.fetchMetadata("loadBalancer-weight").orElse(0);
        } catch (Exception ignore) {}
        return 0;
    }


    private boolean validatePlayerLimits(Player player) {
        Family family = this.family().orElseThrow();

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
    public Player.Connection.Request connect(Player player, Player.Connection.Power power) {
        try {
            if (!player.online())
                return Player.Connection.Request.failedRequest(player, player.username() + " isn't online.");

            try {
                ServerPreJoinEvent event = new ServerPreJoinEvent(this, player, power);
                boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
                if(canceled) return Player.Connection.Request.failedRequest(player, event.canceledMessage());
            } catch (Exception ignore) {
                return Player.Connection.Request.failedRequest(player, "Connection attempt timed out.");
            }

            if (!this.validatePlayerLimits(player))
                return Player.Connection.Request.failedRequest(player, "The server is currently full. Try again later.");
            
            Player.Connection.Request request = RC.P.Adapter().connectServer(this, player);
            
            try {
                // In environments where player connections are able to move fast enough, this code will allow for the server player count to increase instantly.
                // If this call times out, it's okay because the actual player count will be set the next time the underlying server pings.
                if(request.result().get(1, TimeUnit.SECONDS).connected()) this.playerCount.addAndGet(1);
            } catch (Exception ignore) {}
            
            return request;
        } catch (Exception ignore) {}

        return Player.Connection.Request.failedRequest(player, "Unable to connect you to the server!");
    }
    public Player.Connection.Request connect(Player player) {
        return connect(player, Player.Connection.Power.MINIMAL);
    }

    @Override
    public long players() {
        return this.playerCount.get();
    }

    @Override
    public String toString() {
        return "["+this.displayName()+"]("+this.address()+")";
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(id, server.id());
    }

    public static @NotNull Server generateServer(Configuration configuration) {
        return new Server(
            configuration.id,
            configuration.address,
            configuration.metadata,
            configuration.timeout
        );
    }

    /**
     * Responsible for holding multiple servers.
     */
    public interface Container {
        /**
         * Checks if the container holds the server.
         * @param id The id to check for.
         * @return `true` if the container holds the server. `false` otherwise.
         */
        boolean containsServer(@NotNull String id);

        /**
         * Adds the server to the server container.
         * @param server The server to add.
         */
        void addServer(@NotNull Server server);

        /**
         * Removes the server from the server container.
         * @param server The server to remove.
         */
        void removeServer(@NotNull Server server);

        /**
         * Fetches the specified server from the container.
         * @param id The id to look for.
         * @return An optional containing the server or nothing if one doesn't exist.
         */
        Optional<Server> fetchServer(@NotNull String id);

        /**
         * Gets all servers held by this container, regardless of their state within the actual container.
         * @return All servers in this container. The returned list is immutable.
         */
        List<Server> servers();

        /**
         * Gets all locked servers held by this container.
         * @return All locked servers. The returned list is immutable.
         */
        List<Server> lockedServers();

        /**
         * Gets all unlocked servers held by this container.
         * @return All unlocked servers. The returned list is immutable.
         */
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

        /**
         * Finds an available server that the player can reasonably connect to.
         * This method exists for cases where a caller needs to fetch a server for a potential player connection
         * without actually connecting the player to it.
         * @return A Server if one exists which is available.
         */
        Optional<Server> availableServer();
    }

    public interface Packets {
        @PacketType("RC-SL")
        class Lock extends Packet.Remote {
            public Lock(Packet packet) {
                super(packet);
            }
        }
        @PacketType("RC-SU")
        class Unlock extends Packet.Remote {
            public Unlock(Packet packet) {
                super(packet);
            }
        }
    }

    /**
     * A Server Configuration.
     * Used alongside {@link group.aelysium.rustyconnector.proxy.ProxyKernel#registerServer(Flux, Configuration)} to register new server instances.
     * @param id The unique id for this server - this value must be unique between servers.
     * @param address The connection address for the server.
     * @param timeout The number of seconds that this server needs to refresh or else it'll timeout.
     */
    public record Configuration(
            @NotNull String id,
            @NotNull InetSocketAddress address,
            @NotNull Map<String, Object> metadata,
            int timeout
    ) {}
}