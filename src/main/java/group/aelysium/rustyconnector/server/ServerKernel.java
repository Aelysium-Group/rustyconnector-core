package group.aelysium.rustyconnector.server;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

public class ServerKernel extends RCKernel<ServerAdapter> {
    private final String targetFamily;
    private final InetSocketAddress address;

    public ServerKernel(
            @NotNull String id,
            @NotNull ServerAdapter adapter,
            @NotNull Path directory,
            @NotNull Path modulesDirectory,
            @NotNull InetSocketAddress address,
            @NotNull String targetFamily
    ) throws Exception {
        super(id, adapter, directory, modulesDirectory);
        this.address = address;
        this.targetFamily = targetFamily;
        this.storeMetadata("serverGenerator", Parameter.fromUnknown("default"));
    }
    
    /**
     * The display name of this Server.
     */
    public @Nullable String displayName() {
        try {
            return this.fetchMetadata("displayName").orElseThrow().toString();
        } catch(Exception ignore) {}
        return null;
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
     * @return The name of the family that this server is wanting to be connected to.
     */
    public @NotNull String targetFamily() {
        return this.targetFamily;
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
     * If both a family AND server have an id equal to `target`, you'll have to clarify which to use via "flags".
     * @param playerID The id of the player to send.
     * @param target The id of the family or server to send the player to.
     * @param flags A set of flags to use.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> sendID(String playerID, String target, Set<MagicLinkCore.Packets.SendPlayer.Flag> flags) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        MagicLinkCore.Packets.SendPlayer
            .sendID(playerID, target, flags)
            .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                response.complete(packet);
                return PacketListener.Response.success("Successfully indicated the status of the server's send request.");
            });
        return response;
    }
    
    /**
     * Sends a player to a family or server if it exists.
     * If both a family AND server have an id equal to `target`, you'll have to clarify which to use via "flags".
     * @param playerUsername The username of the player to send.
     * @param target The id of the family or server to send the player to.
     * @param flags A set of flags to use.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> sendUsername(String playerUsername, String target, Set<MagicLinkCore.Packets.SendPlayer.Flag> flags) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        MagicLinkCore.Packets.SendPlayer
            .sendUsername(playerUsername, target, flags)
            .onReply(MagicLinkCore.Packets.Response.class, packet -> {
                response.complete(packet);
                return PacketListener.Response.success("Successfully indicated the status of the server's send request.");
            });
        return response;
    }

    @Override
    public @Nullable Component details() {
        return join(
            newlines(),
            RC.Lang("rustyconnector-keyValue").generate("ID", this.id()),
            RC.Lang("rustyconnector-keyValue").generate("Modules Installed", this.modules.size()),
            RC.Lang("rustyconnector-keyValue").generate("Address", AddressUtil.addressToString(this.address())),
            RC.Lang("rustyconnector-keyValue").generate("Family", this.targetFamily()),
            RC.Lang("rustyconnector-keyValue").generate("Online Players", this.playerCount()),
            empty(),
            text("Extra Properties:", DARK_GRAY),
            (
                this.metadata().isEmpty() ?
                    text("There is no metadata to show.", DARK_GRAY)
                    :
                    join(
                        newlines(),
                        this.metadata().entrySet().stream().map(e -> RC.Lang("rustyconnector-keyValue").generate(e.getKey(), e.getValue())).toList()
                    )
            )
        );
    }
}