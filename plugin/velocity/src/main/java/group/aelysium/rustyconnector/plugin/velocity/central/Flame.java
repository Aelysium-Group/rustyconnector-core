package group.aelysium.rustyconnector.plugin.velocity.central;

import com.velocitypowered.api.proxy.ProxyServer;
import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.velocity.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;
import group.aelysium.rustyconnector.toolkit.velocity.util.Version;
import group.aelysium.rustyconnector.common.lang.LangService;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * The core RustyConnector kernel.
 * All aspects of the plugin should be accessible from here.
 * If not, check {@link Tinder}.
 */
public class Flame extends Kernel.Particle {
    private final UUID uuid;
    private final Version version;
    private final Flux<IFamilies> families;
    private final Flux<IMagicLink> magicLink;
    private final Flux<IRemoteStorage> remoteStorage;
    private final ILocalStorage localStorage;
    private final IEventManager eventManager;
    private final List<Component> bootOutput;

    protected Flame(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull List<Component> bootOutput,
            @NotNull Flux<IFamilies> families,
            @NotNull Flux<IMagicLink> magicLink,
            @NotNull Flux<IRemoteStorage> remoteStorage,
            @NotNull ILocalStorage localStorage,
            @NotNull IEventManager eventManager
    ) {
        this.uuid = uuid;
        this.version = version;
        this.bootOutput = bootOutput;
        this.families = families;
        this.magicLink = magicLink;
        this.remoteStorage = remoteStorage;
        this.localStorage = localStorage;
        this.eventManager = eventManager;
    }

    public Version version() {
        return this.version;
    }

    @Override
    public Flux<IFamilies> Families() {
        return null;
    }

    @Override
    public Flux<IMagicLink> MagicLink() {
        return null;
    }

    @Override
    public Flux<IRemoteStorage> RemoteStorage() {
        return this.remoteStorage;
    }

    @Override
    public ILocalStorage LocalStorage() {
        return this.localStorage;
    }

    @Override
    public IEventManager EventManager() {
        return this.eventManager;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public List<Component> bootLog() { return this.bootOutput; }

    public void close() throws Exception {
        this.families.close();
        this.magicLink.close();
        this.remoteStorage.close();
        this.localStorage.close();
        this.eventManager.close();
        this.bootOutput.clear();
    }

    public PluginLogger logger() {
        return this.pluginLogger;
    }

    public Path dataFolder() {
        return this.dataFolder;
    }

    public LangService lang() {
        return this.lang;
    }

    /**
     * Gets a resource by id and returns it as a stream.
     * @param filename The id of the resource to get.
     * @return The resource as a stream.
     */
    public static InputStream resourceAsStream(String filename)  {
        return IMCLoaderTinder.class.getClassLoader().getResourceAsStream(filename);
    }

    /**
     * Get the velocity server
     */
    public ProxyServer velocityServer() {
        return this.server;
    }
}
