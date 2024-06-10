package group.aelysium.rustyconnector.toolkit;

import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.common.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.mc_loader.MCLoaderAdapter;
import group.aelysium.rustyconnector.toolkit.velocity.ProxyAdapter;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;

import java.util.NoSuchElementException;
import java.util.Optional;
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

        static IMagicLink.Proxy MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static IRemoteStorage RemoteStorage() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().RemoteStorage().orElseThrow();
        }

        static ILocalStorage LocalStorage() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().LocalStorage();
        }

        static IEventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().EventManager();
        }

        static ProxyAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Adapter();
        }

        static Optional<IFamily> Family(String id) throws NoSuchElementException {
            IFamily family = null;
            try {
                family = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().find(id).orElseThrow().orElseThrow();
            } catch (Exception ignore) {}
            return Optional.ofNullable(family);
        }

        static Optional<IMCLoader> MCLoader(UUID uuid) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().LocalStorage().mcloaders().fetch(uuid);
        }

        static Optional<IPlayer> Player(UUID uuid) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().LocalStorage().players().fetch(uuid);
        }
    }

    /**
     * The interface containing MCLoader based operations.
     */
    interface M {
        static IMagicLink.MCLoader MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.MCLoader().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static MCLoaderAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.MCLoader().orElseThrow().orElseThrow().Adapter();
        }

        static IEventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.MCLoader().orElseThrow().orElseThrow().EventManager();
        }
    }
}
