package group.aelysium.rustyconnector.toolkit.proxy;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.common.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.proxy.lang.ProxyLangLibrary;
import group.aelysium.rustyconnector.toolkit.proxy.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.proxy.storage.IRemoteStorage;
import group.aelysium.rustyconnector.toolkit.proxy.util.Version;

import java.io.InputStream;
import java.util.UUID;

public interface IProxyFlame extends Particle {
    /**
     * Gets the uuid of this Proxy.
     * The Proxy's uuid shouldn't change between re-boots unless you delete the files on the Proxy.
     * @return {@link UUID}
     */
    UUID uuid();

    /**
     * Gets the current version of RustyConnector
     * @return {@link Version}
     */
    Version version();
    ProxyAdapter Adapter();
    Flux<ProxyLangLibrary> Lang();
    Flux<IFamilies> Families();
    Flux<IMagicLink.Proxy> MagicLink();
    Flux<IRemoteStorage> RemoteStorage();
    ILocalStorage LocalStorage();
    IEventManager EventManager();

    /**
     * Gets a resource by name and returns it as a stream.
     * @param filename The name of the resource to get.
     * @return The resource as a stream.
     */
    static InputStream resourceAsStream(String filename)  {
        return IProxyFlame.class.getClassLoader().getResourceAsStream(filename);
    }

    abstract class Tinder extends Particle.Tinder<IProxyFlame> {}
}
