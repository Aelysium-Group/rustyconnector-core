package group.aelysium.rustyconnector.server;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Provides access to RustyConnector-specific environment variables.
 */
public class Environment {
    /**
     * Fetches the pod name from the associated environment variable.<br/>
     * The environment variable that this method references is "RC_POD_NAME".
     * @return An optional containing the value of the environment variable if it exists, otherwise `null`.
     */
    public static Optional<String> podName() {
        try {
            return Optional.ofNullable(System.getenv("RC_POD_NAME"));
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Optional.empty();
        }
    }

    /**
     * Fetches the address from the associated environment variable.<br/>
     * The environment variable that this method references is "RC_ADDRESS".
     * @return An optional containing the value of the environment variable if it exists, otherwise `null`.
     */
    public static Optional<InetSocketAddress> address() {
        try {
            String address = System.getenv("RC_ADDRESS");
            if(address == null) return Optional.empty();
            return Optional.of(AddressUtil.parseAddress(address));
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Optional.empty();
        }
    }

    /**
     * Fetches the display name from the associated environment variable.<br/>
     * The environment variable that this method references is "RC_DISPLAY_NAME".
     * @return An optional containing the value of the environment variable if it exists, otherwise `null`.
     */
    public static Optional<String> displayName() {
        try {
            return Optional.ofNullable(System.getenv("RC_DISPLAY_NAME"));
        } catch (Exception e) {
            RC.Error(Error.from(e));
            return Optional.empty();
        }
    }
}