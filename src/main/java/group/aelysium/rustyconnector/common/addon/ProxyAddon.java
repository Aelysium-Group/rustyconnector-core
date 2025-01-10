package group.aelysium.rustyconnector.common.addon;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.proxy.ProxyKernel;

public abstract class ProxyAddon implements Particle {
    public abstract void start(ProxyKernel kernel);
    public abstract void stop();
}
