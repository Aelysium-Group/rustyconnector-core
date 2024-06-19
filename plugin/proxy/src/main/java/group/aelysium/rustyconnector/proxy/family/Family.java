package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.toolkit.RustyConnector;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.proxy.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.proxy.player.IPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public abstract class Family implements IFamily {
    protected final String id;
    protected final IFamilyConnector connector;
    protected final String displayName;
    protected final String parent;

    protected Family(
            @NotNull String id,
            @NotNull IFamilyConnector connector,
            String displayName,
            String parent
    ) {
        this.id = id;
        this.connector = connector;
        this.displayName = displayName;
        this.parent = parent;
    }

    public String id() {
        return this.id;
    }
    public String displayName() {
        return this.displayName;
    }
    public Optional<Flux<IWhitelist>> whitelist() {
        return this.connector.whitelist();
    }

    public IFamilyConnector connector() {
        return this.connector;
    }

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     */
    public Optional<Flux<IFamily>> parent() {
        if(this.parent == null) return Optional.empty();
        try {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().find(this.parent);
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Family that = (Family) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public Request connect(IPlayer player) {
        return this.connector.connect(player);
    }

    @Override
    public void leave(IPlayer player) {
        this.connector.leave(player);
    }

    @Override
    public void close() throws Exception {
        this.connector.close();
    }

    public record Settings(
            String id,
            String displayName,
            ILoadBalancer.Settings loadBalancer,
            String parent,
            IWhitelist.Settings whitelist
    ) {}
}
