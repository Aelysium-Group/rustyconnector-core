package group.aelysium.rustyconnector.toolkit.velocity.player;

import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public interface IPlayer {
    UUID uuid();
    String username();

    /**
     * Check whether the Player is online.
     * @return `true` if the player is online. `false` otherwise.
     */
    boolean online();

    /**
     * Convenience method that will resolve the player and then send a message to them if the resolution was successful.
     * If the resolution was not successful, nothing will happen.
     * @param message The message to send.
     */
    void sendMessage(Component message);

    /**
     * Convenience method that will resolve the player and then disconnect them if the resolution was successful.
     * If the resolution was not successful, nothing will happen.
     * @param reason The message to send as the reason for the disconnection.
     */
    void disconnect(Component reason);

    /**
     * Convenience method that will resolve the player and then return their MCLoader if there is one.
     */
    Optional<IMCLoader> server();
}