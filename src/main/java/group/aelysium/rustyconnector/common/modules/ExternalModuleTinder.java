package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class ExternalModuleTinder<P extends ModuleParticle> extends Particle.Tinder<P> {
    public ExternalModuleTinder() {}

    /**
     * Runs after {@link #onStart()} successfully returns an instance.
     * This is where you can register into the RustyConnector Kernel.
     * Example usages would be registering Lang nodes or adding events to the EventListener.
     * @param kernel The running kernel that's ready to be bound to.
     * @param instance The Particle instance that was just created by {@link #ignite()}.
     */
    public void bind(@NotNull ProxyKernel kernel, @NotNull Particle instance) {}

    /**
     * Runs after {@link #onStart()} successfully returns an instance.
     * This is where you can register into the RustyConnector Kernel.
     * Example usages would be registering Lang nodes or adding events to the EventListener.
     * @param kernel The running kernel that's ready to be bound to.
     * @param instance The Particle instance that was just created by {@link #ignite()}.
     */
    public void bind(@NotNull ServerKernel kernel, @NotNull Particle instance) {}

    /**
     * Runs when the RustyConnector kernel is ready to load your module.
     * This method should only be used to configure and start your module, you shouldn't interact with the RustyConnector kernel here.
     * @return The fully configured and running instance of your module.
     * @throws Exception If there was any issue initializing your module.
     */
    public abstract @NotNull P onStart() throws Exception;

    public final @NotNull P ignite() throws Exception {
        return this.onStart();
    }
}