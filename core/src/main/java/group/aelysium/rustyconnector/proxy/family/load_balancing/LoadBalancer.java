package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.proxy.events.FamilyRebalanceEvent;
import group.aelysium.rustyconnector.proxy.events.ServerLockedEvent;
import group.aelysium.rustyconnector.proxy.events.ServerUnlockedEvent;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class LoadBalancer implements Server.Factory, Particle {
    protected final ScheduledExecutorService executor;
    protected boolean weighted;
    protected boolean persistence;
    protected int attempts;
    protected int index = 0;
    protected Vector<Server> unlockedServers = new Vector<>();
    protected Vector<Server> lockedServers = new Vector<>();
    protected Map<UUID, Server> servers = new ConcurrentHashMap<>();
    protected Runnable sorter = () -> {
        try {
            Server p = this.unlockedServers.get(0);
            if(p == null) p = this.lockedServers.get(0);
            RC.P.EventManager().fireEvent(new FamilyRebalanceEvent(p.family()));
        } catch (Exception ignore) {}

        this.completeSort();
    };

    protected LoadBalancer(boolean weighted, boolean persistence, int attempts, @Nullable LiquidTimestamp rebalance) {
        this.weighted = weighted;
        this.persistence = persistence;
        this.attempts = attempts;

        if(rebalance == null) this.executor = null;
        else {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.schedule(this.sorter, rebalance.value(), rebalance.unit());
        }
    }

    /**
     * Is the load balancer persistent?
     * @return `true` if the load balancer is persistent. `false` otherwise.
     */
    public boolean persistent() {
        return this.persistence;
    }

    /**
     * Is the load balancer weighted?
     * @return `true` if the load balancer is weighted. `false` otherwise.
     */
    public boolean weighted() {
        return this.weighted;
    }

    /**
     * Get the number of attempts that persistence will make.
     * @return The number of attempts.
     */
    public int attempts() {
        if(!this.persistent()) return 0;
        return this.attempts;
    }

    /**
     * Get the item that the iterator is currently pointing to.
     * Once this returns an item, it will automatically iterate to the next item.
     *
     * @return The item.
     */
    public Optional<Server> current() {
        if(this.unlockedServers.isEmpty()) return Optional.empty();

        Server item;
        if(this.index >= this.unlockedServers.size()) {
            this.index = 0;
            item = this.unlockedServers.get(this.index);
        } else item = this.unlockedServers.get(this.index);

        return Optional.of(item);
    }

    /**
     * Get the index number of the currently selected item.
     * @return The current index.
     */
    public int index() {
        return this.index;
    }

    /**
     * Iterate to the next item.
     * Some conditions might apply causing it to not truly iterate.
     */
    public void iterate() {
        this.index += 1;
        if(this.index >= this.unlockedServers.size()) this.index = 0;
    }

    /**
     * No matter what, iterate to the next item.
     */
    final public void forceIterate() {
        this.index += 1;
        if(this.index >= this.unlockedServers.size()) this.index = 0;
    }

    /**
     * Sort the entire load balancer's contents.
     * Also resets the index to 0.
     */
    public abstract void completeSort();

    /**
     * Sort only one index into a new position.
     * The index chosen is this.index.
     * Also resets the index to 0.
     */
    public abstract void singleSort();

    /**
     * Set the persistence of the load balancer.
     * @param persistence The persistence.
     * @param attempts The number of attempts that persistence will try to connect a player before quiting. This value doesn't matter if persistence is set to `false`
     */
    public void setPersistence(boolean persistence, int attempts) {
        this.persistence = persistence;
        this.attempts = attempts;
    }

    /**
     * Set whether the load balancer is weighted.
     * @param weighted Whether the load balancer is weighted.
     */
    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    /**
     * Resets the index of the load balancer.
     */
    public void resetIndex() {
        this.index = 0;
    }

    /**
     * Attempt to fetch a "good enough" Server for a potential player connection.
     * @return The Server.
     */
    public Optional<Server> staticFetch() {
        return this.current();
    }

    @Override
    public @NotNull Server generateServer(@NotNull UUID uuid, @NotNull InetSocketAddress address, @Nullable String podName, @Nullable String displayName, int softPlayerCap, int hardPlayerCap, int weight, int timeout) {
        return null;
    }

    @Override
    public void deleteServer(@NotNull Server server) {
        if(!this.servers.containsKey(server.uuid())) return;
        if(!this.unlockedServers.remove(server))
            this.lockedServers.remove(server);
        this.servers.remove(server.uuid());
    }

    @Override
    public boolean containsServer(@NotNull Server server) {
        return this.servers.containsKey(server.uuid());
    }

    @Override
    public List<Server> servers() {
        return this.servers.values().stream().toList();
    }

    @Override
    public List<Server> lockedServers() {
        return this.lockedServers.stream().toList();
    }

    @Override
    public List<Server> unlockedServers() {
        return this.unlockedServers.stream().toList();
    }

    @Override
    public void lockServer(@NotNull Server server) {
        if(!this.unlockedServers.remove(server)) return;
        this.lockedServers.add(server);

        RC.P.EventManager().fireEvent(new ServerLockedEvent(server.family(), server));
    }

    @Override
    public void unlockServer(@NotNull Server server) {
        if(!this.lockedServers.remove(server)) return;
        this.unlockedServers.add(server);

        RC.P.EventManager().fireEvent(new ServerUnlockedEvent(server.family(), server));
    }

    @Override
    public boolean isLocked(@NotNull Server server) {
        return false;
    }

    @Override
    public void close() throws Exception {
        this.servers.clear();
        this.unlockedServers.clear();
        this.lockedServers.clear();
        this.executor.shutdownNow();
    }

    public record Settings(
            String algorithm,
            boolean weighted,
            boolean persistence,
            int attempts,
            LiquidTimestamp rebalance
    ) {}
}
