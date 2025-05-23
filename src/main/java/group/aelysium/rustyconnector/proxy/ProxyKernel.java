package group.aelysium.rustyconnector.proxy;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Server;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

public class ProxyKernel extends RCKernel<ProxyAdapter> {
    public ProxyKernel(
            @NotNull String id,
            @NotNull ProxyAdapter adapter,
            @NotNull Path directory,
            @NotNull Path modulesDirectory
    ) throws Exception {
        super(id, adapter, directory, modulesDirectory);
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
    public @NotNull Server registerServer(@NotNull Flux<Family> familyFlux, @NotNull Server.Configuration configuration) throws CancellationException, NoSuchElementException, IllegalStateException {
        FamilyRegistry familyRegistry = RC.P.Families();
        Server server;
        try {
            server = familyRegistry.ServerGenerators()
                    .fetch(configuration.metadata().get("serverGenerator").toString())
                    .apply(configuration);
        } catch (NullPointerException ignore) {
            server = new Server(
                    configuration.id(),
                    configuration.address(),
                    configuration.metadata(),
                    configuration.timeout()
            );
        }
        
        Family family = null;
        try {
            family = familyFlux.get(10, TimeUnit.SECONDS);
            
            try {
                ServerRegisterEvent event = new ServerRegisterEvent(familyFlux.orElseThrow(), configuration);
                boolean canceled = RC.P.EventManager().fireEvent(event).get(1, TimeUnit.MINUTES);
                if (canceled) throw new CancellationException(event.canceledMessage());
            } catch (Exception ignore) {}
            
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
        RC.P.Adapter().unregisterServer(server);

        Family family = server.family().orElse(null);
        
        if(family != null) family.removeServer(server);
        
        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server, family));
    }

    @Override
    public @Nullable Component details() {
        int families = RC.P.Families().size();
        int servers = RC.P.Servers().size();
        int players = RC.P.Players().onlinePlayers().size();

        return join(
            newlines(),
            RC.Lang("rustyconnector-keyValue").generate("ID", this.id()),
            RC.Lang("rustyconnector-keyValue").generate("Modules Installed", this.modules.size()),
            RC.Lang("rustyconnector-keyValue").generate("Family", families),
            RC.Lang("rustyconnector-keyValue").generate("Servers", servers),
            RC.Lang("rustyconnector-keyValue").generate("Online Players", players),
            space(),
            text("Extra Properties:", DARK_GRAY),
            (
                this.metadata().isEmpty() ?
                    text("There are no properties to show.", DARK_GRAY)
                    :
                    join(
                        newlines(),
                        this.metadata().entrySet().stream().map(e -> RC.Lang("rustyconnector-keyValue").generate(e.getKey(), e.getValue())).toList()
                    )
            )
        );
    }

//                LoadBalancerAlgorithmExchange.registerAlgorithm(RoundRobin.algorithm, settings -> new RoundRobin.Builder(
//        settings.weighted(),
//        settings.persistence(),
//        settings.attempts(),
//        settings.rebalance()
//        ));
//                LoadBalancerAlgorithmExchange.registerAlgorithm(MostConnection.algorithm, settings -> new MostConnection.Builder(
//        settings.weighted(),
//        settings.persistence(),
//        settings.attempts(),
//        settings.rebalance()
//        ));
//                LoadBalancerAlgorithmExchange.registerAlgorithm(LeastConnection.algorithm, settings -> new LeastConnection.Builder(
//        settings.weighted(),
//        settings.persistence(),
//        settings.attempts(),
//        settings.rebalance()
//        ));
}
