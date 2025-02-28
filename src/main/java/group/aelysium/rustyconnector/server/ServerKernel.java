package group.aelysium.rustyconnector.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.modules.ModuleBuilder;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.proxy.util.Version;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
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
    private final Map<String, Packet.Parameter> metadata = new ConcurrentHashMap<>(Map.of(
            "softCap", new Packet.Parameter(30),
            "hardCap", new Packet.Parameter(40)
    ));

    protected ServerKernel(
            @NotNull String id,
            @NotNull Version version,
            @NotNull ServerAdapter adapter,
            @NotNull Path directory,
            @NotNull Path modulesDirectory,
            @NotNull InetSocketAddress address,
            @NotNull String targetFamily,
            @NotNull Map<String, Packet.Parameter> metadata
    ) throws Exception {
        super(id, version, adapter, directory, modulesDirectory);
        this.address = address;
        this.targetFamily = targetFamily;
        this.metadata.putAll(metadata);
    }

    /**
     * Stores metadata in the Server.
     * @param propertyName The name of the metadata to store.
     * @param property The metadata to store.
     * @return `true` if the metadata could be stored. `false` if the name of the metadata is already in use.
     */
    public boolean metadata(String propertyName, Packet.Parameter property) {
        if(this.metadata.containsKey(propertyName)) return false;
        this.metadata.put(propertyName, property);
        return true;
    }

    /**
     * Fetches metadata from the server.
     * @param propertyName The name of the metadata to fetch.
     * @return An optional containing the metadata, or an empty metadata if no metadata could be found.
     * @param <T> The type of the metadata that's being fetched.
     */
    public <T> Optional<T> metadata(String propertyName) {
        return Optional.ofNullable((T) this.metadata.get(propertyName));
    }

    /**
     * Removes metadata from the server.
     * @param propertyName The name of the metadata to remove.
     */
    public void dropMetadata(String propertyName) {
        this.metadata.remove(propertyName);
    }

    /**
     * @return A map containing all of this server's metadata.
     */
    public Map<String, Object> metadata() {
        Map<String, Object> reduced = new HashMap<>();
        this.metadata.forEach((k, v)->reduced.put(k, v.getOriginalValue()));
        return Collections.unmodifiableMap(reduced);
    }

    /**
     * @return A map containing all of this server's metadata.
     */
    public Map<String, Packet.Parameter> parameterizedMetadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

    /**
     * The display name of this Server.
     */
    public @Nullable String displayName() {
        try {
            return this.metadata.get("displayName").getAsString();
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
     * If both a family AND server have an id equal to `target`, you'll have to clarify which to send to use.
     * @param player The uuid or username of the player to send. RustyConnector will automatically determine if this is a UUID or username.
     * @param target The id of the family or server to send the player to.
     * @return A future that completes to the response received from the proxy.
     */
    public CompletableFuture<MagicLinkCore.Packets.Response> send(String player, String target, String flags) {
        CompletableFuture<MagicLinkCore.Packets.Response> response = new CompletableFuture<>();
        Packet.New()
                .identification(Packet.Type.from("RC","PS"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER, player)
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.GENERIC_TARGET, target)
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.FLAGS, flags)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send()
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