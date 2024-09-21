package group.aelysium.rustyconnector.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.ServerRegistry;
import group.aelysium.rustyconnector.proxy.family.load_balancing.*;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.proxy.lang.ProxyLang;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.proxy.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProxyKernel implements Particle {
    private final UUID uuid;
    private final Version version;
    private final ProxyAdapter adapter;
    private final Particle.Flux<? extends LangLibrary<? extends ProxyLang>> lang;
    private final Particle.Flux<? extends Whitelist> whitelist;
    private final Particle.Flux<? extends FamilyRegistry> familyRegistry;
    private final Particle.Flux<? extends MagicLinkCore.Proxy> magicLink;
    private final Particle.Flux<? extends PlayerRegistry> playerRegistry;
    private final Particle.Flux<? extends EventManager> eventManager;
    private final Particle.Flux<? extends ServerRegistry> serverRegistry;
    private final List<Component> bootOutput;

    protected ProxyKernel(
            @NotNull UUID uuid,
            @NotNull ProxyAdapter adapter,
            @NotNull Particle.Flux<? extends LangLibrary<? extends ProxyLang>> lang,
            @Nullable Particle.Flux<? extends Whitelist> whitelist,
            @NotNull List<Component> bootOutput,
            @NotNull Particle.Flux<? extends FamilyRegistry> familyRegistry,
            @NotNull Particle.Flux<? extends MagicLinkCore.Proxy> magicLink,
            @NotNull Particle.Flux<? extends PlayerRegistry> playerRegistry,
            @NotNull Particle.Flux<? extends EventManager> eventManager,
            @NotNull Particle.Flux<? extends ServerRegistry> serverRegistry
    ) throws RuntimeException {
        this.uuid = uuid;
        this.adapter = adapter;
        this.lang = lang;
        this.whitelist = whitelist;
        this.bootOutput = bootOutput;
        this.familyRegistry = familyRegistry;
        this.magicLink = magicLink;
        this.playerRegistry = playerRegistry;
        this.eventManager = eventManager;
        this.serverRegistry = serverRegistry;

        try {
            try (InputStream input = ProxyKernel.class.getClassLoader().getResourceAsStream("metadata.json")) {
                if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(new String(input.readAllBytes()), JsonObject.class);
                this.version = new Version(object.get("version").getAsString());
            }

            this.lang.access().get();
            this.familyRegistry.access().get();
            this.playerRegistry.access().get();
            this.eventManager.access().get();
            this.magicLink.access().get();
            this.serverRegistry.access().get();
            try {
                this.adapter.log(Component.text("Booting proxy whitelist..."));
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
    public Particle.Flux<? extends FamilyRegistry> FamilyRegistry() {
        return this.familyRegistry;
    }
    public Particle.Flux<? extends MagicLinkCore.Proxy> MagicLink() {
        return this.magicLink;
    }
    public Particle.Flux<? extends PlayerRegistry> PlayerRegistry() {
        return this.playerRegistry;
    }
    public Particle.Flux<? extends EventManager> EventManager() {
        return this.eventManager;
    }
    public Particle.Flux<? extends ServerRegistry> ServerRegistry() {
        return this.serverRegistry;
    }

    /**
     * Registers a server to the Proxy.
     * Other methods in the codebase have similar names to this one.
     * However, they individually only accomplish a small part of the job.
     * If you're attempting to simply register a server to RustyConnector, this is the method you should use.
     * Once you use this method, assuming it completes successfully, you can assume that the server is completely registered and you don't need to do anything else to register the server.
     * @param familyFlux The family to register the server into.
     * @param configuration The server configuration to use when generating the server.
     * @return The generated server if successfully generated and registered.
     * @throws CancellationException If the registration was canceled.
     * @throws IllegalStateException IF there was an issue registering the server.
     * @throws NoSuchElementException If the provided family flux doesn't resolve within a few seconds.
     */
    public @NotNull Server registerServer(@NotNull Flux<? extends Family> familyFlux, @NotNull Server.Configuration configuration) throws CancellationException, NoSuchElementException, IllegalStateException {
        try {
            ServerRegisterEvent event = new ServerRegisterEvent(familyFlux, configuration);
            boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
            if (canceled) throw new CancellationException(event.canceledMessage());
        } catch (Exception ignore) {}

        if(!RC.P.Adapter().registerServer(configuration))
            throw new IllegalStateException("The server failed to register to the proxy software running the RustyConnector kernel.");


        Family family = null;
        Server server = null;
        try {
            family = familyFlux.access().get(10, TimeUnit.SECONDS);
            server = family.generateServer(configuration);

            RC.P.ServerRegistry().register(server);
        } catch (CancellationException | TimeoutException e) {
            if(server != null) {
                family.removeServer(server);
                RC.P.Adapter().unregisterServer(server);
                RC.P.ServerRegistry().unregister(server);
            }

            throw new CancellationException(e.getMessage());
        } catch (Exception e) {
            if(server != null) {
                family.removeServer(server);
                RC.P.Adapter().unregisterServer(server);
                RC.P.ServerRegistry().unregister(server);
            }

            throw new IllegalStateException(e);
        }

        return server;
    }

    /**
     * Unregisters the server from the proxy.
     * Other methods in the codebase have similar names to this one.
     * However, they individually only accomplish a small part of the job.
     * If you're attempting to simply unregister a server from RustyConnector, this is the method you should use.
     * Once you use this method, assuming it completes successfully, you can assume that the server is completely unregistered and you don't need to do anything else to unregister the server.
     * @param server The server to unregister.
     */
    public void unregister(@NotNull Server server) {
        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));

        RC.P.Adapter().unregisterServer(server);

        RC.P.ServerRegistry().unregister(server);

        server.family().ifPresent(flux -> flux.executeNow(family -> family.removeServer(server)));
    }

    public List<Component> bootLog() { return this.bootOutput; }
    public void close() {
        this.familyRegistry.close();
        this.magicLink.close();
        this.playerRegistry.close();
        this.eventManager.close();
        this.bootOutput.clear();
    }

    /**
     * Provides a declarative method by which you can establish a new Proxy instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends Particle.Tinder<ProxyKernel> {
        private final UUID uuid;
        private final ProxyAdapter adapter;
        private Particle.Tinder<? extends LangLibrary<? extends ProxyLang>> lang = LangLibrary.Tinder.DEFAULT_PROXY_CONFIGURATION;
        private Particle.Tinder<? extends Whitelist> whitelist = null;
        private Particle.Tinder<? extends FamilyRegistry> familyRegistry = FamilyRegistry.Tinder.DEFAULT_CONFIGURATION;
        private final Particle.Tinder<? extends MagicLinkCore.Proxy> magicLink;
        private Particle.Tinder<? extends PlayerRegistry> playerRegistry = PlayerRegistry.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends ServerRegistry> serverRegistry = ServerRegistry.Tinder.DEFAULT_CONFIGURATION;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull ProxyAdapter adapter,
                @NotNull Particle.Tinder<? extends MagicLinkCore.Proxy> magicLink
                ) {
            super();
            this.uuid = uuid;
            this.adapter = adapter;
            this.magicLink = magicLink;

            try {
                LoadBalancerAlgorithmExchange.registerAlgorithm(RoundRobin.algorithm, RoundRobin.Tinder::new);
                LoadBalancerAlgorithmExchange.registerAlgorithm(MostConnection.algorithm, MostConnection.Tinder::new);
                LoadBalancerAlgorithmExchange.registerAlgorithm(LeastConnection.algorithm, LeastConnection.Tinder::new);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Tinder lang(@NotNull Particle.Tinder<? extends LangLibrary<? extends ProxyLang>> lang) {
            this.lang = lang;
            return this;
        }

        public Tinder whitelist(@Nullable Particle.Tinder<? extends Whitelist> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public Tinder familyRegistry(@NotNull Particle.Tinder<? extends FamilyRegistry> familyRegistry) {
            this.familyRegistry = familyRegistry;
            return this;
        }

        public Tinder playerRegistry(@NotNull Particle.Tinder<? extends PlayerRegistry> playerRegistry) {
            this.playerRegistry = playerRegistry;
            return this;
        }

        public Tinder eventManager(@NotNull Particle.Tinder<? extends EventManager> eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        public Tinder serverRegistry(@NotNull Particle.Tinder<? extends ServerRegistry> serverRegistry) {
            this.serverRegistry = serverRegistry;
            return this;
        }

        public @NotNull ProxyKernel ignite() throws RuntimeException {
            return new ProxyKernel(
                    this.uuid,
                    this.adapter,
                    this.lang.flux(),
                    this.whitelist == null ? null : this.whitelist.flux(),
                    new ArrayList<>(),
                    this.familyRegistry.flux(),
                    this.magicLink.flux(),
                    this.playerRegistry.flux(),
                    this.eventManager.flux(),
                    this.serverRegistry.flux()
            );
        }
    }
}
