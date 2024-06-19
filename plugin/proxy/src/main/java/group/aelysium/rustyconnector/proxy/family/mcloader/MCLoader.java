package group.aelysium.rustyconnector.proxy.family.mcloader;

import group.aelysium.rustyconnector.proxy.Permission;
import group.aelysium.rustyconnector.toolkit.RC;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.toolkit.proxy.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.proxy.family.mcloader.IMCLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MCLoader implements IMCLoader {
    private final UUID uuid;
    private final String displayName;
    private final String podName;
    private final InetSocketAddress address;
    private final Particle.Flux<IFamily> family;
    private Object raw = null;
    private AtomicInteger playerCount = new AtomicInteger(0);
    private int weight;
    private int softPlayerCap;
    private int hardPlayerCap;
    private AtomicInteger timeout;

    public MCLoader(
            @NotNull UUID uuid,
            @NotNull InetSocketAddress address,
            @Nullable String podName,
            @Nullable String displayName,
            @NotNull Particle.Flux<IFamily> family,
            int softPlayerCap,
            int hardPlayerCap,
            int weight,
            int timeout
    ) {
        this.uuid = uuid;
        this.address = address;
        this.podName = podName;
        this.displayName = displayName;
        this.family = family;

        this.weight = Math.max(weight, 0);

        this.softPlayerCap = softPlayerCap;
        this.hardPlayerCap = hardPlayerCap;

        // Soft player cap MUST be at most the same value as hard player cap.
        if(this.softPlayerCap > this.hardPlayerCap) this.softPlayerCap = this.hardPlayerCap;

        this.timeout = new AtomicInteger(timeout);
    }

    public boolean stale() {
        return this.timeout.get() <= 0;
    }

    public void setTimeout(int newTimeout) {
        if(newTimeout < 0) throw new IndexOutOfBoundsException("New timeout must be at least 0!");
        this.timeout.set(newTimeout);
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String uuidOrDisplayName() {
        if(displayName == null) return this.uuid.toString();
        return this.displayName;
    }

    public Optional<String> podName() {
        if(this.podName == null) return Optional.empty();
        return Optional.of(this.podName);
    }

    public int decreaseTimeout(int amount) {
        if(amount > 0) amount = amount * -1;
        this.timeout.addAndGet(amount);
        if(this.timeout.get() < 0) this.timeout.set(0);

        return this.timeout.get();
    }

    public InetSocketAddress address() {
        return this.address;
    }

    @Override
    public Object raw() {
        return this.raw;
    }

    /**
     * Is the server full? Will return `true` if and only if `soft-player-cap` has been reached or surpassed.
     * @return `true` if the server is full
     */
    public boolean full() {
        return this.playerCount.get() >= softPlayerCap;
    }

    /**
     * Is the server maxed out? Will return `true` if and only if `hard-player-cap` has been reached or surpassed.
     * @return `true` if the server is maxed out
     */
    public boolean maxed() {
        return this.playerCount.get() >= hardPlayerCap;
    }

    @Override
    public int playerCount() {
        return this.playerCount.get();
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount.set(playerCount);
    }

    public double sortIndex() {
        return this.playerCount.get();
    }

    @Override
    public int weight() {
        return this.weight;
    }

    @Override
    public int softPlayerCap() {
        return this.softPlayerCap;
    }

    @Override
    public int hardPlayerCap() {
        return this.hardPlayerCap;
    }

    public Particle.Flux<IFamily> family() {
        return this.family;
    }

    public boolean validatePlayerLimits(IPlayer player) {
        IFamily family = this.family.access().get(10, TimeUnit.SECONDS);

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

    public IPlayerConnectable.Request connect(IPlayer player) {
        ProxyAdapter adapter = RC.P.Adapter();
        return adapter.connectServer(this, player);
    }

    @Override
    public void leave(IPlayer player) {
        this.playerCount.decrementAndGet();
    }

    public void lock() {
        this.family.executeNow(f -> f.connector().lock(this));
    }

    public void unlock() {
        this.family.executeNow(f -> f.connector().unlock(this));
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
        MCLoader mcLoader = (MCLoader) o;
        return Objects.equals(uuid, mcLoader.uuid());
    }
}
