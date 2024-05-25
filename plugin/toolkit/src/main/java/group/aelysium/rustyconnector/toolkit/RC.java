package group.aelysium.rustyconnector.toolkit;

import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * This interface provides shorthand fetch operations for common requests.
 * All fetch operations are non-thread locking and will either return the desired outcome immediately or throw an Exception.
 * For more control over handling edge cases and issues, see {@link RustyConnector}.
 */
public interface RC {
    /**
     * The interface containing Proxy based operations.
     */
    interface P {
        static IFamilies Families() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow();
        }

        static IFamily Family(String id) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().find(id).orElseThrow().orElseThrow();
        }

        static IMCLoader MCLoader(UUID uuid) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().LocalStorage().mcloaders().fetch(uuid).orElseThrow();
        }

        static IMagicLink MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static IRemoteStorage RemoteStorage() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().RemoteStorage().orElseThrow();
        }

        static ILocalStorage LocalStorage() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().LocalStorage();
        }
    }

    /**
     * The interface containing MCLoader based operations.
     */
    interface M {

    }
}
