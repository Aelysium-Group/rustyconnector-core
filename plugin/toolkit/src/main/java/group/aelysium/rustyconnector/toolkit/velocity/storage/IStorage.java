package group.aelysium.rustyconnector.toolkit.velocity.storage;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;

public interface IStorage extends Particle {
    IRemoteStorage database();
    ILocalStorage localStorage();
}
