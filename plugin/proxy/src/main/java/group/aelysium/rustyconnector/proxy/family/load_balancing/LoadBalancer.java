package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.FamilyRebalanceEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.MCLoaderLockedEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.MCLoaderUnlockedEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import org.checkerframework.checker.optional.qual.OptionalPropagator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class LoadBalancer implements ILoadBalancer {
    protected final ScheduledExecutorService executor;
    private boolean weighted;
    private boolean persistence;
    private int attempts;
    protected int index = 0;
    protected Vector<IMCLoader> unlockedServers = new Vector<>();
    protected Vector<IMCLoader> lockedServers = new Vector<>();
    protected Map<UUID, IMCLoader> mcloaders = new ConcurrentHashMap<>();
    protected Runnable sorter = () -> {
        try {
            IMCLoader p = this.unlockedServers.get(0);
            if(p == null) p = this.lockedServers.get(0);
            RC.P.EventManager().fireEvent(new FamilyRebalanceEvent(p.family()));
        } catch (Exception ignore) {}

        this.completeSort();
    };

    public LoadBalancer(boolean weighted, boolean persistence, int attempts, @Nullable LiquidTimestamp rebalance) {
        this.weighted = weighted;
        this.persistence = persistence;
        this.attempts = attempts;

        if(rebalance == null) this.executor = null;
        else {
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.executor.schedule(this.sorter, rebalance.value(), rebalance.unit());
        }
    }

    public boolean persistent() {
        return this.persistence;
    }

    public int attempts() {
        if(!this.persistent()) return 0;
        return this.attempts;
    }

    public boolean weighted() {
        return this.weighted;
    }

    public Optional<IMCLoader> current() {
        if(this.size(false) == 0) return Optional.empty();

        IMCLoader item;
        if(this.index >= this.size()) {
            this.index = 0;
            item = this.unlockedServers.get(this.index);
        } else item = this.unlockedServers.get(this.index);

        return Optional.of(item);
    }

    public int index() {
        return this.index;
    }

    public void iterate() {
        this.index += 1;
        if(this.index >= this.unlockedServers.size()) this.index = 0;
    }

    final public void forceIterate() {
        this.index += 1;
        if(this.index >= this.unlockedServers.size()) this.index = 0;
    }

    public abstract void completeSort();

    public abstract void singleSort();

    public void add(@NotNull IMCLoader item) {
        if(this.mcloaders.containsKey(item.uuid())) return;
        this.unlockedServers.add(item);
        this.mcloaders.put(item.uuid(), item);
    }

    public void remove(@NotNull IMCLoader item) {
        if(!this.mcloaders.containsKey(item.uuid())) return;
        if(!this.unlockedServers.remove(item))
            this.lockedServers.remove(item);
        this.mcloaders.remove(item.uuid());
    }

    public int size() {
        return this.mcloaders.size();
    }

    public int size(boolean locked) {
        if(locked) return this.lockedServers.size();
        return this.unlockedServers.size();
    }

    public List<IMCLoader> servers() {
        return mcloaders.values().stream().toList();
    }
    public List<IMCLoader> openServers() {
        return this.unlockedServers.stream().toList();
    }
    public List<IMCLoader> lockedServers() {
        return this.lockedServers.stream().toList();
    }

    public String toString() {
        return "LoadBalancer (RoundRobin): "+this.size()+" items";
    }

    public void setPersistence(boolean persistence, int attempts) {
        this.persistence = persistence;
        this.attempts = attempts;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public void resetIndex() {
        this.index = 0;
    }

    public boolean contains(@NotNull IMCLoader item) {
        return this.mcloaders.containsKey(item.uuid());
    }

    public void lock(@NotNull IMCLoader server) {
        if(!this.unlockedServers.remove(server)) return;
        this.lockedServers.add(server);

        RC.P.EventManager().fireEvent(new MCLoaderLockedEvent(server.family(), server));
    }

    public void unlock(@NotNull IMCLoader server) {
        if(!this.lockedServers.remove(server)) return;
        this.unlockedServers.add(server);

        RC.P.EventManager().fireEvent(new MCLoaderUnlockedEvent(server.family(), server));
    }

    public boolean joinable(@NotNull IMCLoader server) {
        return this.unlockedServers.contains(server);
    }

    public Optional<IMCLoader> staticFetch() {
        return this.current();
    }

    @Override
    public void close() throws Exception {
        try {
            if(this.executor == null) throw new RuntimeException();
            this.executor.shutdownNow();
        } catch (Exception ignore) {}
        this.lockedServers.clear();
        this.unlockedServers.clear();
    }
}