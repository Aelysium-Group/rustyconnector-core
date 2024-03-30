package group.aelysium.rustyconnector.plugin.velocity.lib.dynamic_scale;

import group.aelysium.rustyconnector.core.lib.events.EventManager;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.FamilyService;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.ProxyLang;
import group.aelysium.rustyconnector.toolkit.core.log_gate.GateKey;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ClockService;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.RebalanceEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.util.DependencyInjector;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;

public class DynamicScalingService extends ClockService {
    protected final LiquidTimestamp heartbeat;
    public DynamicScalingService(int threads, LiquidTimestamp heartbeat) {
        super(threads);
        this.heartbeat = heartbeat;
    }

    public void init(DependencyInjector.DI3<FamilyService, PluginLogger, EventManager> deps) {
    }
}
