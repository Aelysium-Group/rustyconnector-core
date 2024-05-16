package group.aelysium.rustyconnector.core.proxy.family.dynamic_scale;

import group.aelysium.rustyconnector.core.common.events.EventManager;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import group.aelysium.rustyconnector.core.proxy.family.Families;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ClockService;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.RebalanceEvent;
import group.aelysium.rustyconnector.toolkit.velocity.util.DependencyInjector;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;

public class DynamicScalingService extends ClockService {
    protected final LiquidTimestamp heartbeat;
    public DynamicScalingService(int threads, LiquidTimestamp heartbeat) {
        super(threads);
        this.heartbeat = heartbeat;
    }

    public void init(DependencyInjector.DI3<Families, PluginLogger, EventManager> deps) {
    }
}
