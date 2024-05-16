package group.aelysium.rustyconnector.core.proxy.family.matchmaking.packet_handlers;

import group.aelysium.rustyconnector.core.common.packets.BuiltInIdentifications;
import group.aelysium.rustyconnector.core.common.packets.RankedGame;
import group.aelysium.rustyconnector.core.proxy.family.mcloader.RankedMCLoader;
import group.aelysium.rustyconnector.core.proxy.family.ranked_family.RankedFamily;
import group.aelysium.rustyconnector.toolkit.core.packet.Packet;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketListener;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.ISession;
import group.aelysium.rustyconnector.toolkit.velocity.server.IRankedMCLoader;

public class RankedGameEndListener extends PacketListener<RankedGame.End> {
    protected Tinder api;

    public RankedGameEndListener(Tinder api) {
        this.api = api;
    }

    @Override
    public PacketIdentification target() {
        return BuiltInIdentifications.RANKED_GAME_END;
    }

    @Override
    public RankedGame.End wrap(Packet packet) {
        return new RankedGame.End(packet);
    }

    @Override
    public void execute(RankedGame.End packet) {
        RankedMCLoader mcloader = new IRankedMCLoader.Reference(packet.sender().uuid()).get();

        ISession session = mcloader.currentSession().orElseGet(() -> {
            RankedFamily family = (RankedFamily) mcloader.family();
            return family.matchmaker().fetch(packet.session().uuid()).orElseThrow(()->
                    new RuntimeException("No session with the uuid: "+packet.session().uuid()+" exists on MCLoader: "+mcloader.uuid())
            );
        });

        session.end(packet.session().winners(), packet.session().losers(), packet.unlock());
    }
}