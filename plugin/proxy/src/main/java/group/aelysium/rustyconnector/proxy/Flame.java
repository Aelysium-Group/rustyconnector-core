package group.aelysium.rustyconnector.proxy;

import group.aelysium.rustyconnector.proxy.event_handlers.OnFamilyLeave;
import group.aelysium.rustyconnector.proxy.event_handlers.OnMCLoaderLeave;
import group.aelysium.rustyconnector.proxy.event_handlers.OnMCLoaderRegister;
import group.aelysium.rustyconnector.proxy.event_handlers.OnMCLoaderUnregister;
import group.aelysium.rustyconnector.proxy.local_storage.LocalStorage;
import group.aelysium.rustyconnector.proxy.magic_link.MagicLink;
import group.aelysium.rustyconnector.proxy.remote_storage.RemoteStorage;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.proxy.family.Families;
import group.aelysium.rustyconnector.toolkit.velocity.central.ProxyAdapter;
import group.aelysium.rustyconnector.common.lang.LangService;
import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.MCLoaderRegisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.MCLoaderUnregisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.FamilyLeaveEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.MCLoaderLeaveEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.velocity.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.velocity.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.velocity.storage.IRemoteStorage;
import group.aelysium.rustyconnector.toolkit.velocity.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The core RustyConnector kernel.
 * All aspects of the plugin should be accessible from here.
 * If not, check {@link Tinder}.
 */
public class Flame extends Kernel.Particle {
    private final UUID uuid;
    private final Version version;
    private final ProxyAdapter adapter;
    private final Flux<IFamilies> families;
    private final Flux<IMagicLink> magicLink;
    private final Flux<IRemoteStorage> remoteStorage;
    private final ILocalStorage localStorage;
    private final IEventManager eventManager;
    private final List<Component> bootOutput;

    protected Flame(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull ProxyAdapter adapter,
            @NotNull List<Component> bootOutput,
            @NotNull Flux<IFamilies> families,
            @NotNull Flux<IMagicLink> magicLink,
            @NotNull Flux<IRemoteStorage> remoteStorage,
            @NotNull ILocalStorage localStorage,
            @NotNull IEventManager eventManager
    ) {
        this.uuid = uuid;
        this.version = version;
        this.adapter = adapter;
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
    public ProxyAdapter Adapter() {
        return this.adapter;
    }

    @Override
    public Flux<IFamilies> Families() {
        return this.families;
    }

    @Override
    public Flux<IMagicLink> MagicLink() {
        return this.magicLink;
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

    public static class Tinder extends Kernel.Tinder {
        private final UUID uuid;
        private final Version version;
        private final ProxyAdapter adapter;
        private final Families.Tinder families;
        private final MagicLink.Tinder magicLink;
        private final RemoteStorage.Tinder remoteStorage;
        private final LocalStorage localStorage;
        private final EventManager eventManager;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull Version version,
                @NotNull ProxyAdapter adapter,
                @NotNull Families.Tinder families,
                @NotNull MagicLink.Tinder magicLink,
                @NotNull RemoteStorage.Tinder remoteStorage,
                @NotNull LocalStorage localStorage,
                @NotNull EventManager eventManager
        ) {
            super();
            this.uuid = uuid;
            this.version = version;
            this.adapter = adapter;
            this.families = families;
            this.magicLink = magicLink;
            this.remoteStorage = remoteStorage;
            this.localStorage = localStorage;
            this.eventManager = eventManager;
        }

        public Kernel.@NotNull Particle ignite() throws RuntimeException {
            this.eventManager.on(FamilyLeaveEvent.class, new OnFamilyLeave());
            this.eventManager.on(MCLoaderRegisterEvent.class, new OnMCLoaderRegister());
            this.eventManager.on(MCLoaderUnregisterEvent.class, new OnMCLoaderUnregister());
            this.eventManager.on(MCLoaderLeaveEvent.class, new OnMCLoaderLeave());

            return new Flame(
                    this.uuid,
                    this.version,
                    this.adapter,
                    new ArrayList<>(),
                    this.families.flux(),
                    this.magicLink.flux(),
                    this.remoteStorage.flux(),
                    this.localStorage,
                    this.eventManager
            );
        }
    }
}
