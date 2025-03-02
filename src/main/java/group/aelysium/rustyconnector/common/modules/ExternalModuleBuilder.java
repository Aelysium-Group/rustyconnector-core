package group.aelysium.rustyconnector.common.modules;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ExternalModuleBuilder<P extends ModuleParticle> implements Supplier<P> {
    /**
     * Runs after {@link #onStart()} successfully returns an instance.
     * This is where you can register into the RustyConnector Kernel.
     * Example usages would be registering Lang nodes or adding events to the EventListener.
     * @param kernel The running kernel that's ready to be bound to.
     * @param instance The Particle instance that was just created.
     */
    public void bind(@NotNull ProxyKernel kernel, @NotNull P instance) {}

    /**
     * Runs after {@link #onStart()} successfully returns an instance.
     * This is where you can register into the RustyConnector Kernel.
     * Example usages would be registering Lang nodes or adding events to the EventListener.
     * @param kernel The running kernel that's ready to be bound to.
     * @param instance The Particle instance that was just created.
     */
    public void bind(@NotNull ServerKernel kernel, @NotNull P instance) {}

    /**
     * Runs when the RustyConnector kernel is ready to load your module.
     * This method should only be used to configure and start your module, you shouldn't interact with the RustyConnector kernel here.
     * @return The fully configured and running instance of your module.
     * @throws Exception If there was any issue initializing your module.
     */
    public abstract @NotNull P onStart() throws Exception;

    public final P get() {
        try {
            return this.onStart();
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        return null;
    }
}