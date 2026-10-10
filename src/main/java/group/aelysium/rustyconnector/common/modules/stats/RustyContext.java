package group.aelysium.rustyconnector.common.modules.stats;

import dev.faststats.SimpleContext;
import dev.faststats.Token;
import dev.faststats.config.SimpleConfig;
import dev.faststats.internal.LoggerFactory;
import dev.faststats.internal.PlatformLoggerFactory;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.modules.Module;
import org.jetbrains.annotations.Contract;

import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Rusty FastStats context.
 *
 * @since 0.24.0
 */
public final class RustyContext extends SimpleContext {
    private final Set<Runnable> cancellations = new CopyOnWriteArraySet<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Module module;

    private RustyContext(final Factory factory, final LoggerFactory loggerFactory, final Module module, @Token final String token) {
        super(factory, loggerFactory, SimpleConfig.read(getConfigPath(), loggerFactory), "rustyconnector", token);
        this.module = module;
        initializeServices(factory);
    }

    @Override
    @Contract(value = " -> new", pure = true)
    protected RustyMetrics.Factory metricsFactory() {
        return new RustyMetricsImpl.Factory(this, this.module);
    }

    @Override
    public void ready() {
        super.ready();
    }

    private static Path getConfigPath() {
        return RC.Kernel().moduleConfigDirectory().resolve("faststats").resolve("config.properties");
    }

    @Override
    protected boolean preSubmissionStart() {
        return ((SimpleConfig) getConfig()).preSubmissionStart(this);
    }

    @Override
    public String getProjectName() {
        return RC.Kernel().id();
    }

    @Override
    protected void scheduleAtFixedRate(final Runnable task, final long initialDelay, final long period, final TimeUnit unit) {
        final ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
        cancellations.add(() -> scheduledTask.cancel(false));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cancellations.forEach(Runnable::run);
        cancellations.clear();
        scheduler.shutdown();
    }

    public static final class Factory extends SimpleContext.Factory<RustyContext, Factory> {
        private final Module module;
        private final @Token String token;

        public Factory(final Module module, @Token final String token) {
            this.module = module;
            this.token = token;
        }

        @Override
        public RustyContext create() {
            final var loggerFactory = new PlatformLoggerFactory((level, throwable, message) -> {
                try {
                    String levelStr = "[" + level.name() + "] ";
                    Component component = Component.text(levelStr + message);
                    RC.Adapter().log(component);
                    if (throwable != null) {
                        Error error = Error.from(throwable);
                        RC.Error(error);
                    }
                } catch (Exception ignore) {
                    System.out.println("[FastStats] " + level + ": " + message);
                    if (throwable != null) throwable.printStackTrace();
                }
            });
            return new RustyContext(this, loggerFactory, module, token);
        }
    }
}
