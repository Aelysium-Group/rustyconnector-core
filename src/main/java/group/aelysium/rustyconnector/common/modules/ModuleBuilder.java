package group.aelysium.rustyconnector.common.modules;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class ModuleBuilder<P extends ModuleParticle> implements Supplier<P> {
    public final String name;
    public final String description;
    
    public ModuleBuilder(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }
}