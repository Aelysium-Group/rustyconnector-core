package group.aelysium.rustyconnector.common.errors;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.Module;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.JoinConfiguration.newlines;

public class ErrorRegistry implements Module {
    private final boolean logErrors;
    private final int cacheSize;
    private final Vector<Error> errors = new Vector<>() {
        @Override
        public synchronized boolean add(Error element) {
            if (this.size() >= cacheSize) { this.remove(0); }
            return super.add(element);
        }
    };

    public ErrorRegistry(boolean logErrors, int cacheSize) {
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

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Log Errors", this.logErrors),
                RC.Lang("rustyconnector-keyValue").generate("Cache Size", this.cacheSize),
                RC.Lang("rustyconnector-keyValue").generate("Error Count", this.errors.size())
        );
    }
}
