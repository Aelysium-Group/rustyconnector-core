package group.aelysium.rustyconnector.core.proxy.family;

import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public abstract class Family<Connector extends IFamilyConnector> implements IFamily<Connector> {
    protected final String id;
    protected final Connector connector;
    protected final String displayName;
    protected final String parent;

    protected Family(
            @NotNull String id,
            @NotNull Connector connector,
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

    public Connector connector() {
        return this.connector;
    }

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     */
    public Optional<Flux<IFamily<?>>> parent() {
        // Needs to fetch the parent from the Family Service
        return this.settings.parent();
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
