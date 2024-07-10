package group.aelysium.rustyconnector.common.magic_link.buitin_packets;

import group.aelysium.rustyconnector.common.magic_link.packet.Packet;

public interface MCLoader {
    class Lock extends group.aelysium.rustyconnector.common.magic_link.Packet.Wrapper {
        public Lock(Packet packet) {
            super(packet);
        }
    }
    class Unlock extends group.aelysium.rustyconnector.common.magic_link.Packet.Wrapper {
        public Unlock(Packet packet) {
            super(packet);
        }
    }
}
