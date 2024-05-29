package group.aelysium.lib.magic_link.handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.central.MCLoaderTinder;
import group.aelysium.lib.magic_link.MagicLink;
import group.aelysium.lib.server_info.ServerInfoService;
import group.aelysium.rustyconnector.toolkit.common.logger.PluginLogger;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.mc_loader.events.magic_link.TimeoutEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HandshakeStalePingListener extends PacketListener<group.aelysium.rustyconnector.common.packets.MagicLink.StalePing> {
    protected IMCLoaderTinder api;

    public HandshakeStalePingListener(IMCLoaderTinder api) {
        this.api = api;
    }

    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.MAGICLINK_HANDSHAKE_STALE_PING;
    }

    @Override
    public group.aelysium.rustyconnector.common.packets.MagicLink.StalePing wrap(Packet packet) {
        return new group.aelysium.rustyconnector.common.packets.MagicLink.StalePing(packet);
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.packets.MagicLink.StalePing packet) {
        PluginLogger logger = api.logger();
        MagicLink service = ((MCLoaderTinder) api).services().magicLink();
        ServerInfoService serverInfoService = ((MCLoaderTinder) api).services().serverInfo();
        ((MCLoaderTinder) api).services().events().fireEvent(new TimeoutEvent());

        logger.send(Component.text("Connection to the Proxy has timed out! Attempting to reconnect...", NamedTextColor.RED));
        service.setDelay(5);
        Packet response = packet.reply()
                .identification(BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.ADDRESS, serverInfoService.address())
                .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.DISPLAY_NAME, serverInfoService.displayName())
                .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.MAGIC_CONFIG_NAME, serverInfoService.magicConfig())
                .parameter(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Ping.Parameters.PLAYER_COUNT, new PacketParameter(serverInfoService.playerCount()))
                .build();
        service.connection().orElseThrow().publish(response);
    }
}
