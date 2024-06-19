package group.aelysium.rustyconnector.proxy.family.scalar_family;

import group.aelysium.rustyconnector.proxy.family.load_balancing.LeastConnection;
import group.aelysium.rustyconnector.proxy.family.load_balancing.MostConnection;
import group.aelysium.rustyconnector.proxy.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.proxy.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.scalar_family.IScalarFamily;
import group.aelysium.rustyconnector.toolkit.proxy.family.whitelist.IWhitelist;
import org.jetbrains.annotations.NotNull;

public class RootFamily extends ScalarFamily implements IRootFamily {
    private final boolean catchDisconnectingPlayers;

    public RootFamily(
            @NotNull String id,
            @NotNull IScalarFamily.Connector connector,
            boolean catchDisconnectingPlayers,
            String displayName,
            String parent
    ) throws Exception {
        super(id, connector, displayName, parent);
        this.catchDisconnectingPlayers = catchDisconnectingPlayers;
    }

    @Override
    public boolean catchDisconnectingPlayers() {
        return this.catchDisconnectingPlayers;
    }

    public static class Tinder extends Particle.Tinder<IFamily> {
        private final IRootFamily.Settings settings;

        public Tinder(@NotNull IRootFamily.Settings settings) {
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

            return new RootFamily(
                    this.settings.id(),
                    new IScalarFamily.Connector(loadBalancer, whitelist),
                    this.settings.catchDisconnectingPlayers(),
                    this.settings.displayName(),
                    this.settings.parent()
            );
        }
    }
}
