package group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family;

import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public interface IScalarFamily extends IFamily {

    record Settings(
            @NotNull String id,
            @NotNull ILoadBalancer.Settings loadBalancer,
            String displayName,
            String parent,
            IWhitelist.Settings whitelist
    ) {}

    class Connector implements IFamilyConnector {
        protected final Flux<IWhitelist> whitelist;
        protected final Flux<ILoadBalancer> loadBalancer;

        public Connector(@NotNull Flux<ILoadBalancer> loadBalancer, @Nullable Flux<IWhitelist> whitelist) {
            this.loadBalancer = loadBalancer;
            this.whitelist = whitelist;
        }

        @Override
        public void register(IMCLoader mcloader) {
            this.loadBalancer.executeNow(m -> m.add(mcloader));
        }
        @Override
        public void unregister(IMCLoader mcloader) {
            this.loadBalancer.executeNow(m -> m.remove(mcloader));
        }
        @Override
        public void lock(IMCLoader mcloader) {
            this.loadBalancer.executeNow(m -> m.lock(mcloader));
        }
        @Override
        public void unlock(IMCLoader mcloader) {
            this.loadBalancer.executeNow(m -> m.unlock(mcloader));
        }
        @Override
        public long players() {
            AtomicLong output = new AtomicLong(0);
            this.loadBalancer().executeNow(l ->
                    l.servers().forEach(s -> output.addAndGet(s.playerCount()))
            );

            return output.get();
        }
        @Override
        public List<IMCLoader> mcloaders() {
            AtomicReference<List<IMCLoader>> mcloaders = new AtomicReference<>(new ArrayList<>());

            this.loadBalancer.executeNow(l -> mcloaders.set(l.servers()));

            return mcloaders.get();
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
