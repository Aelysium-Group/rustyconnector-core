package group.aelysium.rustyconnector.common.errors;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.plugins.PluginTinder;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ErrorRegistry implements Particle {
    private final boolean logErrors;
    private final int cacheSize;
    private final Vector<Error> errors = new Vector<>() {
        @Override
        public synchronized boolean add(Error element) {
            if (this.size() >= cacheSize) { this.remove(0); }
            return super.add(element);
        }
    };

    protected ErrorRegistry(boolean logErrors, int cacheSize) {
        this.logErrors = logErrors;
        this.cacheSize = cacheSize;
    }

    public boolean logErrors() {
        return this.logErrors;
    }
    public int cacheSize() {
        return this.cacheSize;
    }

    public void register(Error error) {
        this.errors.add(error);
        if(this.logErrors || error.urgent()) RC.Adapter().log(error.toComponent());
    }

    public Optional<Error> fetch(@NotNull UUID uuid) {
        return this.errors.stream().filter(e -> e.uuid().equals(uuid)).findFirst();
    }

    public List<Error> fetchAll() {
        return Collections.unmodifiableList(this.errors);
    }

    @Override
    public void close() throws Exception {
        this.errors.clear();
    }

    public static class Tinder extends PluginTinder<ErrorRegistry> {
        private boolean logErrors = false;
        private int cacheSize = 200;

        public Tinder() {
            super(
                "ErrorRegistry",
                "Provides error capture and logging services.",
                "rustyconnector-errorRegistryDetails"
            );
        }

        public Tinder logErrors(boolean logErrors) {
            this.logErrors = logErrors;
            return this;
        }

        public Tinder cacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        @Override
        public @NotNull ErrorRegistry ignite() throws Exception {
            return new ErrorRegistry(
                    this.logErrors,
                    this.cacheSize
            );
        }

        public static PluginTinder<? extends ErrorRegistry> DEFAULT_CONFIGURATION = new Tinder();
    }
}
