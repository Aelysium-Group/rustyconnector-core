package group.aelysium.rustyconnector;

import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.server.ServerAdapter;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.server.lang.ServerLang;
import group.aelysium.rustyconnector.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.proxy.ProxyFlame;
import group.aelysium.rustyconnector.proxy.family.Families;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.whitelist.Whitelist;
import group.aelysium.rustyconnector.proxy.lang.ProxyLang;
import group.aelysium.rustyconnector.proxy.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.proxy.player.Players;

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
        static ProxyFlame Kernel() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow();
        }

        static Families Families() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow();
        }

        static Players Players() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Players().orElseThrow();
        }

        static Optional<Whitelist> Whitelist() throws NoSuchElementException {
            Optional<Particle.Flux<Whitelist>> whitelistOptional = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Whitelist();
            if(whitelistOptional.isEmpty()) return Optional.empty();
            return Optional.of(whitelistOptional.orElseThrow().orElseThrow());
        }

        static WebSocketMagicLink MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().EventManager().orElseThrow();
        }

        static ProxyAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Adapter();
        }

        static LangLibrary<ProxyLang> Lang() throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Lang().orElseThrow();
        }

        static Optional<Family> Family(String id) throws NoSuchElementException {
            Family family = null;
            try {
                family = RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Families().orElseThrow().find(id).orElseThrow().orElseThrow();
            } catch (Exception ignore) {}
            return Optional.ofNullable(family);
        }

        static Optional<Server> Server(UUID uuid) throws NoSuchElementException {
            Families families = RC.P.Families();
            AtomicReference<Server> server = new AtomicReference<>(null);
            for (Particle.Flux<Family> family : families.dump()) {
                family.executeNow(f -> {
                    f.servers().stream().filter(s -> s.uuid().equals(uuid)).findAny().ifPresent(server::set);
                });
                if(server.get() != null) break;
            }
            return Optional.ofNullable(server.get());
        }

        static Optional<Player> Player(UUID uuid) throws NoSuchElementException {
            return RustyConnector.Toolkit.Proxy().orElseThrow().orElseThrow().Players().orElseThrow().fetch(uuid);
        }
    }

    /**
     * The interface containing Server based operations.
     */
    interface S {
        /**
         * Returns the RustyConnector Kernel.
         * @return {@link ProxyFlame}
         * @throws NoSuchElementException If the kernel cannot be found.
         */
        static ServerFlame Kernel() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow();
        }

        static group.aelysium.rustyconnector.server.magic_link.WebSocketMagicLink MagicLink() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().MagicLink().orElseThrow();
        }

        static ServerAdapter Adapter() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().Adapter();
        }

        static LangLibrary<ServerLang> Lang() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().Lang().orElseThrow();
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RustyConnector.Toolkit.Server().orElseThrow().orElseThrow().EventManager().orElseThrow();
        }
    }
}
