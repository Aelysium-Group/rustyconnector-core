package group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family;

import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface IScalarFamily extends IFamily<IScalarFamily.Connector> {
    class Connector implements IFamilyConnector {
        protected final Flux<IWhitelist> whitelist;
        protected final Flux<ILoadBalancer> loadBalancer;

        public Connector(@NotNull Flux<ILoadBalancer> loadBalancer, @Nullable Flux<IWhitelist> whitelist) {
            this.loadBalancer = loadBalancer;
            this.whitelist = whitelist;
        }

        @Override
        public Optional<Flux<IWhitelist>> whitelist() {
            return Optional.ofNullable(this.whitelist);
        }

        public Flux<ILoadBalancer> loadBalancer() {
            return this.loadBalancer;
        }

        @Override
        public Request connect(IPlayer player) {
            if(this.whitelist != null)
                try {
                    IWhitelist w = this.whitelist.access().get(10, TimeUnit.SECONDS);
                    if(!w.validate(player))
                        return IPlayerConnectable.failedRequest(player, Component.text(w.message()));
                } catch (Exception ignore) {}

            try {
                return this.loadBalancer.access().get(20, TimeUnit.SECONDS).connect(player);
            } catch (Exception ignore) {
                return IPlayerConnectable.failedRequest(player, Component.text("The server you're attempting to access isn't available! Try again later."));
            }
        }

        @Override
        public void leave(IPlayer player) {}

        @Override
        public void close() throws Exception {
            this.loadBalancer.close();
            try {
                assert this.whitelist != null;
                this.whitelist.close();
            } catch (Exception ignore) {}
        }
    }
}
