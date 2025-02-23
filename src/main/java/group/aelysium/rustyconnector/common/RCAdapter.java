package group.aelysium.rustyconnector.common;

import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
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
    
    public abstract <T> CommandManager<T> commandManager();
}
