package group.aelysium.rustyconnector.plugin.velocity.lib.family.mcloader;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.server.IServerService;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerService extends IServerService {
    protected final Map<UUID, IMCLoader> servers = new ConcurrentHashMap<>();
    private final int serverTimeout;
    private final int serverInterval;

    protected ServerService(int serverTimeout, int serverInterval) {
        this.serverTimeout = serverTimeout;
        this.serverInterval = serverInterval;
    }

    public int serverTimeout() {
        return this.serverTimeout;
    }

    public int serverInterval() {
        return this.serverInterval;
    }

    public void add(IMCLoader mcLoader) {
        this.servers.put(mcLoader.uuid(), mcLoader);
    }
    public void remove(IMCLoader mcLoader) {
        this.servers.remove(mcLoader.uuid());
    }

    public Optional<IMCLoader> fetch(UUID uuid) {
        IMCLoader loader = this.servers.get(uuid);
        if(loader == null) return Optional.empty();
        return Optional.of(loader);
    }

    public List<IMCLoader> servers() {
        return this.servers.values().stream().toList();
    }

    public boolean contains(UUID uuid) {
        return this.servers.containsKey(uuid);
    }

    /**
     * Registers fake servers into the proxy to help with testing systems.
     */
    /*public void registerFakeServers() {
        Tinder api = Tinder.get();
        PluginLogger logger = api.logger();

        for (Family family : api.services().family().dump()) {
            logger.log("---| Starting on: " + family.id());
            // Register 1000 servers into each family
            for (int i = 0; i < 1000; i++) {
                InetSocketAddress address = AddressUtil.stringToAddress("localhost:"+i);
                String name = "fakeSRV-"+i;

                MCLoader server = new MCLoader(UUID.randomUUID(), address, name, 40, 50, 0, this.serverTimeout);
                server.setPlayerCount((int) (Math.random() * 50));

                try {
                    RegisteredServer registeredServer = api.velocityServer().registerServer(server.serverInfo());
                    server.registeredServer(registeredServer);

                    family.addServer(server);

                    logger.log("-----| Added: " + server.serverInfo() + " to " + family.id());
                } catch (Exception ignore) {}
            }
        }
    }*/

    public void close() throws Exception {
        this.servers.clear();
    }

    public static class Tinder extends Particle.Tinder<ServerService> {
        protected int timeout = 15;
        protected int interval = 10;

        public Tinder() {}

        public void serverTimeout(int timeout) {
            this.timeout = timeout;
        }

        public void serverInterval(int interval) {
            this.interval = interval;
        }

        @Override
        public @NotNull ServerService ignite() throws Exception {
            return new ServerService(timeout, interval);
        }
    }
}
