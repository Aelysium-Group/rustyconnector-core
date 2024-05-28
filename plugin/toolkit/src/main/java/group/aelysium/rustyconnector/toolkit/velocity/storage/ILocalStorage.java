package group.aelysium.rustyconnector.toolkit.velocity.storage;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.mcloader.IMCLoader;

import java.util.Optional;
import java.util.UUID;

/**
 * Local storage is the internal memory solution used for short-term internal RustyConnector data.
 * Local storage will not persist between software restarts since it's stored in RAM.
 */
public interface ILocalStorage extends Particle {
    MCLoaders mcloaders();
    Players players();

    interface MCLoaders extends Particle {
        void store(UUID uuid, IMCLoader mcloader);
        Optional<IMCLoader> fetch(UUID uuid);
        void remove(UUID uuid);
    }
    interface Players extends Particle {
        void store(UUID uuid, IPlayer player);
        Optional<IPlayer> fetch(UUID uuid);
        void remove(UUID uuid);
    }
}
