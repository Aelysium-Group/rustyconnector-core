package group.aelysium.rustyconnector.common;

import group.aelysium.rustyconnector.proxy.player.Player;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public abstract class RCAdapter {
    /**
     * Logs the components as a message into the console.
     * @param component The component to log.
     */
    public abstract void log(@NotNull Component component);

    /**
     * Logs the specified component into the console.
     * @param component The component to log.
     */
    public abstract void messagePlayer(@NotNull UUID player, @NotNull Component component);
}
