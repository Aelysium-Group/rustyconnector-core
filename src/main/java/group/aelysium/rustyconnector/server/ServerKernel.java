package group.aelysium.rustyconnector.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.plugins.PluginTinder;
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
            @NotNull String id,
            @NotNull Version version,
            @NotNull ServerAdapter adapter,
            @NotNull List<? extends Flux<? extends Particle>> plugins,
            @Nullable String displayName,
            @NotNull InetSocketAddress address
    ) {
        super(id, version, adapter, plugins);
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
     * The number of players on this server.
     * @return {@link Integer}
     */
    public int playerCount() {
        return this.Adapter().onlinePlayerCount();
    }

    /**
     * Locks this Server so that players can't join it via the family's load balancer.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> lock() {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","SL"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                    response.complete(packet);
                    return PacketListener.Response.success("Successfully indicated the status of the server's lock request");
                });
        return response;
    }

    /**
     * Unlocks this Server so that players can join it via the family's load balancer.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> unlock() {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","SU"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                    response.complete(packet);
                    return PacketListener.Response.success("Successfully indicated the status of the server's unlock request");
                });
        return response;
    }

    /**
     * Sends a player to a family or server if it exists.
     * If both a family AND server have an id equal to `target`, you'll have to clarify which to send to using
     * @param player The uuid or username of the player to send. RustyConnector will automatically determine if this is a UUID or username.
     * @param target The id of the family or server to send the player to.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> send(String player, String target) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","PS"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER, player)
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.GENERIC_TARGET, target)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                    response.complete(packet);
                    return PacketListener.Response.success("Successfully indicated the status of the server's send request.");
                });
        return response;
    }

    /**
     * Sends a player to a server if it exists.
     * @param player The uuid or username of the player to send. RustyConnector will automatically determine if this is a UUID or username.
     * @param target The id of the server to send the player to.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> sendServer(String player, String target) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","PS"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER, player)
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_SERVER, target)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                    response.complete(packet);
                    return PacketListener.Response.success("Successfully indicated the status of the server's send request.");
                });
        return response;
    }

    /**
     * Sends a player to a family if it exists.
     * @param player The uuid or username of the player to send. RustyConnector will automatically determine if this is a UUID or username.
     * @param target The id of the family to send the player to.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> sendFamily(String player, String target) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","PS"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER, player)
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_FAMILY, target)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
                .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                    response.complete(packet);
                    return PacketListener.Response.success("Successfully indicated the status of the server's send request.");
                });
        return response;
    }

    /**
     * Provides a declarative method by which you can establish a new Server instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends RCKernel.Tinder<ServerAdapter, ServerKernel> {
        private final String displayName;
        private final InetSocketAddress address;
        private PluginTinder<? extends MagicLinkCore.Server> magicLink;

        public Tinder(
                @NotNull String id,
                @NotNull ServerAdapter adapter,
                @Nullable String displayName,
                @NotNull InetSocketAddress address,
                @NotNull PluginTinder<? extends MagicLinkCore.Server> magicLink
                ) {
            super(id, adapter);
            this.displayName = displayName;
            this.address = address;
            this.magicLink = magicLink;
        }

        public Tinder lang(@NotNull PluginTinder<? extends LangLibrary> lang) {
            this.lang = lang;
            return this;
        }

        public Tinder magicLink(@NotNull PluginTinder<? extends MagicLinkCore.Server> magicLink) {
            this.magicLink = magicLink;
            return this;
        }

        public Tinder eventManager(@NotNull PluginTinder<? extends EventManager> eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        public Tinder errorHandler(@NotNull PluginTinder<? extends ErrorRegistry> errorHandler) {
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
                    id,
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