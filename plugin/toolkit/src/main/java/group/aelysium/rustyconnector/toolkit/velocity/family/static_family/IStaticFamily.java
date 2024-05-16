package group.aelysium.rustyconnector.toolkit.velocity.family.static_family;

import group.aelysium.rustyconnector.toolkit.velocity.connection.ConnectionResult;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.UnavailableProtocol;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static group.aelysium.rustyconnector.toolkit.velocity.family.UnavailableProtocol.*;

public interface IStaticFamily extends IFamily {
    /**
     * Gets the {@link UnavailableProtocol} for this family. {@link UnavailableProtocol} governs what happens when a player's resident server is unavailable.
     * @return {@link UnavailableProtocol}
     */
    UnavailableProtocol unavailableProtocol();

    /**
     * Gets the {@link LiquidTimestamp resident server expiration} for this family.
     * Server expiration governs how long a server counts as a {@link com.velocitypowered.api.proxy.Player player's} residence before it no longer counts for that player.
     * You can set this option to save resources on your network.
     * For example, you can set an expiration of 30 days, after that time if a player doesn't join this family, their record will be removed.
     * @return {@link LiquidTimestamp}
     */
    LiquidTimestamp homeServerExpiration();

    record Settings(
            @NotNull String id,
            @NotNull ILoadBalancer.Settings loadBalancer,
            @NotNull UnavailableProtocol unavailableProtocol,
            @NotNull LiquidTimestamp homeServerExpiration,
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
            this.loadBalancer.executeNow(l -> l.add(mcloader));
        }

        @Override
        public void unregister(IMCLoader mcloader) {
            this.loadBalancer.executeNow(l -> l.remove(mcloader));
        }

        @Override
        public void lock(IMCLoader mcloader) {
            this.loadBalancer.executeNow(l -> l.lock(mcloader));
        }

        @Override
        public void unlock(IMCLoader mcloader) {
            this.loadBalancer.executeNow(l -> l.unlock(mcloader));
        }
        @Override
        public long players() {
            AtomicLong count = new AtomicLong(0);

            this.loadBalancer().executeNow(l ->
                    l.servers().forEach(s -> count.addAndGet(s.playerCount()))
            );

            return count.get();
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
            CompletableFuture<ConnectionResult> result = new CompletableFuture<>();
            Request request = new Request(player, result);
            try {
                try {
                    Optional<IServerResidence.MCLoaderEntry> residenceOptional = this.storage.database().residences().get(family, player);

                    // Set new residence if none exists
                    if (residenceOptional.isEmpty()) {
                        request = this.connector.connect(player);

                        if(!request.result().get().connected()) return request;

                        IMCLoader server = request.result().get().server().orElseThrow();

                        this.storage.database().residences().set(family, server, player);

                        return request;
                    }

                    IServerResidence.MCLoaderEntry residence = residenceOptional.orElseThrow();

                    ConnectionResult result1 = residence.server().connect(player).result().get(10, TimeUnit.SECONDS);
                    if(!result1.connected()) throw new NoOutputException();

                    return residence.server().connect(player);
                } catch (NoOutputException ignore) {}

                switch (family.unavailableProtocol()) {
                    case ASSIGN_NEW_HOME -> {
                        request = DEFAULT_CONNECTOR.connect(player);

                        if(!request.result().get().connected()) return request;

                        IMCLoader server = request.result().get().server().orElseThrow();

                        this.storage.database().residences().set(family, server, player);

                        return request;
                    }
                    case CONNECT_WITH_ERROR -> {
                        Request tempRequest = DEFAULT_CONNECTOR.handleSingletonConnect(loadBalancer, player);

                        if(!tempRequest.result().get().connected()) return tempRequest;

                        result.complete(new ConnectionResult(ConnectionResult.Status.SUCCESS, ProxyLang.MISSING_HOME_SERVER, tempRequest.result().get().server()));

                        return request;
                    }
                    case CANCEL_CONNECTION_ATTEMPT -> {
                        result.complete(ConnectionResult.failed(ProxyLang.BLOCKED_STATIC_FAMILY_JOIN_ATTEMPT));
                        return request;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.complete(ConnectionResult.failed(Component.text("There was an issue connecting you to the server!")));
            return request;
        }

        @Override
        public void leave(IPlayer player) {

        }

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