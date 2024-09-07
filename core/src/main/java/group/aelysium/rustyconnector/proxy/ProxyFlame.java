package group.aelysium.rustyconnector.proxy;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.proxy.family.Families;
import group.aelysium.rustyconnector.proxy.family.load_balancing.*;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.proxy.lang.ProxyLang;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.player.Players;
import group.aelysium.rustyconnector.proxy.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;

public class ProxyFlame implements Particle {
    private final UUID uuid;
    private final Version version;
    private final ProxyAdapter adapter;
    private final Particle.Flux<? extends LangLibrary<? extends ProxyLang>> lang;
    private final Particle.Flux<? extends Whitelist> whitelist;
    private final Particle.Flux<? extends Families> families;
    private final Particle.Flux<? extends MagicLinkCore.Proxy> magicLink;
    private final Particle.Flux<? extends Players> players;
    private final Particle.Flux<? extends EventManager> eventManager;
    private final List<Component> bootOutput;

    protected ProxyFlame(
            @NotNull UUID uuid,
            @NotNull ProxyAdapter adapter,
            @NotNull Particle.Flux<? extends LangLibrary<? extends ProxyLang>> lang,
            @Nullable Particle.Flux<? extends Whitelist> whitelist,
            @NotNull List<Component> bootOutput,
            @NotNull Particle.Flux<? extends Families> families,
            @NotNull Particle.Flux<? extends MagicLinkCore.Proxy> magicLink,
            @NotNull Particle.Flux<? extends Players> players,
            @NotNull Particle.Flux<? extends EventManager> eventManager
    ) throws RuntimeException {
        this.uuid = uuid;
        this.adapter = adapter;
        this.lang = lang;
        this.whitelist = whitelist;
        this.bootOutput = bootOutput;
        this.families = families;
        this.magicLink = magicLink;
        this.players = players;
        this.eventManager = eventManager;
        try {
            LoadBalancerAlgorithmExchange.registerAlgorithm(RoundRobin.algorithm, RoundRobin.Tinder::new);
            LoadBalancerAlgorithmExchange.registerAlgorithm(MostConnection.algorithm, MostConnection.Tinder::new);
            LoadBalancerAlgorithmExchange.registerAlgorithm(LeastConnection.algorithm, LeastConnection.Tinder::new);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            try (InputStream input = ProxyFlame.class.getClassLoader().getResourceAsStream("version.txt")) {
                if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
                String stringVersion = new String(input.readAllBytes());
                this.version = new Version(stringVersion);
            }

            this.lang.access().get();
            this.families.access().get();
            this.magicLink.access().get();
            this.players.access().get();
            this.eventManager.access().get();
            try {
                assert this.whitelist != null;
                this.whitelist.access().get();
            } catch (Exception ignore) {}
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the uuid of this Proxy.
     * The Proxy's uuid shouldn't change between re-boots unless you delete the files on the Proxy.
     * @return {@link UUID}
     */
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * Gets the current version of RustyConnector
     * @return {@link Version}
     */
    public Version version() {
        return this.version;
    }

    public ProxyAdapter Adapter() {
        return this.adapter;
    }
    public Particle.Flux<? extends LangLibrary<? extends ProxyLang>> Lang() {
        return this.lang;
    }
    public Optional<Particle.Flux<? extends Whitelist>> Whitelist() {
        return Optional.ofNullable(this.whitelist);
    }
    public Particle.Flux<? extends Families> Families() {
        return this.families;
    }
    public Particle.Flux<? extends MagicLinkCore.Proxy> MagicLink() {
        return this.magicLink;
    }
    public Particle.Flux<? extends Players> Players() {
        return this.players;
    }

    public Particle.Flux<? extends EventManager> EventManager() {
        return this.eventManager;
    }

    public List<Component> bootLog() { return this.bootOutput; }
    public void close() {
        this.families.close();
        this.magicLink.close();
        this.players.close();
        this.eventManager.close();
        this.bootOutput.clear();
    }

    /**
     * Provides a declarative method by which you can establish a new Proxy instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends Particle.Tinder<ProxyFlame> {
        private final UUID uuid;
        private final ProxyAdapter adapter;
        private Particle.Tinder<? extends LangLibrary<? extends ProxyLang>> lang = LangLibrary.Tinder.DEFAULT_PROXY_CONFIGURATION;
        private Particle.Tinder<? extends Whitelist> whitelist = null;
        private Particle.Tinder<? extends Families> families = Families.Tinder.DEFAULT_CONFIGURATION;
        private final Particle.Tinder<? extends MagicLinkCore.Proxy> magicLink;
        private Particle.Tinder<? extends Players> players = Players.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull ProxyAdapter adapter,
                @NotNull Particle.Tinder<? extends MagicLinkCore.Proxy> magicLink
                ) {
            super();
            this.uuid = uuid;
            this.adapter = adapter;
            this.magicLink = magicLink;
        }

        public Tinder lang(@NotNull Particle.Tinder<? extends LangLibrary<? extends ProxyLang>> lang) {
            this.lang = lang;
            return this;
        }

        public Tinder whitelist(@Nullable Particle.Tinder<? extends Whitelist> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public Tinder families(@NotNull Particle.Tinder<? extends Families> families) {
            this.families = families;
            return this;
        }

        public Tinder players(@NotNull Particle.Tinder<? extends Players> players) {
            this.players = players;
            return this;
        }

        public Tinder eventManager(@NotNull Particle.Tinder<? extends EventManager> eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        public @NotNull ProxyFlame ignite() throws RuntimeException {
            return new ProxyFlame(
                    this.uuid,
                    this.adapter,
                    this.lang.flux(),
                    this.whitelist == null ? null : this.whitelist.flux(),
                    new ArrayList<>(),
                    this.families.flux(),
                    this.magicLink.flux(),
                    this.players.flux(),
                    this.eventManager.flux()
            );
        }
    }
}
