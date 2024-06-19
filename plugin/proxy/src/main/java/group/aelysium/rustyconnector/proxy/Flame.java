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
import group.aelysium.rustyconnector.toolkit.common.magic_link.IMagicLink;
import group.aelysium.rustyconnector.toolkit.proxy.ProxyAdapter;
import group.aelysium.rustyconnector.toolkit.common.events.IEventManager;
import group.aelysium.rustyconnector.toolkit.proxy.IProxyFlame;
import group.aelysium.rustyconnector.toolkit.proxy.events.mc_loader.MCLoaderRegisterEvent;
import group.aelysium.rustyconnector.toolkit.proxy.events.mc_loader.MCLoaderUnregisterEvent;
import group.aelysium.rustyconnector.toolkit.proxy.events.player.FamilyLeaveEvent;
import group.aelysium.rustyconnector.toolkit.proxy.events.player.MCLoaderLeaveEvent;
import group.aelysium.rustyconnector.toolkit.proxy.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.proxy.lang.ProxyLangLibrary;
import group.aelysium.rustyconnector.toolkit.proxy.storage.ILocalStorage;
import group.aelysium.rustyconnector.toolkit.proxy.storage.IRemoteStorage;
import group.aelysium.rustyconnector.toolkit.proxy.util.Version;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Flame implements IProxyFlame {
    private final UUID uuid;
    private final Version version;
    private final ProxyAdapter adapter;
    private final Flux<ProxyLangLibrary> lang;
    private final Flux<IFamilies> families;
    private final Flux<IMagicLink.Proxy> magicLink;
    private final Flux<IRemoteStorage> remoteStorage;
    private final ILocalStorage localStorage;
    private final IEventManager eventManager;
    private final List<Component> bootOutput;

    protected Flame(
            @NotNull UUID uuid,
            @NotNull Version version,
            @NotNull ProxyAdapter adapter,
            @NotNull Flux<ProxyLangLibrary> lang,
            @NotNull List<Component> bootOutput,
            @NotNull Flux<IFamilies> families,
            @NotNull Flux<IMagicLink.Proxy> magicLink,
            @NotNull Flux<IRemoteStorage> remoteStorage,
            @NotNull ILocalStorage localStorage,
            @NotNull IEventManager eventManager
    ) {
        this.uuid = uuid;
        this.version = version;
        this.adapter = adapter;
        this.lang = lang;
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
    public Flux<ProxyLangLibrary> Lang() {
        return this.lang;
    }

    @Override
    public Flux<IFamilies> Families() {
        return this.families;
    }

    @Override
    public Flux<IMagicLink.Proxy> MagicLink() {
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

    public static class Tinder extends IProxyFlame.Tinder {
        private final UUID uuid;
        private final Version version;
        private final ProxyAdapter adapter;
        private final ProxyLangLibrary.Tinder lang;
        private final Families.Tinder families;
        private final MagicLink.Tinder magicLink;
        private final RemoteStorage.Tinder remoteStorage;
        private final LocalStorage localStorage;
        private final EventManager eventManager;

        public Tinder(
                @NotNull UUID uuid,
                @NotNull Version version,
                @NotNull ProxyAdapter adapter,
                @NotNull ProxyLangLibrary.Tinder lang,
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
            this.lang = lang;
            this.families = families;
            this.magicLink = magicLink;
            this.remoteStorage = remoteStorage;
            this.localStorage = localStorage;
            this.eventManager = eventManager;
        }

        public @NotNull Flame ignite() throws RuntimeException {
            this.eventManager.on(FamilyLeaveEvent.class, new OnFamilyLeave());
            this.eventManager.on(MCLoaderRegisterEvent.class, new OnMCLoaderRegister());
            this.eventManager.on(MCLoaderUnregisterEvent.class, new OnMCLoaderUnregister());
            this.eventManager.on(MCLoaderLeaveEvent.class, new OnMCLoaderLeave());

            return new Flame(
                    this.uuid,
                    this.version,
                    this.adapter,
                    this.lang.flux(),
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
