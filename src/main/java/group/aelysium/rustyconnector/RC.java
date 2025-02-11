package group.aelysium.rustyconnector;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangNode;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.server.ServerAdapter;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This interface provides shorthand fetch operations for common requests.
 * All fetch operations are non-thread locking and will either return the desired outcome immediately or throw an Exception.
 * For more control over handling edge cases and issues, see {@link RustyConnector}.<br/><br/>
 * You can make requests to the Proxy instance using {@link P} and to the Server instance using {@link S}.
 * Some data providers are shared between both and can be accessed via {@link RC} without defining the proxy or server.
 */
public interface RC {
    /**
     * The interface containing Proxy based operations.
     */
    interface P {
        static ProxyKernel Kernel() throws NoSuchElementException {
            Particle.Flux<? extends RCKernel<?>> flux = RustyConnector.kernel.get();
            if(flux == null) throw new NoSuchElementException("No Proxy Kernel has been registered for RustyConnector.");
            if(!flux.exists()) throw new NoSuchElementException("The RustyConnector Proxy Kernel is currently unavailable. It might be rebooting.");
            return (ProxyKernel) flux.orElseThrow();
        }

        static FamilyRegistry Families() throws NoSuchElementException {
            return (FamilyRegistry) P.Kernel().fetchModule("FamilyRegistry")
                    .orElseThrow(()->new NoSuchElementException("The Family Registry is not currently available. It might be rebooting."));
        }

        static PlayerRegistry Players() throws NoSuchElementException {
            return (PlayerRegistry) P.Kernel().fetchModule("PlayerRegistry")
                    .orElseThrow(()->new NoSuchElementException("The Player Registry is not currently available. It might be rebooting."));
        }

        static MagicLinkCore.Proxy MagicLink() throws NoSuchElementException {
            return (MagicLinkCore.Proxy) P.Kernel().fetchModule("MagicLink")
                    .orElseThrow(()->new NoSuchElementException("The Magic Link module is not currently available. It might be rebooting."));
        }

        static EventManager EventManager() throws NoSuchElementException {
            return (EventManager) P.Kernel().fetchModule("EventManager")
                    .orElseThrow(()->new NoSuchElementException("The Event Manager is not currently available. It might be rebooting."));
        }

        static HazeProvider Haze() throws NoSuchElementException {
            return (HazeProvider) P.Kernel().fetchModule("Haze")
                    .orElseThrow(()->new NoSuchElementException("The Haze Provider is not currently available. It might be rebooting or you haven't installed an RC module that implemented it."));
        }

        static ProxyAdapter Adapter() throws NoSuchElementException {
            return RC.P.Kernel().Adapter();
        }

        static LangLibrary Lang() throws NoSuchElementException {
            return (LangLibrary) P.Kernel().fetchModule("LangLibrary")
                    .orElseThrow(()->new NoSuchElementException("The Language Registry is not currently available. It might be rebooting."));
        }

        static ErrorRegistry Errors() {
            return (ErrorRegistry) P.Kernel().fetchModule("ErrorRegistry")
                    .orElseThrow(()->new NoSuchElementException("The Error Registry is not currently available. It might be rebooting."));
        }

        static Optional<? extends Family> Family(String id) throws NoSuchElementException {
            Family family = RC.P.Families().find(id)
                    .orElseThrow(()->new NoSuchElementException("No family with the id "+id+" exists."))
                    .orElseThrow(()->new NoSuchElementException("The family "+id+" is not currently available. it might be rebooting."));
            return Optional.ofNullable(family);
        }

        static Optional<Particle.Flux<? extends Family>> Family(Server server) throws NoSuchElementException {
            for (Particle.Flux<? extends ModuleParticle> f : RC.P.Families().modules().values()) {
                try {
                    Family family = (Family) f.access().get(5, TimeUnit.SECONDS);
                    if (!family.containsServer(server.id())) continue;
                    return Optional.of(((Particle.Flux<? extends Family>) f));
                } catch (InterruptedException | CancellationException | ExecutionException | TimeoutException ignore) {}
            }
            return Optional.empty();
        }

        static Optional<Server> Server(String id) throws NoSuchElementException {
            AtomicReference<Server> server = new AtomicReference<>(null);
            Families().modules().values().forEach(flux -> flux.executeNow(f->((Family) f).fetchServer(id).ifPresent(server::set)));
            return Optional.ofNullable(server.get());
        }

        static List<Server> Servers() throws NoSuchElementException {
            List<Server> servers = new ArrayList<>();
            Families().modules().values().forEach(flux -> flux.executeNow(f->servers.addAll(((Family) f).servers())));
            return Collections.unmodifiableList(servers);
        }

        static Optional<Player> Player(UUID uuid) throws NoSuchElementException {
            return RC.P.Players().fetch(uuid);
        }
        static Optional<Player> Player(String username) throws NoSuchElementException {
            return RC.P.Players().fetch(username);
        }

        static Optional<? extends HazeDatabase> Haze(String name) throws NoSuchElementException {
            Particle.Flux<? extends HazeDatabase> flux = P.Haze().fetchDatabase(name).orElse(null);
            if(flux == null) return Optional.empty();
            return Optional.of(flux.orElseThrow());
        }

    }

    /**
     * The interface containing Server based operations.
     */
    interface S {
        static ServerKernel Kernel() throws NoSuchElementException {
            Particle.Flux<? extends RCKernel<?>> flux = RustyConnector.kernel.get();
            if(flux == null) throw new NoSuchElementException("No Server Kernel has been registered for RustyConnector.");
            if(!flux.exists()) throw new NoSuchElementException("The RustyConnector Server Kernel is currently unavailable. It might be rebooting.");
            return (ServerKernel) flux.orElseThrow();
        }

        static MagicLinkCore.Server MagicLink() throws NoSuchElementException {
            return (MagicLinkCore.Server) S.Kernel().fetchModule("MagicLink")
                    .orElseThrow(()->new NoSuchElementException("The Magic Link module is not currently available. It might be rebooting."));
        }

        static ServerAdapter Adapter() throws NoSuchElementException {
            return RC.S.Kernel().Adapter();
        }

        static LangLibrary Lang() throws NoSuchElementException {
            return (LangLibrary) S.Kernel().fetchModule("LangLibrary")
                    .orElseThrow(()->new NoSuchElementException("The Language Registry is not currently available. It might be rebooting."));
        }

        static EventManager EventManager() throws NoSuchElementException {
            return (EventManager) S.Kernel().fetchModule("EventManager")
                    .orElseThrow(()->new NoSuchElementException("The Event Manager is not currently available. It might be rebooting."));
        }

        static ErrorRegistry Errors() {
            return (ErrorRegistry) S.Kernel().fetchModule("ErrorRegistry")
                    .orElseThrow(()->new NoSuchElementException("The Error Registry is not currently available. It might be rebooting."));
        }

        static HazeProvider Haze() throws NoSuchElementException {
            return (HazeProvider) S.Kernel().fetchModule("Haze")
                    .orElseThrow(()->new NoSuchElementException("The Haze Provider is not currently available. It might be rebooting or you haven't installed an RC module that implemented it."));
        }

        static Optional<? extends HazeDatabase> Haze(String name) throws NoSuchElementException {
            Particle.Flux<? extends HazeDatabase> flux = S.Haze().fetchDatabase(name).orElse(null);
            if(flux == null) return Optional.empty();
            return Optional.of(flux.orElseThrow());
        }
    }

    static RCKernel<? extends RCAdapter> Kernel() throws NoSuchElementException {
        try {
            return RC.S.Kernel();
        } catch (Exception ignore) {}
        try {
            return RC.P.Kernel();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("No RustyConnector kernels currently exist.");
    }

    static void Error(@NotNull Error error) throws NoSuchElementException {
        try {
            RC.Errors().register(error);
        } catch (Exception e) {
            if(error.throwable() != null) {
                error.throwable().printStackTrace();
                return;
            }
            System.out.println(error.message());
        }
    }

    static ErrorRegistry Errors() throws NoSuchElementException {
        try {
            return RC.S.Errors();
        } catch (Exception ignore) {}
        try {
            return RC.P.Errors();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested ErrorRegistry doesn't exist.");
    }

    static LangLibrary Lang() throws NoSuchElementException {
        try {
            return RC.S.Lang();
        } catch (Exception ignore) {}
        try {
            return RC.P.Lang();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested LangLibrary doesn't exist.");
    }

    static LangNode Lang(String id) throws NoSuchElementException {
        return RC.Lang().lang(id);
    }

    static EventManager EventManager() throws NoSuchElementException {
        try {
            return RC.S.EventManager();
        } catch (Exception ignore) {}
        try {
            return RC.P.EventManager();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested EventManager doesn't exist.");
    }

    static MagicLinkCore MagicLink() throws NoSuchElementException {
        try {
            return RC.S.MagicLink();
        } catch (Exception ignore) {}
        try {
            return RC.P.MagicLink();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested MagicLink provider doesn't exist.");
    }

    static RCAdapter Adapter() throws NoSuchElementException {
        try {
            return RC.S.Adapter();
        } catch (Exception ignore) {}
        try {
            return RC.P.Adapter();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested adapter doesn't exist.");
    }

    static HazeProvider Haze() throws NoSuchElementException {
        try {
            return RC.S.Haze();
        } catch (Exception ignore) {}
        try {
            return RC.P.Haze();
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested Haze provider doesn't exist.");
    }

    static Optional<? extends HazeDatabase> Haze(String name) throws NoSuchElementException {
        try {
            return RC.S.Haze(name);
        } catch (Exception ignore) {}
        try {
            return RC.P.Haze(name);
        } catch (Exception ignore) {}
        throw new NoSuchElementException("The requested Haze database doesn't exist.");
    }
}
