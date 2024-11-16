package group.aelysium.rustyconnector.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.load_balancing.*;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import group.aelysium.rustyconnector.proxy.util.Version;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProxyKernel extends RCKernel<ProxyAdapter> {
    protected ProxyKernel(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull ProxyAdapter adapter,
            List<? extends Flux<? extends Plugin>> plugins
    ) {
        super(uuid, version, adapter, plugins);
    }

    /**
     * Registers a server to the Proxy.<br/>
     * This method is the central source of truth for registering servers.<br/>
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
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }

        Server server = Server.generateServer(configuration);

        Family family = null;
        try {
            family = familyFlux.access().get(10, TimeUnit.SECONDS);
            family.addServer(server);

            if(!RC.P.Adapter().registerServer(server))
                throw new IllegalStateException("The server failed to register to the proxy software running the RustyConnector kernel.");

        } catch (CancellationException | TimeoutException e) {
            if(family != null) family.removeServer(server);
            RC.P.Adapter().unregisterServer(server);

            throw new CancellationException(e.getMessage());
        } catch (Exception e) {
            if(family != null) family.removeServer(server);
            RC.P.Adapter().unregisterServer(server);

            throw new IllegalStateException(e);
        }

        return server;
    }

    /**
     * Unregisters the server from the proxy.<br/>
     * This method is the central source of truth for registering servers.<br/>
     * Other methods in the codebase have similar names to this one.
     * However, they individually only accomplish a small part of the job.
     * If you're attempting to simply unregisterServer a server from RustyConnector, this is the method you should use.
     * Once you use this method, assuming it completes successfully, you can assume that the server is completely unregistered and you don't need to do anything else to unregisterServer the server.
     * @param server The server to unregister.
     */
    public void unregisterServer(@NotNull Server server) {
        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));

        RC.P.Adapter().unregisterServer(server);

        server.family().ifPresent(flux -> flux.executeNow(family -> family.removeServer(server)));
    }

    /**
     * Provides a declarative method by which you can establish a new Proxy instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends Particle.Tinder<ProxyKernel> {
        private final UUID uuid;
        private final ProxyAdapter adapter;
        private Particle.Tinder<? extends LangLibrary> lang = LangLibrary.Tinder.DEFAULT_LANG_LIBRARY;
        private Particle.Tinder<? extends FamilyRegistry> familyRegistry = FamilyRegistry.Tinder.DEFAULT_CONFIGURATION;
        private final Particle.Tinder<? extends MagicLinkCore.Proxy> magicLink;
        private Particle.Tinder<? extends PlayerRegistry> playerRegistry = PlayerRegistry.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends ErrorRegistry> errors = ErrorRegistry.Tinder.DEFAULT_CONFIGURATION;

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

        public Tinder lang(@NotNull Particle.Tinder<? extends LangLibrary> lang) {
            this.lang = lang;
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

        public Tinder errorHandler(@NotNull Particle.Tinder<? extends ErrorRegistry> errorHandler) {
            this.errors = errorHandler;
            return this;
        }

        public @NotNull ProxyKernel ignite() throws Exception {
            Version version;
            try (InputStream input = ProxyKernel.class.getClassLoader().getResourceAsStream("metadata.json")) {
                if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(new String(input.readAllBytes()), JsonObject.class);
                version = new Version(object.get("version").getAsString());
            }
            return new ProxyKernel(
                    this.uuid,
                    version,
                    this.adapter,
                    List.of(
                        this.lang.flux(),
                        this.familyRegistry.flux(),
                        this.magicLink.flux(),
                        this.playerRegistry.flux(),
                        this.eventManager.flux(),
                        this.errors.flux()
                    )
            );
        }
    }
}
