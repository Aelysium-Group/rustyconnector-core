package group.aelysium.rustyconnector.mc_loader.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.mc_loader.ServerFlame;
import group.aelysium.rustyconnector.mc_loader.events.magic_link.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MagicLink extends MagicLinkCore {
    protected final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger delay = new AtomicInteger(5);
    private final String magicConfig;
    private final String podName = System.getenv("POD_NAME");
    private final AtomicBoolean stopPinging = new AtomicBoolean(false);

    protected MagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull String magicConfig
    ) {
        super(cryptor, cache, self);
        this.magicConfig = magicConfig;
        this.heartbeat();
    }

    public String magicConfig() {
        return this.magicConfig;
    }

    public void setDelay(int delay) {
        this.delay.set(delay);
    }

    private void heartbeat() {
        this.executor.schedule(() -> {
            if(stopPinging.get()) return;

            ServerFlame flame = RustyConnector.Toolkit.Server().orElseThrow().orElseThrow();

            try {
                Packet.Builder.PrepareForSending packet = Packet.New()
                        .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.MAGIC_CONFIG_NAME, this.magicConfig())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName()+":"+flame.address().getPort())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new PacketParameter(flame.playerCount()));

                if(podName != null)
                    packet.parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.POD_NAME, this.podName);

                packet.addressedTo(Packet.Target.allAvailableProxies()).send();
            } catch (Exception e) {
                e.printStackTrace();
            }

            MagicLink.this.heartbeat();
        }, this.delay.get(), TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        stopPinging.set(true);
        try {
            this.executor.shutdownNow();
        } catch (Exception ignore) {}

        try {
            Packet.New()
                    .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT)
                    .addressedTo(Packet.Target.allAvailableProxies())
                    .send();

            RC.S.EventManager().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}
    }

    public static class Tinder extends Particle.Tinder<MagicLink> {
        private final AESCryptor cryptor;
        private final MessageCache cache;
        private final Packet.Target self;
        private final String magicConfig;
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull MessageCache cache,
                @NotNull Packet.Target self,
                @NotNull String magicConfig
        ) {
            this.cryptor = cryptor;
            this.cache = cache;
            this.self = self;
            this.magicConfig = magicConfig;
        }

        @Override
        public @NotNull MagicLink ignite() throws Exception {
            return new MagicLink(
                    this.cryptor,
                    this.cache,
                    this.self,
                    this.magicConfig
            );
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID serverUUID) {
            return new Tinder(
                    AESCryptor.DEFAULT_CRYPTOR,
                    new MessageCache(50),
                    Packet.Target.server(serverUUID),
                    "default"
            );
        }
    }
}
