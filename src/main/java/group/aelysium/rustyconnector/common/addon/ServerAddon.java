package group.aelysium.rustyconnector.common.addon;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.server.ServerKernel;

public abstract class ServerAddon implements Particle {
    public abstract void start(ServerKernel kernel);
    public abstract void stop(); 
}