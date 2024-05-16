package group.aelysium.rustyconnector.core.proxy.family.matchmaking;

import com.velocitypowered.api.proxy.Player;
import group.aelysium.rustyconnector.core.common.algorithm.SingleSort;
import group.aelysium.rustyconnector.core.common.exception.NoOutputException;
import group.aelysium.rustyconnector.core.proxy.family.load_balancing.RoundRobin;
import group.aelysium.rustyconnector.core.proxy.family.mcloader.RankedMCLoader;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.core.proxy.family.matchmaking.rank.DefaultRankResolver;
import group.aelysium.rustyconnector.core.proxy.family.matchmaking.rank.RandomizedPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.connection.ConnectionResult;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.ISession;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchmaker;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IVelocityPlayerRank;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.kyori.adventure.text.format.NamedTextColor.GRAY;

public class Matchmaker extends RoundRobin implements IMatchmaker {
    protected final ScheduledExecutorService supervisor = Executors.newScheduledThreadPool(5);
    protected final ScheduledExecutorService queueIndicator = Executors.newSingleThreadScheduledExecutor();
    protected final String gameId;
    protected final IMatchmaker.Settings settings;
    protected final ISession.Settings sessionSettings;
    protected final int minPlayersPerGame;
    protected final int maxPlayersPerGame;
    protected final AtomicInteger failedBuilds = new AtomicInteger(0);
    protected BossBar waitingForPlayers = BossBar.bossBar(
            Component.text("Waiting for players...").color(GRAY),
            (float) 0.0,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );
    protected BossBar waitingForServers = BossBar.bossBar(
            Component.text("Waiting for servers...").color(GRAY),
            (float) 0.0,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );
    protected Map<UUID, ISession> activeSessions = new ConcurrentHashMap<>();
    protected Map<UUID, ISession> queuedSessions = new ConcurrentHashMap<>();
    protected Map<UUID, ISession> sessionPlayers = new ConcurrentHashMap<>();
    protected List<IMatchPlayer> queuedPlayers = Collections.synchronizedList(new ArrayList<>());


    public Matchmaker(@NotNull IMatchmaker.Settings settings, @NotNull String gameId) {
        super(false, false, 0);
        this.gameId = gameId;
        this.settings = settings;
        this.sessionSettings = new ISession.Settings(
                settings.freezeActiveSessions(),
                settings.closingThreshold(),
                settings.max(),
                settings.variance(),
                this.gameId,
                settings.quittersLose(),
                settings.stayersWin()
        );

        this.minPlayersPerGame = settings.min();
        this.maxPlayersPerGame = settings.max();

        this.storage().purgeSchemas(this);

        this.supervisor.schedule(() -> {
            if(this.queuedPlayers.size() < minPlayersPerGame) this.failedBuilds.set(0);

            int i = 0;
            double varianceLookahead = (this.settings.variance() + (this.settings.varianceExpansionCoefficient() * this.failedBuilds.get())) * 2;
            List<IMatchPlayer> selectedPlayers = new ArrayList<>();
            Map<UUID, ISession> sessionMappings = new HashMap<>();
            List<ISession> builtSessions = new ArrayList<>();
            while(i < this.queuedPlayers.size()) {
                IMatchPlayer current = null;
                IMatchPlayer thrown = null;
                try {
                    current = this.queuedPlayers.get(i);
                    thrown = this.queuedPlayers.get(i + (this.minPlayersPerGame - 1));
                } catch (IndexOutOfBoundsException | NoOutputException ignore) {}

                if(current == null || thrown == null) {
                    i = i + this.minPlayersPerGame;
                    continue;
                }

                double varianceMax = (current.gameRank().rank() + varianceLookahead);

                if(thrown.gameRank().rank() > varianceMax) {
                    i = i + this.minPlayersPerGame;
                    continue;
                }

                ISession session = new Session(this, this.sessionSettings);
                for (int j = i; j < i + maxPlayersPerGame; j++) {
                    IMatchPlayer nextInsert = null;
                    try {
                        nextInsert = this.queuedPlayers.get(j);
                    } catch (IndexOutOfBoundsException ignore) {}
                    if(nextInsert == null) break;
                    if(nextInsert.gameRank().rank() > varianceMax) break;
                    session.join(nextInsert);
                }

                if(session.size() < this.settings.min()) {
                    session.empty();
                    i = i + this.minPlayersPerGame;
                    continue;
                }

                if(session.size() < this.settings.max() && this.failedBuilds.get() < this.settings.requiredExpansionsForAccept()) {
                    session.empty();
                    i = i + this.minPlayersPerGame;
                    continue;
                }

                builtSessions.add(session);
                session.players().values().forEach(p -> {
                    selectedPlayers.add(p);
                    sessionMappings.put(p.player().uuid(), session);
                });
                i = i + session.size();
            }

            if(builtSessions.isEmpty() && this.queuedPlayers.size() >= this.minPlayersPerGame) this.failedBuilds.incrementAndGet();
            if(!builtSessions.isEmpty()) this.failedBuilds.set(0);

            this.sessionPlayers.putAll(sessionMappings);
            this.queuedPlayers.removeAll(selectedPlayers);
            builtSessions.forEach(s -> this.queuedSessions.put(s.uuid(), s));

            // Some intentional cleanup
            sessionMappings.clear();
            selectedPlayers.clear();
            builtSessions.clear();
        }, this.settings.sessionDispatchInterval().value(), this.settings.sessionDispatchInterval().unit());

        this.supervisor.schedule(() -> {
            if(this.size(false) == 0) return;

            for (ISession session : this.queuedSessions.values()) {
                if(this.size(false) == 0) return;

                try {
                    if (session.size() < session.settings().min()) {
                        session.implode("There are not enough players to start a game!");
                        continue;
                    }

                    RankedMCLoader server = (RankedMCLoader) this.current().orElseThrow(
                            () -> new RuntimeException("There are no servers to connect to!")
                    );

                    session.start(server);

                    this.queuedSessions.remove(session.uuid());
                    this.activeSessions.put(session.uuid(), session);

                    session.players().values().forEach(matchPlayer -> {
                        try {
                            Player velocityPlayer = matchPlayer.player().resolve().orElseThrow();
                            hideBossBars(velocityPlayer);
                        } catch (Exception ignore) {}
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }, this.settings.sessionDispatchInterval().value(), this.settings.sessionDispatchInterval().unit());

        if(!this.settings.showInfo()) return;
        this.queueIndicator.schedule(() -> {
            // So that we don't lock the Vector while sending messages
            for(IMatchPlayer matchPlayer : this.queuedPlayers.stream().toList()) {
                Player velocityPlayer = matchPlayer.player().resolve().orElseThrow();

                hideBossBars(velocityPlayer);
                velocityPlayer.sendActionBar(Component.text("----< MATCHMAKING >----", NamedTextColor.YELLOW));

                Bossbar.WAITING_FOR_PLAYERS(this.waitingForPlayers, 0, maxPlayersPerGame);
                velocityPlayer.showBossBar(this.waitingForPlayers);
            }
            for (ISession session : this.queuedSessions.values())
                for (IMatchPlayer matchPlayer : session.players().values())
                    try {
                        Player velocityPlayer = matchPlayer.player().resolve().orElseThrow();

                        hideBossBars(velocityPlayer);
                        velocityPlayer.sendActionBar(Component.text("----< MATCHMAKING >----", NamedTextColor.YELLOW));

                        Bossbar.WAITING_FOR_SERVERS(this.waitingForServers, this.size(true), this.size(false));
                        velocityPlayer.showBossBar(this.waitingForServers);
                    } catch (Exception ignore) {}
        }, 5, TimeUnit.SECONDS);
    }

    public IMatchmaker.Settings settings() {
        return this.settings;
    }
    public Database.PlayerRanks storage() {
        return this.storage.database().ranks();
    }
    public String gameId() {
        return this.gameId;
    }

    public Optional<IMatchPlayer> matchPlayer(IPlayer player) {
        Optional<IVelocityPlayerRank> rank = this.storage.database().ranks().get(player, this.gameId, DefaultRankResolver.New());
        return rank.map(r -> new MatchPlayer(player, r, this.gameId));
    }

    public synchronized Request connect(IPlayer player) {
        try {
            IMatchPlayer matchPlayer = this.resolveMatchPlayer(player);

            if(this.queuedPlayers.contains(matchPlayer)) return;
            if(settings.reconnect()) {
                for (ISession s : this.activeSessions.values()) {
                    if(!s.previousPlayers().contains(player.uuid())) continue;
                    this.connectSession(s, matchPlayer);
                    result.complete(ConnectionResult.success(Component.text("Successfully queued into the matchmaker!"), s.mcLoader().orElse(null)));
                    return;
                }
                for (ISession s : this.queuedSessions.values()) {
                    if(!s.previousPlayers().contains(player.uuid())) continue;
                    this.connectSession(s, matchPlayer);
                    result.complete(ConnectionResult.success(Component.text("Successfully queued into the matchmaker!"), null));
                    return;
                }
            } else if(this.sessionPlayers.containsKey(matchPlayer.player().uuid()))  throw new RuntimeException("Player is already queued!");

            int insertIndex = this.queuedPlayers.size();
            this.queuedPlayers.add(insertIndex, matchPlayer);
            SingleSort.sortAsc(this.queuedPlayers, insertIndex);
        } catch (Exception e) {
            result.complete(ConnectionResult.failed(Component.text("There was an issue queuing into matchmaking!")));
            throw new RuntimeException(e);
        }
        result.complete(ConnectionResult.success(Component.text("Successfully queued into the matchmaker!"), null));
    }

    public void leave(IPlayer player) {
        try {
            hideBossBars(player.resolve().orElseThrow());
        } catch (Exception ignore) {}

        IMatchPlayer matchPlayer = this.resolveMatchPlayer(player);

        if(this.queuedPlayers.remove(matchPlayer)) return;

        Session session = ((Session) this.sessionPlayers.get(player.uuid()));
        if(session == null) return;
        session.leave(player);

        this.sessionPlayers.remove(player.uuid());
    }

    public void remove(ISession session) {
        session.players().keySet().forEach(k->this.sessionPlayers.remove(k));
        this.activeSessions.remove(session.uuid());
        this.queuedSessions.remove(session.uuid());
    }

    public boolean contains(IPlayer player) {
        Optional<IMatchPlayer> matchPlayer = this.matchPlayer(player);
        if(matchPlayer.isEmpty()) return false;
        if(this.sessionPlayers.containsKey(player.uuid())) return true;
        return this.queuedPlayers.contains(matchPlayer.get());
    }

    public boolean isQueued(IPlayer player) {
        Optional<IMatchPlayer> matchPlayer = this.matchPlayer(player);
        if(matchPlayer.isEmpty()) return false;
        return this.queuedPlayers.contains(matchPlayer.get());
    }

    public Optional<ISession> fetchPlayersSession(UUID playerUUID) {
        ISession session = this.sessionPlayers.get(playerUUID);
        if(session == null) return Optional.empty();
        return Optional.of(session);
    }

    public Optional<ISession> fetch(UUID sessionUUID) {
        ISession session = this.activeSessions.get(sessionUUID);
        if(session == null) session = this.queuedSessions.get(sessionUUID);
        if(session == null) return Optional.empty();
        return Optional.of(session);
    }

    public void hideBossBars(Player player) {
        player.hideBossBar(this.waitingForPlayers);
        player.hideBossBar(this.waitingForServers);
    }

    public int playerCount() {
        return this.queuedPlayerCount() + this.activePlayerCount();
    }

    public int queuedPlayerCount() {
        AtomicInteger count = new AtomicInteger();

        for (ISession session : this.queuedSessions.values())
            count.addAndGet(session.size());

        count.addAndGet(this.queuedPlayers.size());

        return count.get();
    }

    public int activePlayerCount() {
        AtomicInteger count = new AtomicInteger();

        for (ISession session : this.activeSessions.values())
            count.addAndGet(session.size());

        return count.get();
    }

    public int sessionCount() {
        return this.activeSessions.size() + this.queuedSessions.size();
    }

    public int queuedSessionCount() {
        return this.queuedSessions.size();
    }

    public int activeSessionCount() {
        return this.activeSessions.size();
    }

    public IVelocityPlayerRank newPlayerRank() {
        try {
            return settings.schema().getConstructor().newInstance();
        } catch(Exception e) {
            if(!settings.schema().equals(RandomizedPlayerRank.class)) e.printStackTrace();
            return RandomizedPlayerRank.New();
        }
    }

    /**
     * Attempts to connect the player to the session.
     */
    protected ConnectionResult connectSession(ISession session, IMatchPlayer matchPlayer) throws ExecutionException, InterruptedException, TimeoutException {
        if(!session.matchmaker().equals(this)) throw new RuntimeException("Attempted to connect to a session governed by another matchmaker!");
        ConnectionResult result = session.join(matchPlayer).result().get(5, TimeUnit.SECONDS);

        if(result.connected())
            this.sessionPlayers.put(matchPlayer.player().uuid(), session);

        return result;
    }

    /**
     * Resolves a player rank for the player.
     */
    protected IMatchPlayer resolveMatchPlayer(IPlayer player) {
        IVelocityPlayerRank rank = this.storage.database().ranks().get(player, this.gameId, DefaultRankResolver.New()).orElseGet(()->{
            IVelocityPlayerRank newRank = this.newPlayerRank();

            this.storage.database().ranks().set(new MatchPlayer(player, newRank, this.gameId));

            return newRank;
        });

        return new MatchPlayer(player, rank, this.gameId);
    }

    @Override
    public void close() throws Exception {
        try {
            this.supervisor.shutdownNow();
        } catch (Exception ignore) {}
        try {
            this.queueIndicator.shutdownNow();
        } catch (Exception ignore) {}

        this.queuedSessions.clear();
        this.activeSessions.clear();

        this.queuedPlayers.clear();
        this.sessionPlayers.clear();
    }

    public static class Tinder extends Particle.Tinder<IMatchmaker> {
        private final IMatchmaker.Settings settings;
        private final String gameId;

        public Tinder(@NotNull IMatchmaker.Settings settings, @NotNull String gameId) {
            this.settings = settings;
            this.gameId = gameId;
        }

        @Override
        public @NotNull IMatchmaker ignite() throws Exception {
            return new Matchmaker(this.settings, this.gameId);
        }
    }

    protected interface Bossbar {
        static void WAITING_FOR_PLAYERS(BossBar bossbar, int players, int max) {
            float percentage = (float) players / max;

            BossBar.Color color = BossBar.Color.WHITE;
            if (percentage > 0.5) color = BossBar.Color.YELLOW;
            if (percentage >= 1) color = BossBar.Color.GREEN;

            bossbar.color(color);
            bossbar.progress(percentage);
        }

        static void WAITING_FOR_SERVERS(BossBar bossbar, int closedServers, int openServers) {
            int totalServers = closedServers + openServers;
            float percentage = (float) openServers / totalServers;

            bossbar.color(BossBar.Color.BLUE);
            bossbar.progress(percentage);
        }
    }
}