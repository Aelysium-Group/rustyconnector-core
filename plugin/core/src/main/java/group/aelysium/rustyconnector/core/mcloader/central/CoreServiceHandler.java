package group.aelysium.rustyconnector.core.mcloader.central;

import group.aelysium.rustyconnector.core.common.events.EventManager;
import group.aelysium.rustyconnector.core.mcloader.lib.ranked_game_interface.RankedGameInterfaceService;
import group.aelysium.rustyconnector.toolkit.core.packet.MCLoaderPacketBuilder;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ServiceHandler;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.ICoreServiceHandler;
import group.aelysium.rustyconnector.core.common.cache.MessageCacheService;
import group.aelysium.rustyconnector.toolkit.core.serviceable.interfaces.Service;
import group.aelysium.rustyconnector.core.mcloader.lib.dynamic_teleport.DynamicTeleportService;
import group.aelysium.rustyconnector.core.mcloader.lib.magic_link.MagicLink;
import group.aelysium.rustyconnector.core.mcloader.lib.server_info.ServerInfoService;

import java.util.Map;
import java.util.Optional;

public class CoreServiceHandler extends ServiceHandler implements ICoreServiceHandler {
    public CoreServiceHandler(Map<Class<? extends Service>, Service> services) {
        super(services);
    }
    public CoreServiceHandler() {
        super();
    }

    public MagicLink magicLink() {
        return this.find(MagicLink.class).orElseThrow();
    }
    public EventManager events() {
        return this.find(EventManager.class).orElseThrow();
    }
    public MessageCacheService messageCache() {
        return this.find(MessageCacheService.class).orElseThrow();
    }
    public ServerInfoService serverInfo() {
        return this.find(ServerInfoService.class).orElseThrow();
    }
    public DynamicTeleportService dynamicTeleport() {
        return this.find(DynamicTeleportService.class).orElseThrow();
    }
    public MCLoaderPacketBuilder packetBuilder() {
        return this.find(MCLoaderPacketBuilder.class).orElseThrow();
    }
    public Optional<RankedGameInterfaceService> rankedGameInterface() {
        return this.find(RankedGameInterfaceService.class);
    }
}