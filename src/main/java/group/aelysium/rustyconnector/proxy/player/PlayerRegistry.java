package group.aelysium.rustyconnector.proxy.player;

import group.aelysium.rustyconnector.common.modules.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public interface PlayerRegistry extends Module {
    /**
     * Handles a player which has signed in.
     */
    void signedIn(@NotNull Player player);
    
    /**
     * Handles a player which has signed out.
     */
    void signedOut(@NotNull Player player);
    
    
    Optional<Player> fetchByID(@NotNull String id);
    Optional<Player> fetchByUsername(@NotNull String username);
    
    @NotNull Set<Player> onlinePlayers();
}
