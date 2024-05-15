package group.aelysium.rustyconnector.toolkit.velocity.central;

import group.aelysium.rustyconnector.toolkit.core.lang.ILangService;
import group.aelysium.rustyconnector.toolkit.core.lang.ILanguageResolver;
import group.aelysium.rustyconnector.toolkit.core.logger.PluginLogger;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyService;
import group.aelysium.rustyconnector.toolkit.velocity.util.Version;
import net.kyori.adventure.text.Component;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * The root api endpoint for the entire RustyConnector api.
 */
public interface Kernel {
    abstract class Particle implements group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle {

        /**
         * Gets the session uuid of this Proxy.
         * The Proxy's uuid won't change while it's alive, but once it's restarted or reloaded, the session uuid will change.
         * @return {@link UUID}
         */
        public abstract UUID uuid();

        /**
         * Gets the current version of RustyConnector
         * @return {@link Version}
         */
        public abstract Version version();

        public abstract Flux<IFamilyService> Families();
        public abstract Flux<?> MagicLink();
        public abstract Flux<?> Storage();
        public abstract Flux<?> Players();

        /**
         * Allows access to the {@link PluginLogger} used by RustyConnector.
         * @return {@link PluginLogger}
         */
        public abstract PluginLogger logger();

        /**
         * Allows access to RustyConnector's data folder.
         * @return {@link String}
         */
        public abstract Path dataFolder();

        public abstract ILangService<? extends ILanguageResolver> lang();

        /**
         * Gets a resource by name and returns it as a stream.
         * @param filename The name of the resource to get.
         * @return The resource as a stream.
         */
        static InputStream resourceAsStream(String filename)  {
            return Kernel.class.getClassLoader().getResourceAsStream(filename);
        }

        /**
         * Gets RustyConnector's boot log.
         * The log represents all the debug messages sent during the boot or reboot of RustyConnector.
         * The log is in the same order of when the logs were sent.
         * @return {@link List < Component >}
         */
        public abstract List<Component> bootLog();
    }

    abstract class Tinder extends group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle.Tinder<Particle> {
    }
}
