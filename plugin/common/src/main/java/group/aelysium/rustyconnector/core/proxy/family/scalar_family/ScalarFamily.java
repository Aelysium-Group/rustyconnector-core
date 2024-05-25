package group.aelysium.rustyconnector.core.proxy.family.scalar_family;

import group.aelysium.rustyconnector.core.proxy.family.load_balancing.LeastConnection;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.MostConnection;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.core.proxy.family.Family;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IScalarFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.core.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public class ScalarFamily extends Family implements IScalarFamily {
    public ScalarFamily(
            @NotNull String id,
            @NotNull IScalarFamily.Connector connector,
            String displayName,
            String parent
    ) throws Exception {
        super(id, connector, displayName, parent);
    }

    public static class Tinder extends Particle.Tinder<IFamily> {
        private final IScalarFamily.Settings settings;

        public Tinder(@NotNull IScalarFamily.Settings settings) {
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

            return new ScalarFamily(
                    this.settings.id(),
                    new IScalarFamily.Connector(loadBalancer, whitelist),
                    this.settings.displayName(),
                    this.settings.parent()
            );
        }
    }
}
