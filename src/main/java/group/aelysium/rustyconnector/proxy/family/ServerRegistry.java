package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.magic_link.exceptions.CanceledPacket;
import group.aelysium.rustyconnector.proxy.events.ServerRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.ServerUnregisterEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ServerRegistry implements Particle {
    private final Map<String, UUID> servers = new ConcurrentHashMap<>();

    protected ServerRegistry() {}

    /**
     * Finds a server based on it's uuid.
     * @param uuid The uuid to search for.
     */
    public Optional<Server> find(@NotNull UUID uuid) {
        FamilyRegistry familyRegistry = RC.P.Families();
        AtomicReference<Server> server = new AtomicReference<>(null);
        for (Particle.Flux<? extends Family> family : familyRegistry.dump()) {
            family.executeNow(f -> {
                f.servers().stream().filter(s -> s.uuid().equals(uuid)).findAny().ifPresent(server::set);
            });
            if(server.get() != null) break;
        }

        return Optional.ofNullable(server.get());
    }

    /**
     * Finds a server based on its registration.
     * @param serverRegistration The registration of the server returned by {@link Server#registration()}.
     */
    public Optional<Server> find(@NotNull String serverRegistration) {
        UUID uuid = this.servers.get(serverRegistration);
        if(uuid == null) return Optional.empty();
        return this.find(uuid);
    }

    /**
     * Registers a new server to the registry.
     * This registry exists to track servers inside the RC environment.
     * This method handles a very specific task, you should only use it if you know what you're doing.
     * If you're just trying to create and register a new server to RustyConnector, you should use {@link group.aelysium.rustyconnector.proxy.ProxyKernel#registerServer(Particle.Flux, Server.Configuration)}
     * @param server The server to register.
     */
    public void register(@NotNull Server server) {
        this.servers.put(server.registration(), server.uuid());
    }

    /**
     * Registers a server from the proxy.
     * @param server The server to unregister.
     */
    public void unregister(@NotNull Server server) {
        RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));

        RC.P.Adapter().unregisterServer(server);
        this.servers.remove(server.registration());
    }

    /**
     * Registers a server from the proxy.
     * @param serverRegistration The registration of the server returned by {@link Server#registration()}.
     */
    public void unregister(@NotNull String serverRegistration) {
        try {
            Server server = this.find(serverRegistration).orElseThrow();
            RC.P.EventManager().fireEvent(new ServerUnregisterEvent(server));

            RC.P.Adapter().unregisterServer(server);
            this.servers.remove(serverRegistration);
        } catch (Exception ignore) {}
    }

    public void close() {
        this.servers.forEach((k, v)-> {
            try {
                RC.P.Adapter().unregisterServer(RC.P.Server(v).orElseThrow());
            } catch (Exception ignore) {}
        });
        this.servers.clear();
    }

    public static class Tinder extends Particle.Tinder<ServerRegistry> {
        @Override
        public @NotNull ServerRegistry ignite() throws Exception {
            return new ServerRegistry();
        }

        /**
         * Returns the default configuration for a FamilyRegistry manager.
         * This default configuration has no root family set and no initial familyRegistry loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
