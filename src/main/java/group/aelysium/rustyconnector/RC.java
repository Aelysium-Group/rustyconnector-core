package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.server.ServerAdapter;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.lang.ServerLang;
import group.aelysium.rustyconnector.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.proxy.lang.ProxyLang;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

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
        static ProxyKernel Kernel() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow();
        }

        static FamilyRegistry Families() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().FamilyRegistry().orElseThrow();
        }

        static PlayerRegistry Players() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().PlayerRegistry().orElseThrow();
        }

        static Optional<? extends Whitelist> Whitelist() throws NoSuchElementException {
            Optional<Particle.Flux<? extends Whitelist>> whitelistOptional = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Whitelist();
            if(whitelistOptional.isEmpty()) return Optional.empty();
            return Optional.of(whitelistOptional.orElseThrow().orElseThrow());
        }

        static MagicLinkCore.Proxy MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().EventManager().orElseThrow();
        }

        static ProxyAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Adapter();
        }

        static LangLibrary<? extends ProxyLang> Lang() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Lang().orElseThrow();
        }

        static Optional<? extends Family> Family(String id) throws NoSuchElementException {
            Family family = null;
            try {
                family = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().FamilyRegistry().orElseThrow().find(id).orElseThrow().orElseThrow();
            } catch (Exception ignore) {}
            return Optional.ofNullable(family);
        }

        static Optional<Server> Server(UUID uuid) throws NoSuchElementException {
            FamilyRegistry familyRegistry = RC.P.Families();
            AtomicReference<Server> server = new AtomicReference<>(null);
            for (Particle.Flux<? extends Family> family : familyRegistry.dump()) {
                family.executeNow(f -> {
                    f.servers().stream().filter(s -> s.uuid().equals(uuid)).findAny().ifPresent(server::set);
                });
                if(server.get() != null) break;
            }
            return Optional.ofNullable(server.get());
        }

        static Optional<Player> Player(UUID uuid) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().PlayerRegistry().orElseThrow().fetch(uuid);
        }
        static Optional<Player> Player(String username) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().PlayerRegistry().orElseThrow().fetch(username);
        }
    }

    /**
     * The interface containing Server based operations.
     */
    interface S {
        /**
         * Returns the RustyConnector Kernel.
         * @return {@link ProxyKernel}
         * @throws NoSuchElementException If the kernel cannot be found.
         */
        static ServerKernel Kernel() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow();
        }

        static MagicLinkCore.Server MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static ServerAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().Adapter();
        }

        static LangLibrary<? extends ServerLang> Lang() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().Lang().orElseThrow();
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().EventManager().orElseThrow();
        }
    }
}
