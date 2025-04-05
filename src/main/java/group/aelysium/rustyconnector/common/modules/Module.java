package group.aelysium.rustyconnector.common.modules;

import group.aelysium.ara.Closure;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface Module extends Closure {
    /**
     * Returns a {@link Component} which describes the internal details of this module.
     * If there's no details to show, can just return null.
     * @return A Component describing additional details about the module.
     */
    @Nullable Component details();
    
    abstract class Builder<P extends Module> implements Supplier<P> {
        public final String name;
        public final String description;
        
        public Builder(@NotNull String name, @NotNull String description) {
            this.name = name;
            this.description = description;
        }
    }
}
