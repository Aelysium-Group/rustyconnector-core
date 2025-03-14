package group.aelysium.rustyconnector.common.modules;

import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.server.ServerKernel;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public abstract class ExternalModuleBuilder<P extends Module> {
    /**
     * Runs after {@link #onStart(Path)} successfully returns an instance and is registered into the RustyConnector kernel.
     * This method will only be run when your module is first registered to the kernel, or when the kernel is restarted.
     * It should be used to specifically link into kernel resources on a one-off basis.
     * Example usages would be registering Lang nodes or adding events to the EventListener.
     * @param kernel The running kernel that's ready to be bound to.
     * @param instance The Particle instance that was just created.
     */
    public void bind(@NotNull ProxyKernel kernel, @NotNull P instance) {}

    /**
     * Runs after {@link #onStart(Path)} successfully returns an instance and is registered into the RustyConnector kernel for the first time.
     * This method will only be run when your module is first registered to the kernel, or when the kernel is restarted.
     * It should be used to specifically link into kernel resources on a one-off basis.
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
    public abstract @NotNull P onStart(@NotNull Path dataDirectory) throws Exception;
}