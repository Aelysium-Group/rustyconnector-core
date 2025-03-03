package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Flux;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.common.util.MetadataHolder;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.newlines;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public abstract class Family extends ModuleCollection<Module> implements MetadataHolder<Object>, Player.Connectable, Server.Container, ModuleHolder<Module>, Module {
    private final Map<String, Object> metadata = new ConcurrentHashMap<>(Map.of(
            "serverSoftCap", 30,
            "serverHardCap", 40
    ));
    protected final String id;
    protected final String displayName;
    protected final String parent;

    protected Family(
            @NotNull String id,
            @Nullable String displayName,
            @Nullable String parent,
            @NotNull Map<String, ?> metadata
    ) {
        if(id.length() > 16) throw new IllegalArgumentException("Family names must be no longer than 16 characters. If you want a longer name for the family, use display name.");
        if(id.isBlank()) throw new IllegalArgumentException("Please provide a valid family name.");
        this.id = id;
        this.displayName = displayName;
        this.parent = parent;
    }

    public boolean storeMetadata(String propertyName, Object property) {
        if(this.metadata.containsKey(propertyName)) return false;
        this.metadata.put(propertyName, property);
        return true;
    }
    
    @Override
    public <T> Optional<T> fetchMetadata(String propertyName) {
        return Optional.ofNullable((T) this.metadata.get(propertyName));
    }
    
    @Override
    public void removeMetadata(String propertyName) {
        this.metadata.remove(propertyName);
    }
    
    @Override
    public Map<String, Object> metadata() {
        return Collections.unmodifiableMap(this.metadata);
    }

    public @NotNull String id() {
        return this.id;
    }
    public @Nullable String displayName() {
        return this.displayName;
    }

    public abstract long players();

    /**
     * Fetches a reference to the parent of this family.
     * The parent of this family should always be either another family, or the root family.
     * If this family is the root family, this method will always return `null`.
     */
    public @NotNull Optional<Flux<Family>> parent() {
        if(this.parent == null) return Optional.empty();
        try {
            return Optional.ofNullable(RC.P.Families().find(this.parent));
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Family that = (Family) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public @Nullable Component details() {
        AtomicReference<String> parentName = new AtomicReference<>("none");
        try {
            Flux<Family> parent = this.parent().orElse(null);
            if(parent == null) throw new RuntimeException();
            parent.compute(
                f -> parentName.set(f.id()),
                ()->parentName.set("[Unavailable]"),
                10, TimeUnit.SECONDS
            );
        } catch (Exception ignore) {}

        return join(
            newlines(),
            RC.Lang("rustyconnector-keyValue").generate("Display Name", this.displayName() == null ? "No Display Name" : this.displayName()),
            RC.Lang("rustyconnector-keyValue").generate("Parent Family", parentName.get()),
            RC.Lang("rustyconnector-keyValue").generate("Servers", this.servers().size()),
            RC.Lang("rustyconnector-keyValue").generate("Players", this.players()),
            RC.Lang("rustyconnector-keyValue").generate("Plugins", text(String.join(", ",this.modules().keySet()), BLUE)),
            space(),
            text("Extra Properties:", DARK_GRAY),
            (
                this.metadata().isEmpty() ?
                    text("There are no properties to show.", DARK_GRAY)
                    :
                    join(
                        newlines(),
                        this.metadata().entrySet().stream().map(e -> RC.Lang("rustyconnector-keyValue").generate(e.getKey(), e.getValue())).toList()
                    )
            ),
            space(),
            text("Servers:", DARK_GRAY),
            (
                this.servers().isEmpty() ?
                    text("There are no servers in this family.", DARK_GRAY)
                    :
                    join(
                        newlines(),
                        this.servers().stream().map(s->{
                            boolean locked = this.isLocked(s);
                            return join(
                                JoinConfiguration.separator(empty()),
                                text("[", DARK_GRAY),
                                text(s.id(), BLUE),
                                space(),
                                text(AddressUtil.addressToString(s.address()), YELLOW),
                                text("]:", DARK_GRAY),
                                space(),
                                (
                                    s.displayName() == null ? empty() :
                                        text(Objects.requireNonNull(s.displayName()), AQUA)
                                            .append(space())
                                ),
                                text("(Players: ", DARK_GRAY),
                                text(s.players(), YELLOW),
                                text(")", DARK_GRAY),
                                space(),
                                (
                                    locked ? text("Locked", RED) : empty()
                                )
                            );
                        }).toList()
                    )
            )
        );
    }
}
