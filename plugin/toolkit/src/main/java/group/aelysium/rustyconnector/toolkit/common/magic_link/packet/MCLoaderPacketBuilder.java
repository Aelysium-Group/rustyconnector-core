package group.aelysium.rustyconnector.toolkit.common.magic_link.packet;

import group.aelysium.rustyconnector.toolkit.common.serviceable.interfaces.Service;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.ICoreServiceHandler;
import group.aelysium.rustyconnector.toolkit.mc_loader.MCLoaderFlame;

public class MCLoaderPacketBuilder implements Service {
    private final MCLoaderFlame<? extends ICoreServiceHandler> flame;

    public MCLoaderPacketBuilder(MCLoaderFlame<? extends ICoreServiceHandler> flame) {
        this.flame = flame;
    }

    public Packet.MCLoaderPacketBuilder newBuilder() {
        return new Packet.MCLoaderPacketBuilder(flame);
    }

    @Override
    public void kill() {}
}
