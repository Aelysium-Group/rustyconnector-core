package group.aelysium.rustyconnector.toolkit.velocity.magic_link;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnection;

import java.net.ConnectException;
import java.util.Optional;

public abstract class IMagicLink implements Particle {
    /**
     * Gets the connection to the remote resource.
     * @return {@link IMessengerConnection}
     */
    public abstract Optional<IMessengerConnection> connection();

    /**
     * Connect to the remote resource.
     *
     * @return A {@link IMessengerConnection}.
     * @throws ConnectException If there was an issue connecting to the remote resource.
     */
    public abstract IMessengerConnection connect() throws ConnectException;

    /**
     * Fetches a Magic Link MCLoader Config based on a name.
     * `name` is considered to be the name of the file found in `magic_configs` on the Proxy, minus the file extension.
     * @param name The name to look for.
     */
    public abstract Optional<MagicLinkMCLoaderSettings> magicConfig(String name);

    public record MagicLinkMCLoaderSettings(
            String family,
            int weight,
            int soft_cap,
            int hard_cap
    ) {};
}
