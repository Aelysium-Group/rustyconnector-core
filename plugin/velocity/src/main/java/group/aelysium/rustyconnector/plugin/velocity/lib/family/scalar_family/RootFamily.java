package group.aelysium.rustyconnector.plugin.velocity.lib.family.scalar_family;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IScalarFamily;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing.LeastConnection;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing.MostConnection;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
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

    public static class Tinder extends Particle.Tinder<IRootFamily> {
        private final IRootFamily.Settings settings;

        public Tinder(@NotNull IRootFamily.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull RootFamily ignite() throws Exception {
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

    public static class Connector extends Family.Connector.Core {
        protected final Family.Connector.Core connector;

        protected Connector(@NotNull LoadBalancer loadBalancer, IWhitelist.Reference whitelist) {
            super(loadBalancer, whitelist);

            if(loadBalancer.persistent() && loadBalancer.attempts() > 1)
                this.connector = new ScalarConnector.Persistent(loadBalancer, whitelist);
            else
                this.connector = new ScalarConnector.Singleton(loadBalancer, whitelist);
        }

        @Override
        public Request connect(IPlayer player) {
            return this.connector.connect(player);
        }
    }
}
