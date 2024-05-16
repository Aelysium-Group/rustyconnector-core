package group.aelysium.rustyconnector.toolkit.velocity.family.ranked_family;

import group.aelysium.rustyconnector.toolkit.velocity.connection.ConnectionResult;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchmaker;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.ISession;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.server.IRankedMCLoader;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public interface IRankedFamily extends IFamily {
    record Settings(
            @NotNull String id,
            @NotNull String gameId,
            @NotNull IMatchmaker.Settings matchmaker,
            String displayName,
            String parent,
            IWhitelist.Settings whitelist
    ) {}

    class Connector implements IFamilyConnector {
        protected final Flux<IWhitelist> whitelist;
        protected final Flux<IMatchmaker> matchmaker;

        public Connector(@NotNull Flux<IMatchmaker> matchmaker, @Nullable Flux<IWhitelist> whitelist) {
            this.matchmaker = matchmaker;
            this.whitelist = whitelist;
        }

        @Override
        public void register(IMCLoader mcloader) {
            this.matchmaker.executeNow(m -> m.add(mcloader));
        }
        @Override
        public void unregister(IMCLoader mcloader) {
            this.matchmaker.executeNow(m -> m.remove(mcloader));
        }
        @Override
        public void lock(IMCLoader mcloader) {
            this.matchmaker.executeNow(m -> m.lock(mcloader));
        }
        @Override
        public void unlock(IMCLoader mcloader) {
            this.matchmaker.executeNow(m -> m.unlock(mcloader));
        }
        @Override
        public long players() {
            AtomicLong count = new AtomicLong(0);

            this.matchmaker().executeNow(m -> {
                m.servers().forEach(s -> count.addAndGet(s.playerCount()));
            });

            return count.get();
        }
        @Override
        public List<IMCLoader> mcloaders() {
            AtomicReference<List<IMCLoader>> mcloaders = new AtomicReference<>(new ArrayList<>());

            this.matchmaker.executeNow(l -> mcloaders.set(l.servers()));

            return mcloaders.get();
        }

        @Override
        public Optional<Flux<IWhitelist>> whitelist() {
            return Optional.ofNullable(this.whitelist);
        }

        public Flux<IMatchmaker> matchmaker() {
            return this.matchmaker;
        }

        @Override
        public Request connect(IPlayer player) {
            CompletableFuture<ConnectionResult> result = new CompletableFuture<>();
            Request request = new Request(player, result);

            this.matchmaker.executeNow(matchmaker -> {
                if(Party.locate(player).isPresent()) {
                    result.complete(ConnectionResult.failed(ProxyLang.RANKED_FAMILY_PARTY_DENIAL.build()));
                    return request;
                }

                // Validate that the player isn't in another matchmaker
                Optional<IRankedFamily> family = Tinder.get().services().family().dump().stream().filter(f -> {
                    if(!(f instanceof IRankedFamily)) return false;
                    return ((IRankedFamily) f).connector().matchmaker.contains(player);
                }).findAny();

                if(family.isPresent()) {
                    IRankedFamily rankedFamily = family.get();
                    AtomicBoolean wasQueued = new AtomicBoolean(false);
                    rankedFamily.connector().matchmaker.executeNow(m -> wasQueued.set(m.isQueued(player)));

                    if(wasQueued.get()) {
                        result.complete(ConnectionResult.failed(ProxyLang.RANKED_FAMILY_IN_MATCHMAKER_QUEUE_DENIAL.build()));
                        return request;
                    }

                    Optional<ISession> session = matchmaker.fetchPlayersSession(player.uuid());
                    if(session.isPresent()) {
                        if(session.get().active()) {
                            result.complete(ConnectionResult.failed(ProxyLang.RANKED_FAMILY_IN_MATCHMAKER_GAME_DENIAL.build()));

                            return request;
                        }
                    }
                }

                matchmaker.connect(player);
            }, () -> {
                result.complete(ConnectionResult.failed(Component.text("There are no available servers to connect to!")));
            });

            return request;
        }

        @Override
        public void leave(IPlayer player) {
            this.matchmaker.executeNow(l -> l.leave(player));
        }

        @Override
        public void close() throws Exception {
            this.matchmaker.close();
            try {
                assert this.whitelist != null;
                this.whitelist.close();
            } catch (Exception ignore) {}
        }
    }
}
