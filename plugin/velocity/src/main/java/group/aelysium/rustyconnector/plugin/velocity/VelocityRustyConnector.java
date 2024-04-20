package group.aelysium.rustyconnector.plugin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import group.aelysium.rustyconnector.core.lib.events.EventManager;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc.*;
import group.aelysium.rustyconnector.plugin.velocity.lib.config.ConfigService;
import group.aelysium.rustyconnector.plugin.velocity.lib.config.configs.FamiliesConfig;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.FamilyService;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.PlayerService;
import group.aelysium.rustyconnector.toolkit.RustyConnector;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.ProxyLang;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.core.events.Event;
import group.aelysium.rustyconnector.toolkit.core.events.Listener;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.RegisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.UnregisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.FamilyLeaveEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.FamilySwitchEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.MCLoaderLeaveEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.MCLoaderSwitchEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VelocityRustyConnector {
    private final Metrics.Factory metricsFactory;
    private final Particle.Flux<Kernel.Particle> kernel;
    private final PluginLogger logger;
    private final ProxyServer server;

    @Inject
    public VelocityRustyConnector(ProxyServer server, Logger logger, @DataDirectory Path dataFolder, Metrics.Factory metricsFactory) {
        this.kernel = new Tinder(this, dataFolder).flux();
        this.logger = new PluginLogger(logger);
        this.server = server;

        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onLoad(ProxyInitializeEvent event) {
        this.logger.log("Initializing RustyConnector...");

        if(!this.server.getConfiguration().isOnlineMode())
            logger.log("Offline mode detected");

        this.kernel.optimistic(flame -> {
            // RustyConnector Event Manager
            {
                Map<Class<? extends Event>, Listener<? extends Event>> listeners = new HashMap<>();
                listeners.put(FamilyLeaveEvent.class, new OnFamilyLeave());
                listeners.put(FamilySwitchEvent.class, new OnFamilySwitch());
                listeners.put(RegisterEvent.class, new OnMCLoaderRegister());
                listeners.put(UnregisterEvent.class, new OnMCLoaderUnregister());
                listeners.put(MCLoaderSwitchEvent.class, new OnMCLoaderSwitch());
                listeners.put(MCLoaderLeaveEvent.class, new OnMCLoaderLeave());
                flame.capacitor().store("events", new EventManager.Tinder(listeners));
            }
            flame.capacitor().store("players", new PlayerService.Tinder());

            {
                FamiliesConfig config = FamiliesConfig.construct(dataFolder, lang, deps.d4());
                flame.capacitor().store("families", new FamilyService.Tinder(config));
            }

            flame.capacitor().store("mcloaders", new PlayerService.Tinder());
            capacitor.store("storage", new PlayerService.Tinder());
            capacitor.store("whitelists", new PlayerService.Tinder());
            capacitor.store("magic_link", new PlayerService.Tinder());
        });

        try {
            metricsFactory.make(this, 17972);
            Tinder.get().logger().log("Registered to bstats!");
        } catch (Exception e) {
            e.printStackTrace();
            Tinder.get().logger().log("Failed to register to bstats!");
        }

        ProxyLang.WORDMARK_RUSTY_CONNECTOR.send(Tinder.get().logger(), "v"+Tinder.get().flame().version().toString());
        RustyConnector.Toolkit.register(Tinder.get());

        if(!Tinder.get().velocityServer().getConfiguration().isOnlineMode())
            Tinder.get().logger().send(ProxyLang.BOXED_MESSAGE_COLORED.build("Your network is running in offline mode! YOU WILL RECEIVE NO SUPPORT AT ALL WITH RUSTYCONNECTOR!", NamedTextColor.RED));

        // Velocity requires that at least one server is always defined in velocity.toml
        if(Tinder.get().velocityServer().getConfiguration().getServers().size() > 1)
            Tinder.get().logger().send(ProxyLang.BOXED_COMPONENT_COLORED.build(
                    Component.join(
                            JoinConfiguration.newlines(),
                            Component.text("Your network is identified as having multiple, pre-defined, non-RC servers, in it!"),
                            Component.text("Please note that you will receive no help in regards to making RC work with predefined servers!")
                    )
                    , NamedTextColor.RED));
    }

    @Subscribe
    public void onUnload(ProxyShutdownEvent event) {
        try {
            RustyConnector.Toolkit.unregister();
            this.kernel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
