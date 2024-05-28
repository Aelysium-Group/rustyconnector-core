package group.aelysium.lib.magic_link;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.central.MCLoaderFlame;
import group.aelysium.rustyconnector.toolkit.common.messenger.IMessengerConnection;
import group.aelysium.rustyconnector.toolkit.common.messenger.IMessengerConnector;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.packet.PacketParameter;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.ICoreServiceHandler;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderFlame;
import group.aelysium.rustyconnector.toolkit.mc_loader.events.magic_link.DisconnectedEvent;
import group.aelysium.rustyconnector.toolkit.mc_loader.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.common.serviceable.ClockService;
import group.aelysium.rustyconnector.toolkit.mc_loader.server_info.IServerInfoService;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import group.aelysium.TinderAdapterForCore;

import java.net.ConnectException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MagicLink implements IMagicLink {
    private final IMessengerConnector messenger;
    private final ClockService heartbeat = new ClockService(2);
    private final AtomicInteger delay = new AtomicInteger(5);
    private final String podName = System.getenv("POD_NAME");
    private final AtomicBoolean stopPinging = new AtomicBoolean(false);

    public MagicLink(IMessengerConnector messenger) {
        this.messenger = messenger;
    }

    public void setDelay(int delay) {
        this.delay.set(delay);
    }

    private void scheduleNextPing(IMCLoaderFlame<? extends ICoreServiceHandler> api) {
        IServerInfoService serverInfoService = api.services().serverInfo();
        this.heartbeat.scheduleDelayed(() -> {
            if(stopPinging.get()) return;

            try {
                Packet.MCLoaderPacketBuilder.ReadyForParameters packet = api.services().packetBuilder().newBuilder()
                        .identification(BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                        .sendingToProxy()
                        .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.ADDRESS, serverInfoService.address())
                        .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.DISPLAY_NAME, serverInfoService.displayName())
                        .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.MAGIC_CONFIG_NAME, serverInfoService.magicConfig())
                        .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.PLAYER_COUNT, new PacketParameter(serverInfoService.playerCount()));

                if(podName != null)
                    packet.parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.POD_NAME, this.podName);

                api.services().magicLink().connection().orElseThrow().publish(packet.build());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MagicLink.this.scheduleNextPing(api);
        }, LiquidTimestamp.from(this.delay.get(), TimeUnit.SECONDS));
    }

    public void startHeartbeat(IMCLoaderFlame<? extends ICoreServiceHandler> api) {
        this.scheduleNextPing(api);
    }

    public Optional<IMessengerConnection> connection() {
        return this.messenger.connection();
    }

    public IMessengerConnection connect() throws ConnectException {
        return this.messenger.connect();
    }

    @Override
    public void kill() {
        stopPinging.set(true);
        try {
            this.heartbeat.kill();
        } catch (Exception ignore) {}

        try {
            MCLoaderFlame api = TinderAdapterForCore.getTinder().flame();

            Packet packet = api.services().packetBuilder().newBuilder()
                    .identification(BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT)
                    .sendingToProxy()
                    .build();
            this.connection().orElseThrow().publish(packet);

            api.services().events().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}

        this.messenger.kill();
    }
}
