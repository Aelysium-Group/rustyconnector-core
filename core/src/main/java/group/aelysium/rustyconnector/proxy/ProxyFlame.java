package group.aelysium.rustyconnector.proxy;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.proxy.family.Families;
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
    private final Particle.Flux<LangLibrary<ProxyLang>> lang;
    private final Particle.Flux<Whitelist> whitelist;
    private final Particle.Flux<Families> families;
    private final Particle.Flux<WebSocketMagicLink> magicLink;
    private final Particle.Flux<Players> players;
    private final Particle.Flux<EventManager> eventManager;
    private final List<Component> bootOutput;

    protected ProxyFlame(
            @NotNull UUID uuid,
            @NotNull ProxyAdapter adapter,
            @NotNull Particle.Flux<LangLibrary<ProxyLang>> lang,
            @Nullable Particle.Flux<Whitelist> whitelist,
            @NotNull List<Component> bootOutput,
            @NotNull Particle.Flux<Families> families,
            @NotNull Particle.Flux<WebSocketMagicLink> magicLink,
            @NotNull Particle.Flux<Players> players,
            @NotNull Particle.Flux<EventManager> eventManager
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
    public Particle.Flux<LangLibrary<ProxyLang>> Lang() {
        return this.lang;
    }
    public Optional<Particle.Flux<Whitelist>> Whitelist() {
        return Optional.ofNullable(this.whitelist);
    }
    public Particle.Flux<Families> Families() {
        return this.families;
    }
    public Particle.Flux<WebSocketMagicLink> MagicLink() {
        return this.magicLink;
    }
    public Particle.Flux<Players> Players() {
        return this.players;
    }

    public Particle.Flux<EventManager> EventManager() {
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

    public static class Tinder extends Particle.Tinder<ProxyFlame> {
        private UUID uuid = UUID.randomUUID();
        private ProxyAdapter adapter;
        private LangLibrary.Tinder<ProxyLang> lang = LangLibrary.Tinder.DEFAULT_PROXY_CONFIGURATION;
        private Whitelist.Tinder whitelist = null;
        private Families.Tinder families = Families.Tinder.DEFAULT_CONFIGURATION;
        private WebSocketMagicLink.Tinder magicLink = null;
        private Players.Tinder players = Players.Tinder.DEFAULT_CONFIGURATION;
        private EventManager.Tinder eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;

        public Tinder() {
            super();
        }

        public Tinder uuid(@NotNull UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Tinder adapter(@NotNull ProxyAdapter adapter) {
            this.adapter = adapter;
            return this;
        }

        public Tinder lang(@NotNull LangLibrary.Tinder<ProxyLang> lang) {
            this.lang = lang;
            return this;
        }

        public Tinder whitelist(@Nullable Whitelist.Tinder whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public Tinder families(@NotNull Families.Tinder families) {
            this.families = families;
            return this;
        }

        public Tinder magicLink(@NotNull WebSocketMagicLink.Tinder magicLink) {
            this.magicLink = magicLink;
            return this;
        }

        public Tinder players(@NotNull Players.Tinder players) {
            this.players = players;
            return this;
        }

        public Tinder eventManager(@NotNull EventManager.Tinder eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        public @NotNull ProxyFlame ignite() throws RuntimeException {
            if(this.adapter == null) throw new IllegalArgumentException("ProxyFlame requires that you set the proxy adapter using ProxyFlame#Tinder#adapter()");
            if(this.magicLink == null) this.magicLink = WebSocketMagicLink.Tinder.DEFAULT_CONFIGURATION(this.uuid);

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
