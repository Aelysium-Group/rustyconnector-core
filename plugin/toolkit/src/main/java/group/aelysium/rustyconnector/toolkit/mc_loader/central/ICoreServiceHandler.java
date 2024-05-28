package group.aelysium.rustyconnector.toolkit.mc_loader.central;

import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.common.packet.MCLoaderPacketBuilder;
import group.aelysium.rustyconnector.toolkit.common.serviceable.interfaces.IServiceHandler;
import group.aelysium.rustyconnector.toolkit.mc_loader.dynamic_teleport.ICoordinateRequest;
import group.aelysium.rustyconnector.toolkit.mc_loader.dynamic_teleport.IDynamicTeleportService;
import group.aelysium.rustyconnector.toolkit.mc_loader.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.mc_loader.ranked_game_interface.IRankedGameInterfaceService;
import group.aelysium.rustyconnector.toolkit.mc_loader.server_info.IServerInfoService;

import java.util.Optional;

public interface ICoreServiceHandler extends IServiceHandler {
    /**
     * Gets the {@link IEventManager event manager} which allows access to event based logic.
     * @return {@link IEventManager}
     */
    IEventManager events();
    IMagicLink magicLink();
    IServerInfoService serverInfo();
    Optional<? extends IRankedGameInterfaceService> rankedGameInterface();
    IDynamicTeleportService<? extends ICoordinateRequest> dynamicTeleport();
    MCLoaderPacketBuilder packetBuilder();
}