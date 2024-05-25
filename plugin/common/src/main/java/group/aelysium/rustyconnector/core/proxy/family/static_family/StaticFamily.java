package group.aelysium.rustyconnector.core.proxy.family.static_family;

import group.aelysium.rustyconnector.core.proxy.family.Family;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.LeastConnection;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.MostConnection;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.core.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.UnavailableProtocol;
import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IStaticFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class StaticFamily extends Family implements IStaticFamily {
    protected LiquidTimestamp homeServerExpiration;
    protected UnavailableProtocol unavailableProtocol;

    public StaticFamily(
            @NotNull String id,
            @NotNull UnavailableProtocol unavailableProtocol,
            @NotNull LiquidTimestamp homeServerExpiration,
            @NotNull IStaticFamily.Connector connector,
            String displayName,
            String parent
    ) {
        super(id, connector, displayName, parent);
        this.unavailableProtocol = unavailableProtocol;
        this.homeServerExpiration = homeServerExpiration;
    }

    public UnavailableProtocol unavailableProtocol() {
        return this.unavailableProtocol;
    }

    public LiquidTimestamp homeServerExpiration() {
        return this.homeServerExpiration;
    }

    public static class Tinder extends Particle.Tinder<IFamily> {
        private final IStaticFamily.Settings settings;

        public Tinder(@NotNull IStaticFamily.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull IFamily ignite() throws Exception {
            Flux<IWhitelist> whitelist = null;
            if (settings.whitelist() != null)
                whitelist = (new Whitelist.Tinder(settings.whitelist())).flux();

            Flux<ILoadBalancer> loadBalancer = (switch (settings.loadBalancer().algorithm()) {
                case "ROUND_ROBIN" -> new RoundRobin.Tinder(settings.loadBalancer());
                case "LEAST_CONNECTION" -> new LeastConnection.Tinder(settings.loadBalancer());
                case "MOST_CONNECTION" -> new MostConnection.Tinder(settings.loadBalancer());
                default -> throw new RuntimeException("The id used for "+settings.id()+"'s load balancer is invalid!");
            }).flux();

            StaticFamily family = new StaticFamily(
                    this.settings.id(),
                    this.settings.unavailableProtocol(),
                    this.settings.homeServerExpiration(),
                    new IStaticFamily.Connector(loadBalancer, whitelist),
                    this.settings.displayName(),
                    this.settings.parent()
            );

            try {
                storage.database().residences().refreshExpirations(family);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("There was an issue with MySQL! " + e.getMessage());
            }

            return family;
        }
    }
}