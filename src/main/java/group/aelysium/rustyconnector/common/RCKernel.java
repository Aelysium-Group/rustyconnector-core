package group.aelysium.rustyconnector.common;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import group.aelysium.rustyconnector.proxy.util.Version;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class RCKernel<A extends RCAdapter> extends ModuleCollection implements Particle {
    protected final String id;
    protected final Version version;
    protected final A adapter;

    protected RCKernel(
            @NotNull String id,
            @NotNull Version version,
            @NotNull A adapter,
            @NotNull List<? extends ModuleTinder<?>> modules
    ) {
        this.id = id;
        this.version = version;
        this.adapter = adapter;
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
        protected final B adapter;
        protected ModuleTinder<? extends EventManager> eventManager = EventManager.Tinder.DEFAULT_CONFIGURATION;
        protected ModuleTinder<? extends ErrorRegistry> errors = ErrorRegistry.Tinder.DEFAULT_CONFIGURATION;
        protected ModuleTinder<? extends LangLibrary> lang = LangLibrary.Tinder.DEFAULT_LANG_LIBRARY;

        public Tinder(
                @NotNull String id,
                @NotNull B adapter
        ) {
            super(
                    "Kernel",
                    "The RustyConnector Kernel",
                    "rustyconnector-kernelDetails"
            );

            this.id = id;
            this.adapter = adapter;
        }
    }
}
