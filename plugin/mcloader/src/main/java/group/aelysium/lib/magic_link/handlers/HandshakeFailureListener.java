package group.aelysium.lib.magic_link.handlers;

import group.aelysium.rustyconnector.common.packets.BuiltInIdentifications;
import group.aelysium.central.MCLoaderTinder;
import group.aelysium.lib.magic_link.MagicLink;
import group.aelysium.rustyconnector.toolkit.common.logger.PluginLogger;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HandshakeFailureListener extends PacketListener<group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Failure> {
    protected IMCLoaderTinder api;

    public HandshakeFailureListener(IMCLoaderTinder api) {
        this.api = api;
    }

    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.MAGICLINK_HANDSHAKE_FAIL;
    }

    @Override
    public group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Failure wrap(Packet packet) {
        return new group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Failure(packet);
    }

    @Override
    public void execute(group.aelysium.rustyconnector.common.packets.MagicLink.Handshake.Failure packet) {
        PluginLogger logger = api.logger();
        MagicLink service = ((MCLoaderTinder) api).services().magicLink();

        logger.send(Component.text(packet.reason(), NamedTextColor.RED));
        logger.send(Component.text("Waiting 1 minute before trying again...", NamedTextColor.GRAY));
        service.setDelay(60);
    }
}
