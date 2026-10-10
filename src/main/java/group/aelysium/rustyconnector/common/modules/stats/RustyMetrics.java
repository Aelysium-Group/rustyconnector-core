package group.aelysium.rustyconnector.common.modules.stats;

import dev.faststats.Metrics;
import dev.faststats.data.Metric;

/**
 * Bukkit metrics implementation.
 *
 * @since 0.1.0
 */
public sealed interface RustyMetrics extends Metrics permits RustyMetricsImpl {
    sealed interface Factory extends Metrics.Factory permits RustyMetricsImpl.Factory {
        @Override
        Factory addMetric(Metric<?> metric) throws IllegalArgumentException;

        @Override
        Factory onFlush(Runnable flush);

        @Override
        RustyMetrics create() throws IllegalStateException;
    }
}
