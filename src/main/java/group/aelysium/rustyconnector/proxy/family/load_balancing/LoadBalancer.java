package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.common.util.MetadataHolder;
import group.aelysium.rustyconnector.proxy.events.*;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

public abstract class LoadBalancer implements Server.Container, MetadataHolder<Object>, Module {
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService executor;
    protected boolean weighted;
    protected boolean persistence;
    protected int attempts;
    protected int index = 0;
    protected Vector<Server> unlockedServers = new Vector<>();
    protected Vector<Server> lockedServers = new Vector<>();
    protected Map<String, Server> servers = new ConcurrentHashMap<>();
    protected Runnable sorter = () -> {
        try {
            Server p = this.unlockedServers.getFirst();
            if(p == null) p = this.lockedServers.getFirst();
            RC.P.EventManager().fireEvent(new FamilyRebalanceEvent(p.family().orElseThrow()));
        } catch (Exception ignore) {}

        this.completeSort();
    };

    protected LoadBalancer(
        boolean weighted,
        boolean persistence,
        int attempts,
        @Nullable LiquidTimestamp rebalance,
        @NotNull Map<String, Object> metadata
    ) {
        this.weighted = weighted;
        this.persistence = persistence;
        this.attempts = attempts;
        this.metadata.putAll(metadata);

        if(rebalance == null) this.executor = null;
        else {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.schedule(this.sorter, rebalance.value(), rebalance.unit());
        }
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

        if(this.index >= this.unlockedServers.size())
            this.index = 0;
        
        return Optional.of(this.unlockedServers.get(this.index));
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
    public void persistence(boolean persistence, int attempts) {
        this.persistence = persistence;
        this.attempts = attempts;
    }

    /**
     * Set whether the load balancer is weighted.
     * @param weighted Whether the load balancer is weighted.
     */
    public void weighted(boolean weighted) {
        this.weighted = weighted;
    }

    /**
     * Resets the index of the load balancer.
     */
    public void resetIndex() {
        this.index = 0;
    }

    @Override
    public Optional<Server> availableServer() {
        return this.current();
    }

    @Override
    public void addServer(@NotNull Server server) {
        this.unlockedServers.add(server);
        this.servers.put(server.id(), server);
    }

    @Override
    public void removeServer(@NotNull Server server) {
        if(!this.servers.containsKey(server.id())) return;
        if(!this.unlockedServers.remove(server))
            this.lockedServers.remove(server);
        this.servers.remove(server.id());
    }

    @Override
    public Optional<Server> fetchServer(@NotNull String id) {
        return Optional.ofNullable(this.servers.get(id));
    }

    @Override
    public boolean containsServer(@NotNull String id) {
        return this.servers.containsKey(id);
    }

    @Override
    public List<Server> servers() {
        return this.servers.values().stream().toList();
    }

    @Override
    public List<Server> lockedServers() {
        return Collections.unmodifiableList(this.lockedServers);
    }

    @Override
    public List<Server> unlockedServers() {
        return Collections.unmodifiableList(this.unlockedServers);
    }

    @Override
    public void lockServer(@NotNull Server server) {
        try {
            boolean canceled = RC.P.EventManager().fireEvent(new ServerLockedEvent(server.family().orElseThrow(), server)).get(1, TimeUnit.MINUTES);
            if(canceled) return;
        } catch (Exception ignore) {}

        if(!this.unlockedServers.remove(server)) return;
        this.lockedServers.add(server);
    }

    @Override
    public void unlockServer(@NotNull Server server) {
        try {
            boolean canceled = RC.P.EventManager().fireEvent(new ServerUnlockedEvent(server.family().orElseThrow(), server)).get(1, TimeUnit.MINUTES);
            if(canceled) return;
        } catch (Exception ignore) {}
        if(!this.lockedServers.remove(server)) return;
        this.unlockedServers.add(server);
    }

    @Override
    public boolean isLocked(@NotNull Server server) {
        return this.lockedServers.contains(server);
    }

    @Override
    public void close() {
        this.servers.clear();
        this.unlockedServers.clear();
        this.lockedServers.clear();
        if(this.executor != null) this.executor.shutdownNow();
    }

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Algorithm", this.getClass().getSimpleName()),
                RC.Lang("rustyconnector-keyValue").generate("Total Servers", this.servers.size()),
                RC.Lang("rustyconnector-keyValue").generate("Unlocked Servers", this.unlockedServers.size()),
                RC.Lang("rustyconnector-keyValue").generate("Locked Servers", this.lockedServers.size()),
                RC.Lang("rustyconnector-keyValue").generate("Weighted", this.weighted),
                RC.Lang("rustyconnector-keyValue").generate("Persistence", this.persistence ? "Enabled ("+this.attempts+")" : "Disabled"),
                text("Extra Properties:", DARK_GRAY),
                (
                    this.metadata().isEmpty() ?
                        text("There are no properties to show.", DARK_GRAY)
                        :
                        join(
                            newlines(),
                            this.metadata().entrySet().stream().map(e -> RC.Lang("rustyconnector-keyValue").generate(e.getKey(), e.getValue())).toList()
                        )
                )
        );
    }
    
    public record Config(
        @NotNull String name,
        @NotNull String algorithm,
        boolean weighted,
        boolean persistence,
        int attempts,
        @Nullable LiquidTimestamp rebalance,
        @NotNull Map<String, Object> metadata
    ) {}
}
