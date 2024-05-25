package group.aelysium.rustyconnector.core.proxy.family.matchmaking.packet_handlers;

import group.aelysium.rustyconnector.core.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.core.common.packets.RankedGame;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.core.proxy.family.ranked_family.RankedFamily;
import group.aelysium.rustyconnector.core.proxy.family.mcloader.RankedMCLoader;
import group.aelysium.rustyconnector.toolkit.core.packet.Packet;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.ISession;
import group.aelysium.rustyconnector.toolkit.velocity.server.IRankedMCLoader;

public class RankedGameImplodedListener extends PacketListener<RankedGame.Imploded> {
    protected Tinder api;

    public RankedGameImplodedListener(Tinder api) {
        this.api = api;
    }

    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.RANKED_GAME_IMPLODE;
    }

    @Override
    public RankedGame.Imploded wrap(Packet packet) {
        return new RankedGame.Imploded(packet);
    }

    @Override
    public void execute(RankedGame.Imploded packet) {
        RankedMCLoader mcloader = new IRankedMCLoader.Reference(packet.sender().uuid()).get();

        ISession session = mcloader.currentSession().orElseGet(() -> {
            RankedFamily family = (RankedFamily) mcloader.family();
            return family.matchmaker().fetch(packet.sessionUUID()).orElseThrow(()->
                    new RuntimeException("No session with the uuid: "+packet.sessionUUID()+" exists on MCLoader: "+mcloader.uuid())
            );
        });

        session.implode(packet.reason(), packet.unlock());
    }
}