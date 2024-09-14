package group.aelysium.rustyconnector.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.server.lang.ServerLang;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.util.Version;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeFailureListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeStalePingListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.*;

public class ServerKernel implements Particle {
    private final UUID uuid;
    private final Version version;
    private final ServerAdapter adapter;
    private final Flux<? extends LangLibrary<? extends ServerLang>> lang;
    private final String displayName;
    private final InetSocketAddress address;
    private final Flux<? extends MagicLinkCore.Server> magicLink;
    private final Flux<? extends EventManager> eventManager;

    protected ServerKernel(
            @NotNull UUID uuid,
            @NotNull ServerAdapter adapter,
            @NotNull Flux<? extends LangLibrary<? extends ServerLang>> lang,
            @Nullable String displayName,
            @NotNull InetSocketAddress address,
            @NotNull Flux<? extends MagicLinkCore.Server> magicLink,
            @NotNull Flux<? extends EventManager> eventManager
    ) {
        this.uuid = uuid;
        this.adapter = adapter;
        this.lang = lang;
        this.displayName = displayName;
        this.address = address;
        this.magicLink = magicLink;
        this.eventManager = eventManager;

        try {
            try (InputStream input = ProxyKernel.class.getClassLoader().getResourceAsStream("metadata.json")) {
                if (input == null) throw new NullPointerException("Unable to initialize version number from jar.");
                Gson gson = new Gson();
                JsonObject object = gson.fromJson(new String(input.readAllBytes()), JsonObject.class);
                this.version = new Version(object.get("version").getAsString());
            }

            this.lang.access().get();
            this.magicLink.access().get();
            this.eventManager.access().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the session uuid of this Server.
     * The Server's uuid won't change while it's alive, but once it's restarted or reloaded, the session uuid will change.
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
        return this.adapter.onlinePlayerCount();
    }

    /**
     * Locks this Server so that playerRegistry can't join it via the family's load balancer.
     */
    public void lock() {
        Packet.New()
                .identification(Packet.Identification.from("RC","LS"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send();
    }

    /**
     * Unlocks this Server so that playerRegistry can join it via the family's load balancer.
     */
    public void unlock() {
        Packet.New()
                .identification(Packet.Identification.from("RC","US"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send();
    }

    /**
     * Sends a player to a specific family if it exists.
     * @param player The uuid of the player to send.
     * @param familyID The id of the family to send to.
     * @return The packet which was sent.
     */
    public Packet.Local send(UUID player, String familyID) {
        return Packet.New()
                .identification(Packet.Identification.from("RC","SP"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER_UUID, player.toString())
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_FAMILY, familyID)
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send();
    }

    /**
     * Sends a player to a specific Server if it exists.
     * @param player The uuid of the player to send.
     * @param server The uuid of the server to send to.
     * @return The packet which was sent.
     */
    public Packet.Local send(UUID player, UUID server) {
        return Packet.New()
                .identification(Packet.Identification.from("RC","SP"))
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.PLAYER_UUID, player.toString())
                .parameter(MagicLinkCore.Packets.SendPlayer.Parameters.TARGET_SERVER, server.toString())
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send();
    }

    public Flux<? extends MagicLinkCore.Server> MagicLink() {
        return this.magicLink;
    }

    public ServerAdapter Adapter() {
        return this.adapter;
    }

    public Flux<? extends LangLibrary<? extends ServerLang>> Lang() {
        return this.lang;
    }

    public Flux<? extends EventManager> EventManager() {
        return this.eventManager;
    }

    @Override
    public void close() {
        this.magicLink.close();
    }

    /**
     * Provides a declarative method by which you can establish a new Server instance on RC.
     * Parameters listed in the constructor are required, any other parameters are
     * technically optional because they also have default implementations.
     */
    public static class Tinder extends Particle.Tinder<ServerKernel> {
        private final UUID uuid;
        private final ServerAdapter adapter;
        private Particle.Tinder<? extends LangLibrary<? extends ServerLang>> lang = LangLibrary.Tinder.DEFAULT_SERVER_CONFIGURATION;
        private final String displayName;
        private final InetSocketAddress address;
        private final Particle.Tinder<? extends MagicLinkCore.Server> magicLink;
        private Particle.Tinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;

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

        public Tinder lang(@NotNull Particle.Tinder<? extends LangLibrary<? extends ServerLang>> lang) {
            this.lang = lang;
            return this;
        }

        public Tinder eventManager(@NotNull Particle.Tinder<? extends EventManager> eventManager) {
            this.eventManager = eventManager;
            return this;
        }

        @Override
        public @NotNull ServerKernel ignite() throws Exception {
            return new ServerKernel(
                    uuid,
                    adapter,
                    lang.flux(),
                    displayName,
                    address,
                    magicLink.flux(),
                    eventManager.flux()
            );
        }
    }
}