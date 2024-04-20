package group.aelysium.rustyconnector.plugin.velocity.central;

import com.velocitypowered.api.proxy.ProxyServer;
import group.aelysium.rustyconnector.toolkit.mc_loader.central.IMCLoaderTinder;
import group.aelysium.rustyconnector.toolkit.velocity.central.Kernel;
import group.aelysium.rustyconnector.toolkit.velocity.util.Version;
import group.aelysium.rustyconnector.core.lib.lang.LangService;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import net.kyori.adventure.text.Component;

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
    private final Flux.Capacitor capacitor;
    private final List<Component> bootOutput;

    protected Flame(Flux.Capacitor capacitor, UUID uuid, Version version, List<Component> bootOutput) {
        this.capacitor = capacitor;
        this.uuid = uuid;
        this.version = version;
        this.bootOutput = bootOutput;
    }

    public Version version() {
        return this.version;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public List<Component> bootLog() { return this.bootOutput; }

    public Flux.Capacitor capacitor() {
        return this.capacitor;
    }

    public void close() throws Exception {
        this.capacitor.close();
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
