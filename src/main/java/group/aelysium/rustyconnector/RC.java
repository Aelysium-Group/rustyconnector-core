package group.aelysium.rustyconnector;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.common.haze.HazeDatabase;
import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.RCAdapter;
import group.aelysium.rustyconnector.common.RCKernel;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangNode;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.modules.Module;
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
            Flux<? extends RCKernel<?>> flux = RustyConnector.kernel.get();
            if(flux == null) throw new NoSuchElementException("No Proxy Kernel has been registered for RustyConnector.");
            if(flux.isEmpty()) throw new NoSuchElementException("The RustyConnector Proxy Kernel is currently unavailable. It might be rebooting.");
            return (ProxyKernel) flux.orElseThrow();
        }

        static FamilyRegistry Families() throws NoSuchElementException {
            return RC.Module("FamilyRegistry");
        }

        static PlayerRegistry Players() throws NoSuchElementException {
            return RC.Module("PlayerRegistry");
        }

        static MagicLinkCore.Proxy MagicLink() throws NoSuchElementException {
            return RC.Module("MagicLink");
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RC.Module("EventManager");
        }

        static HazeProvider Haze() throws NoSuchElementException {
            return RC.Module("Haze");
        }

        static ProxyAdapter Adapter() throws NoSuchElementException {
            return RC.P.Kernel().Adapter();
        }

        static LangLibrary Lang() throws NoSuchElementException {
            return RC.Module("LangLibrary");
        }

        static ErrorRegistry Errors() {
            return RC.Module("ErrorRegistry");
        }

        static Optional<Family> Family(String id) throws NoSuchElementException {
            try {
                return Optional.ofNullable(P.Families().find(id)
                    .orElseThrow(() -> new NoSuchElementException("The family " + id + " is not currently available. it might be rebooting.")));
            } catch (NullPointerException ignore) {}
            throw new NoSuchElementException("No family with the id " + id + " exists.");
        }

        static Optional<Family> Family(Server server) throws NoSuchElementException {
            for (Flux<Family> f : RC.P.Families().modules().values()) {
                try {
                    Family family = f.orElseThrow();
                    if (!family.containsServer(server.id())) continue;
                    return Optional.of(family);
                } catch (Exception ignore) {}
            }
            return Optional.empty();
        }

        static Optional<Server> Server(String id) throws NoSuchElementException {
            AtomicReference<Server> server = new AtomicReference<>(null);
            Families().modules().values().forEach(flux -> flux.ifPresent(f-> f.fetchServer(id).ifPresent(server::set)));
            return Optional.ofNullable(server.get());
        }

        static List<Server> Servers() throws NoSuchElementException {
            List<Server> servers = new ArrayList<>();
            Families().modules().values().forEach(flux -> flux.ifPresent(f->servers.addAll(f.servers())));
            return Collections.unmodifiableList(servers);
        }

        static Optional<Player> Player(UUID uuid) throws NoSuchElementException {
            return RC.P.Players().fetch(uuid);
        }
        static Optional<Player> Player(String username) throws NoSuchElementException {
            return RC.P.Players().fetch(username);
        }

        static Optional<HazeDatabase> Haze(String name) throws NoSuchElementException {
            Flux<HazeDatabase> flux = P.Haze().fetchDatabase(name);
            if(flux == null) return Optional.empty();
            return Optional.of(flux.orElseThrow());
        }

    }

    /**
     * The interface containing Server based operations.
     */
    interface S {
        static ServerKernel Kernel() throws NoSuchElementException {
            Flux<? extends RCKernel<?>> flux = RustyConnector.kernel.get();
            if(flux == null) throw new NoSuchElementException("No Server Kernel has been registered for RustyConnector.");
            if(flux.isEmpty()) throw new NoSuchElementException("The RustyConnector Server Kernel is currently unavailable. It might be rebooting.");
            return (ServerKernel) flux.orElseThrow();
        }

        static MagicLinkCore.Server MagicLink() throws NoSuchElementException {
            return RC.Module("MagicLink");
        }

        static ServerAdapter Adapter() throws NoSuchElementException {
            return RC.S.Kernel().Adapter();
        }

        static LangLibrary Lang() throws NoSuchElementException {
            return RC.Module("LangLibrary");
        }

        static EventManager EventManager() throws NoSuchElementException {
            return RC.Module("EventManager");
        }

        static ErrorRegistry Errors() {
            return RC.Module("ErrorRegistry");
        }

        static HazeProvider Haze() throws NoSuchElementException {
            return RC.Module("Haze");
        }

        static Optional<HazeDatabase> Haze(String name) throws NoSuchElementException {
            Flux<HazeDatabase> flux = S.Haze().fetchDatabase(name);
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
    
    static <M extends Module> M Module(@NotNull String moduleName) throws NoSuchElementException {
        return ((Flux<M>) RC.ModuleFlux(moduleName)).orElseThrow(()->new NoSuchElementException(moduleName+" is not currently available. It might be rebooting."));
    }
    static <M extends Module> Flux<M> ModuleFlux(@NotNull String moduleName) throws NoSuchElementException {
        Flux<M> f = RC.Kernel().fetchModule(moduleName);
        if(f == null) throw new NoSuchElementException(moduleName+" is not currently available. It might be rebooting.");
        return f;
    }
    
    static boolean checkAvailability(@NotNull String moduleName) {
        try {
            RC.Kernel().fetchModule(moduleName).orElseThrow();
            return true;
        } catch (Exception ignore) {}
        return false;
    }
}
