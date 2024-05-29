package group.aelysium.lib.magic_link.handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.central.MCLoaderTinder;
import group.aelysium.lib.lang.MCLoaderLang;
import group.aelysium.lib.ranked_game_interface.RankedGameInterfaceService;
import group.aelysium.rustyconnector.toolkit.common.logger.PluginLogger;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.lib.magic_link.MagicLink;
import group.aelysium.rustyconnector.toolkit.common.server.ServerAssignment;
import group.aelysium.rustyconnector.toolkit.mc_loader.events.magic_link.ConnectedEvent;
import net.kyori.adventure.text.Component;

public class HandshakeSuccessListener extends PacketListener<group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Success> {
    protected MCLoaderTinder api;

    public HandshakeSuccessListener(MCLoaderTinder api) {
        this.api = api;
    }

    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.MAGICLINK_HANDSHAKE_SUCCESS;
    }

    @Override
    public group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Success wrap(Packet packet) {
        return new group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Success(packet);
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Success packet) {
        PluginLogger logger = api.logger();
        MagicLink service = api.services().magicLink();
        api.services().events().fireEvent(new ConnectedEvent(packet.assignment()));

        logger.send(Component.text(packet.message(), packet.color()));
        logger.send(MCLoaderLang.MAGIC_LINK.build());

        service.setDelay(packet.pingInterval());
        api.services().serverInfo().assignment(packet.assignment());

        if(packet.assignment() == ServerAssignment.RANKED_GAME_SERVER) api.services().add(new RankedGameInterfaceService());
    }
}
