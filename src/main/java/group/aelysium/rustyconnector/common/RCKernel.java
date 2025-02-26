package group.aelysium.rustyconnector.common;

import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import group.aelysium.rustyconnector.proxy.util.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public abstract class RCKernel<A extends RCAdapter> extends ModuleCollection implements ModuleParticle {
    protected final String id;
    protected final Version version;
    protected final A adapter;
    protected final Path directory;
    protected final Path moduleDirectory;

    protected RCKernel(
            @NotNull String id,
            @NotNull Version version,
            @NotNull A adapter,
            @NotNull Path directory,
            @NotNull Path moduleDirectory,
            @NotNull List<? extends ModuleTinder<?>> modules
    ) {
        if(id.length() > 64) throw new IllegalArgumentException("The Kernel ID cannot be longer than 64 characters.");

        this.id = id;
        this.version = version;
        this.adapter = adapter;
        this.directory = directory;
        this.moduleDirectory = moduleDirectory;
        modules.forEach(t -> {
            try {
                this.registerModule(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * @return The id of this kernel.
     *         The id shouldn't change between re-boots.
     */
    public String id() {
        return this.id;
    }
    
    /**
     * @return The directory that the RustyConnector kernel jar is located.
     *         This path use useful for deciding where to load configuration files.
     */
    public Path directory() {
        return this.directory;
    }
    
    /**
     * @return The directory that RustyConnector modules are located (typically an `rc-modules` folder)
     *         This path use useful for deciding where to load configuration files.
     */
    public Path moduleDirectory() {
        return this.directory;
    }

    /**
     * @return The current version of RustyConnector
     */
    public Version version() {
        return this.version;
    }

    public A Adapter() {
        return this.adapter;
    }

    @Override
    public void close() throws Exception {
        super.close();
    }

    public static abstract class Tinder<B extends RCAdapter,T extends RCKernel<B>> extends ModuleTinder<T> {
        protected final String id;
        protected final Path directory;
        protected final Path modulesDirectory;
        protected final B adapter;
        protected ModuleTinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        protected ModuleTinder<? extends ErrorRegistry> errors = ErrorRegistry.Tinder.DEFAULT_CONFIGURATION;
        protected ModuleTinder<? extends LangLibrary> lang = LangLibrary.Tinder.DEFAULT_LANG_LIBRARY;

        public Tinder(
                @NotNull String id,
                @NotNull B adapter,
                @NotNull Path directory,
                @NotNull Path modulesDirectory
        ) {
            super(
                    "Kernel",
                    "The RustyConnector Kernel"
            );

            this.id = id;
            this.adapter = adapter;
            this.directory = directory;
            this.modulesDirectory = modulesDirectory;
        }
    }
}
