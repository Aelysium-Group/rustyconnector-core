package group.aelysium.rustyconnector.mc_loader;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.buitin_packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.common.magic_link.buitin_packets.SendPlayerPacket;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.mc_loader.magic_link.MagicLink;
import group.aelysium.rustyconnector.proxy.util.Version;
import group.aelysium.rustyconnector.mc_loader.lang.MCLoaderLangLibrary;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.*;

public class MCLoaderFlame implements Particle {
    private final UUID uuid;
    private final Version version;
    private final MCLoaderAdapter adapter;
    private final Flux<MCLoaderLangLibrary> lang;
    private final String displayName;
    private final InetSocketAddress address;
    private final Flux<MagicLink> magicLink;
    private final EventManager eventManager;

    protected MCLoaderFlame(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull MCLoaderAdapter adapter,
            @NotNull Flux<MCLoaderLangLibrary> lang,
            @NotNull String displayName,
            @NotNull InetSocketAddress address,
            @NotNull Flux<MagicLink> magicLink,
            @NotNull EventManager eventManager
    ) {
        this.uuid = uuid;
        this.version = version;
        this.adapter = adapter;
        this.lang = lang;
        this.displayName = displayName;
        this.address = address;
        this.magicLink = magicLink;
        this.eventManager = eventManager;
    }

    /**
     * Gets the session uuid of this MCLoader.
     * The MCLoader's uuid won't change while it's alive, but once it's restarted or reloaded, the session uuid will change.
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
     * The display name of this MCLoader.
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
        return this.adapter.onlinePlayerCount();
    }

    /**
     * Locks this MCLoader so that players can't join it via the family's load balancer.
     */
    public void lock() {
        Packet.New()
                .identification(BuiltInIdentifications.LOCK_SERVER)
                .addressedTo(Packet.Target.allAvailableProxies())
                .send();
    }

    /**
     * Unlocks this MCLoader so that players can join it via the family's load balancer.
     */
    public void unlock() {
        Packet.New()
                .identification(BuiltInIdentifications.UNLOCK_SERVER)
                .addressedTo(Packet.Target.allAvailableProxies())
                .send();
    }

    /**
     * Sends a player to a specific family if it exists.
     * @param player The uuid of the player to send.
     * @param familyID The id of the family to send to.
     */
    public void send(UUID player, String familyID) {
        Packet.New()
                .identification(BuiltInIdentifications.SEND_PLAYER)
                .parameter(SendPlayerPacket.Parameters.PLAYER_UUID, player.toString())
                .parameter(SendPlayerPacket.Parameters.TARGET_FAMILY_NAME, familyID)
                .addressedTo(Packet.Target.allAvailableProxies())
                .send();
    }

    /**
     * Sends a player to a specific MCLoader if it exists.
     * @param player The uuid of the player to send.
     * @param mcloader The uuid of the mcloader to send to.
     */
    public void send(UUID player, UUID mcloader) {

    }

    public Flux<MagicLink> MagicLink() {
        return this.magicLink;
    }

    public MCLoaderAdapter Adapter() {
        return this.adapter;
    }

    public Flux<MCLoaderLangLibrary> Lang() {
        return this.lang;
    }

    public EventManager EventManager() {
        return this.eventManager;
    }

    @Override
    public void close() throws Exception {
        this.magicLink.close();
    }

    public static class Tinder extends Particle.Tinder<MCLoaderFlame> {
        private final UUID uuid;
        private final Version version;
        private final MCLoaderAdapter adapter;
        private final MCLoaderLangLibrary.Tinder lang;
        private final String displayName;
        private final InetSocketAddress address;
        private final MagicLink.Tinder magicLink;
        private final EventManager eventManager;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull Version version,
                @NotNull MCLoaderAdapter adapter,
                @NotNull MCLoaderLangLibrary.Tinder lang,
                @NotNull String displayName,
                @NotNull InetSocketAddress address,
                @NotNull MagicLink.Tinder magicLink,
                @NotNull EventManager eventManager
                ) {
            this.uuid = uuid;
            this.version = version;
            this.adapter = adapter;
            this.lang = lang;
            this.displayName = displayName;
            this.address = address;
            this.magicLink = magicLink;
            this.eventManager = eventManager;
        }

        @Override
        public @NotNull MCLoaderFlame ignite() throws Exception {
            return new MCLoaderFlame(
                    uuid,
                    version,
                    adapter,
                    lang.flux(),
                    displayName,
                    address,
                    magicLink.flux(),
                    eventManager
            );
        }
    }
}