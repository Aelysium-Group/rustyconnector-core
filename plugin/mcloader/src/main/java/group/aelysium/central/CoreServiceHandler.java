package group.aelysium.central;

import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.lib.ranked_game_interface.RankedGameInterfaceService;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.MCLoaderPacketBuilder;
import group.aelysium.rustyconnector.toolkit.common.serviceable.ServiceHandler;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.ICoreServiceHandler;
import group.aelysium.rustyconnector.common.cache.MessageCacheService;
import group.aelysium.rustyconnector.toolkit.common.serviceable.interfaces.Service;
import group.aelysium.lib.dynamic_teleport.DynamicTeleportService;
import group.aelysium.lib.magic_link.MagicLink;
import group.aelysium.lib.server_info.ServerInfoService;

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