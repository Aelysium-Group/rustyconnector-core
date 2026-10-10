package group.aelysium.rustyconnector.common.modules.stats;

import com.google.gson.JsonObject;
import dev.faststats.SimpleMetrics;
import dev.faststats.data.Metric;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.common.modules.Module;

final class RustyMetricsImpl extends SimpleMetrics implements RustyMetrics {
    private final Module module;

    private RustyMetricsImpl(final Factory factory, final Module module) {
        super(factory);
        this.module = module;
    }

    @Override
    protected void appendDefaultData(final JsonObject metrics) {
        final RCKernel<?> kernel = RC.Kernel();
        final String loaderType = (kernel instanceof ProxyKernel) ? "proxy" : "backend";

        metrics.addProperty("online_mode", checkOnlineMode());
        metrics.addProperty("plugin_version", "1.0"); // Modules don't currently expose a version
        metrics.addProperty("module_name", module.details() != null ? module.details().toString() : "unknown");
        metrics.addProperty("server_type", loaderType);
        //metrics.addProperty("game_version", kernel.);
        metrics.addProperty("platform_version", kernel.version());
        metrics.addProperty("player_count", getPlayerCount(kernel));
    }

    private boolean checkOnlineMode() {
        return true;
    }

    private int getPlayerCount(final RCKernel<?> kernel) {
        try {
            if (kernel instanceof ProxyKernel pk) {
                return pk.Adapter().onlinePlayers().size();
            } else if (kernel instanceof ServerKernel sk) {
                return sk.Adapter().onlinePlayerCount();
            }
            return 0;
        } catch (final Throwable t) {
            logger.error("Failed to get player count", t);
            context.errorTrackerService().ifPresent(service -> service.globalErrorTracker().trackError(t));
            return 0;
        }
    }

    public static final class Factory extends SimpleMetrics.Factory implements RustyMetrics.Factory {
        private final Module module;

        Factory(final RustyContext context, final Module module) {
            super(context);
            this.module = module;
        }

        @Override
        public Factory addMetric(final Metric<?> metric) throws IllegalArgumentException {
            return (Factory) super.addMetric(metric);
        }

        @Override
        public Factory onFlush(final Runnable flush) {
            return (Factory) super.onFlush(flush);
        }

        @Override
        public RustyMetrics create() throws IllegalStateException {
            return new RustyMetricsImpl(this, this.module);
        }
    }
}
