package group.aelysium.rustyconnector.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.plugins.Plugin;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.util.Version;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ServerKernel extends RCKernel<ServerAdapter> {
    private final String displayName;
    private final InetSocketAddress address;

    protected ServerKernel(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull ServerAdapter adapter,
            @NotNull List<? extends Flux<? extends Plugin>> plugins,
            @Nullable String displayName,
            @NotNull InetSocketAddress address
    ) {
        super(uuid, version, adapter, plugins);
        this.displayName = displayName;
        this.address = address;
    }

    /**
     * The display name of this Server.
     */
    public String displayName() {
        return this.displayName;
    }

    /**
     * Gets the address of this server.
     * The address, assuming the user entered it properly, should be formatted in the same format as you format a joinable address in Velocity's velocity.toml.
     * @return {@link String}
     */
    public InetSocketAddress address() {
        return this.address;
    }

    /**
     * The number of playerRegistry on this server.
     * @return {@link Integer}
     */
    public int playerCount() {
        return this.Adapter().onlinePlayerCount();
    }

    /**
     * Locks this Server so that playerRegistry can't join it via the family's load balancer.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> lock() {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Identification.from("RC","LS"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, new PacketListener<>() {
                    @Override
                    public Packet.Response handle(MagicLinkCore.Packets.Response packet) {
                        response.complete(packet);
                        return Packet.Response.success("Successfully indicated the status of the server's lock request");
                    }
                });
        return response;
    }

    /**
     * Unlocks this Server so that playerRegistry can join it via the family's load balancer.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> unlock() {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Identification.from("RC","US"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, new PacketListener<>() {
                    @Override
                    public Packet.Response handle(MagicLinkCore.Packets.Response packet) {
                        response.complete(packet);
                        return Packet.Response.success("Successfully indicated the status of the server's unlock request");
                    }
                });
        return response;
    }

    /**
     * Sends a player to a specific family if it exists.
     * @param player The uuid of the player to send.
     * @param familyID The id of the family to send to.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> send(UUID player, String familyID) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Identification.from("RC","SP"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER_UUID, player.toString())
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_FAMILY, familyID)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, new PacketListener<>() {
                    @Override
                    public Packet.Response handle(MagicLinkCore.Packets.Response packet) {
                        response.complete(packet);
                        return Packet.Response.success("Successfully indicated the status of the server's send request.");
                    }
                });
        return response;
    }

    /**
     * Sends a player to a specific Server if it exists.
     * @param player The uuid of the player to send.
     * @param server The uuid of the server to send to.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> send(UUID player, UUID server) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Identification.from("RC","SP"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER_UUID, player.toString())
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_SERVER, server.toString())
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, new PacketListener<>() {
                    @Override
                    public Packet.Response handle(MagicLinkCore.Packets.Response packet) {
                        response.complete(packet);
                        return Packet.Response.success("Successfully indicated the status of the server's send request.");
                    }
                });
        return response;
    }

    /**
     * Provides a declarative method by which you can establish a new Server instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends Particle.Tinder<ServerKernel> {
        private final UUID uuid;
        private final ServerAdapter adapter;
        private Particle.Tinder<? extends LangLibrary> lang = LangLibrary.Tinder.DEFAULT_LANG_LIBRARY;
        private final String displayName;
        private final InetSocketAddress address;
        private final Particle.Tinder<? extends MagicLinkCore.Server> magicLink;
        private Particle.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        private Particle.Tinder<? extends ErrorRegistry> errors = ErrorRegistry.Tinder.DEFAULT_CONFIGURATION;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull ServerAdapter adapter,
                @Nullable String displayName,
                @NotNull InetSocketAddress address,
                @NotNull Particle.Tinder<? extends MagicLinkCore.Server> magicLink
                ) {
            this.uuid = uuid;
            this.adapter = adapter;
            this.displayName = displayName;
            this.address = address;
            this.magicLink = magicLink;
        }

        public Tinder lang(@NotNull Particle.Tinder<? extends LangLibrary> lang) {
            this.lang = lang;
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

        @Override
        public @NotNull ServerKernel ignite() throws Exception {
            Version version;
            try (InputStream input = ProxyKernel.class.getClassLoader().getResourceAsStream("metadata.json")) {
                if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(new String(input.readAllBytes()), JsonObject.class);
                version = new Version(object.get("version").getAsString());
            }

            return new ServerKernel(
                    uuid,
                    version,
                    adapter,
                    List.of(
                        lang.flux(),
                        magicLink.flux(),
                        eventManager.flux(),
                        errors.flux()
                    ),
                    displayName,
                    address
            );
        }
    }
}