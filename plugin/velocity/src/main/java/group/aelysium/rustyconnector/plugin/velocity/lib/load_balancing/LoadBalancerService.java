package group.aelysium.rustyconnector.plugin.velocity.lib.load_balancing;

import group.aelysium.rustyconnector.core.lib.events.EventManager;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.FamilyService;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.log_gate.GateKey;
import group.aelysium.rustyconnector.toolkit.core.serviceable.ClockService;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.ProxyLang;
import group.aelysium.rustyconnector.toolkit.velocity.events.family.RebalanceEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.util.DependencyInjector;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class LoadBalancerService extends Particle {
    protected final ClockService clock;
    protected final LiquidTimestamp heartbeat;
    public LoadBalancerService(int threads, LiquidTimestamp heartbeat) {
        this.clock = new ClockService(threads);
        this.heartbeat = heartbeat;
    }

    public void init(DependencyInjector.DI3<FamilyService, PluginLogger, EventManager> deps) {
        for (IFamily family : deps.d1().dump()) {
            this.clock.scheduleRecurring(() -> {
                try {
                    PluginLogger logger = deps.d2();

                    deps.d3().fireEvent(new RebalanceEvent(family));
                    family.balance();
                    if (logger.loggerGate().check(GateKey.FAMILY_BALANCING))
                        ProxyLang.FAMILY_BALANCING.send(logger, family);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, this.heartbeat);
        }
    }

    @Override
    public void close() throws Exception {
        this.clock.kill();
    }

    public static class Tinder extends Particle.Tinder<Particle> {
        private final int threads;
        private final LiquidTimestamp delay;
        public Tinder(int threads, LiquidTimestamp delay) {
            this.threads = threads;
            this.delay = delay;
        }

        @Override
        public @NotNull Particle ignite() throws Exception {
            return new LoadBalancerService(this.threads, this.delay);
        }
    }
}
