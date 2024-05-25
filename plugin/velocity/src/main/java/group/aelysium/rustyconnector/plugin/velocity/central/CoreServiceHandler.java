package group.aelysium.rustyconnector.plugin.velocity.central;

import group.aelysium.rustyconnector.core.common.events.EventManager;
import group.aelysium.rustyconnector.plugin.velocity.lib.config.ConfigService;
import group.aelysium.rustyconnector.toolkit.core.packet.VelocityPacketBuilder;
import group.aelysium.rustyconnector.toolkit.velocity.central.ICoreServiceHandler;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ServiceHandler;
import group.aelysium.rustyconnector.core.common.cache.MessageCacheService;
import group.aelysium.rustyconnector.core.common.data_transit.DataTransitService;
import group.aelysium.rustyconnector.toolkit.core.serviceable.interfaces.Service;
import group.aelysium.rustyconnector.plugin.velocity.lib.dynamic_teleport.DynamicTeleportService;
import group.aelysium.rustyconnector.core.proxy.family.Families;
import group.aelysium.rustyconnector.plugin.velocity.lib.friends.FriendsService;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing.LoadBalancerService;
import group.aelysium.rustyconnector.plugin.velocity.lib.magic_link.MagicLink;
import group.aelysium.rustyconnector.plugin.velocity.lib.parties.PartyService;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.PlayerService;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.mcloader.ServerService;
import group.aelysium.rustyconnector.plugin.velocity.lib.remote_storage.Storage;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.whitelist.WhitelistService;

import java.util.Map;
import java.util.Optional;

public class CoreServiceHandler extends ServiceHandler implements ICoreServiceHandler {
    public CoreServiceHandler(Map<Class<? extends Service>, Service> services) {
        super(services);
    }

    public EventManager events() {
        return this.find(EventManager.class).orElseThrow();
    }
    public Families family() {
        return this.find(Families.class).orElseThrow();
    }
    public ServerService server() {
        return this.find(ServerService.class).orElseThrow();
    }
    public MagicLink magicLink() {
        return this.find(MagicLink.class).orElseThrow();
    }
    public Storage storage() {
        return this.find(Storage.class).orElseThrow();
    }
    public PlayerService player() {
        return this.find(PlayerService.class).orElseThrow();
    }
    public ConfigService config() {
        return this.find(ConfigService.class).orElseThrow();
    }
    public DataTransitService dataTransitService() {
        return this.find(DataTransitService.class).orElseThrow();
    }
    public MessageCacheService messageCache() {
        return this.find(MessageCacheService.class).orElseThrow();
    }
    public WhitelistService whitelist() {
        return this.find(WhitelistService.class).orElseThrow();
    }
    public LoadBalancerService loadBalancingService() {
        return this.find(LoadBalancerService.class).orElseThrow();
    }
    public VelocityPacketBuilder packetBuilder() {
        return this.find(VelocityPacketBuilder.class).orElseThrow();
    }
    public Optional<PartyService> party() {
        return this.find(PartyService.class);
    }
    public Optional<FriendsService> friends() {
        return this.find(FriendsService.class);
    }
    public Optional<DynamicTeleportService> dynamicTeleport() {
        return this.find(DynamicTeleportService.class);
    }
}