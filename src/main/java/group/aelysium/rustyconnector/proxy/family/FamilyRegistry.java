package group.aelysium.rustyconnector.proxy.family;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.modules.ModuleCollection;
import group.aelysium.rustyconnector.common.modules.ModuleHolder;
import group.aelysium.rustyconnector.common.modules.ModuleParticle;
import group.aelysium.rustyconnector.common.modules.ModuleTinder;
import group.aelysium.rustyconnector.proxy.events.FamilyRegisterEvent;
import group.aelysium.rustyconnector.proxy.events.FamilyUnregisterEvent;
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

public class FamilyRegistry implements ModuleHolder, ModuleParticle {
    private final ModuleCollection families = new ModuleCollection();
    private String rootFamily = null;

    protected FamilyRegistry() {}

    /**
     * Sets the root family.
     * @param familyId The family's ID to be set as root.
     */
    public void rootFamily(@NotNull String familyId) {
        this.rootFamily = familyId;
    }

    /**
     * Get the root family of this FamilyService.
     * If root family hasn't been set, this will return an empty string.
     * @return The root family id or an empty string.
     */
    public @NotNull String rootFamily() {
        if(this.rootFamily == null) return "";
        return this.rootFamily;
    }

    /**
     * Finds a family based on an id.
     * @param id The id to search for.
     */
    public Optional<Flux<? extends Family>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.fetchModule(id));
    }

    /**
     * Registers a new family.
     * @param id The id of the family to add.
     * @param tinder The family tinder to add.
     * @throws Exception If the family was not ignited and failed to ignite.
     */
    public void register(@NotNull String id, @NotNull ModuleTinder<? extends Family> tinder) throws Exception {
        Family family = (Family) this.families.registerModule(id, tinder);
        try {
            RC.EventManager().fireEvent(new FamilyRegisterEvent(family));
        } catch (Exception ignore) {}
    }

    /**
     * Remove a family from this manager.
     * @param id The id of the family to remove.
     */
    public void unregister(@NotNull String id) {
        try {
            Family family = (Family) this.families.fetchModule(id).observe(3, TimeUnit.SECONDS);
            RC.EventManager().fireEvent(new FamilyUnregisterEvent(family));
        } catch (Exception ignore) {}

        this.families.unregisterModule(id);
    }

    /**
     * Get the number of families in this {@link FamilyRegistry}.
     * @return {@link Integer}
     */
    public int size() {
        return this.families.size();
    }

    public void close() throws Exception {
        this.families.close();
    }

    @Override
    public Map<String, Flux<? extends ModuleParticle>> modules() {
        return this.families.modules();
    }

    @Override
    public @Nullable Component details() {
        List<Family> families = new ArrayList<>();
        this.modules().values().forEach(f -> f.executeNow(a -> families.add((Family) a)));

        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Total Families", this.size()),
                RC.Lang("rustyconnector-keyValue").generate("Available Families", families.size()),
                RC.Lang("rustyconnector-keyValue").generate("Root Family", this.rootFamily),
                space(),
                text("Registered Families:", DARK_GRAY),
                (
                    families.isEmpty() ?
                        text("There are no families registered.", DARK_GRAY)
                        :
                        join(
                            newlines(),
                            families.stream().map(f->{
                                return join(
                                    JoinConfiguration.separator(empty()),
                                    text("[", DARK_GRAY),
                                    text(f.id(), BLUE),
                                    text("]:", DARK_GRAY),
                                    space(),
                                    (
                                        f.displayName() == null ? empty() :
                                            text(Objects.requireNonNull(f.displayName()), AQUA)
                                                .append(space())
                                    ),
                                    text("(Players: ", DARK_GRAY),
                                    text(f.players(), GRAY),
                                    text("/", DARK_GRAY),
                                    (
                                        f.players() <= f.unlockedServers().stream().map(Server::softPlayerCap).reduce(0, Integer::sum) ?
                                            text(f.unlockedServers().stream().map(Server::softPlayerCap).reduce(0, Integer::sum), DARK_GRAY)
                                        :
                                            text(f.unlockedServers().stream().map(Server::hardPlayerCap).reduce(0, Integer::sum), RED)
                                    ),
                                    text(")", DARK_GRAY),
                                    space(),
                                    text("(Available Servers: ", DARK_GRAY),
                                    text(f.unlockedServers().size(), GRAY),
                                    (
                                        f.lockedServers().isEmpty() ? empty() :
                                            join(
                                                JoinConfiguration.separator(empty()),
                                                text("/", DARK_GRAY),
                                                text(f.servers().size(), DARK_GRAY)
                                            )
                                    ),
                                    text(")", DARK_GRAY)
                                );
                            }).toList()
                        )
                )
        );
    }

    public static class Tinder extends ModuleTinder<FamilyRegistry> {
        public Tinder() {
            super(
                "FamilyRegistry",
                "Provides indexed access to families."
            );
        }

        @Override
        public @NotNull FamilyRegistry ignite() throws Exception {
            return new FamilyRegistry();
        }

        /**
         * Returns the default configuration for a FamilyRegistry manager.
         * This default configuration has no root family set and no initial families loaded.
         */
        public static Tinder DEFAULT_CONFIGURATION = new Tinder();
    }
}
