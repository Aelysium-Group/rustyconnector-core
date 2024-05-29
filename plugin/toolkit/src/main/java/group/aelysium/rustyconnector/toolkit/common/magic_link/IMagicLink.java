package group.aelysium.rustyconnector.toolkit.common.magic_link;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.packet.Packet;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.PacketListener;

import java.util.Optional;

public interface IMagicLink extends Particle {
    /**
     * Gets the connection to the remote resource.
     * @return {@link IMessengerConnection}
     */
    Optional<IMessengerConnection> connection();

    interface Connection extends AutoCloseable {
        Packet.Builder startNewPacket();

        /**
         * Register a listener to handle particular packets.
         * @param listener The listener to use.
         */
        <TPacketListener extends PacketListener<? extends Packet.Wrapper>> void on(TPacketListener listener);
    }
}
