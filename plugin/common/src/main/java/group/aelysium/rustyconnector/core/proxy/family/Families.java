package group.aelysium.rustyconnector.core.proxy.family;

import group.aelysium.rustyconnector.core.proxy.family.ranked_family.RankedFamily;
import group.aelysium.rustyconnector.core.proxy.family.scalar_family.RootFamily;
import group.aelysium.rustyconnector.core.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.core.proxy.family.static_family.StaticFamily;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilies;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Families extends IFamilies {
    private final Map<String, Flux<IFamily>> families = new ConcurrentHashMap<>();
    private String rootFamily;

    protected Families() {}

    public void setRootFamily(Flux<IFamily> family) {
        if(!(family.orElseThrow() instanceof IRootFamily rootFamily)) return;
        this.families.put(rootFamily.id(), family);
        this.rootFamily = rootFamily.id();
    }

    public Flux<IFamily> rootFamily() {
        return this.families.get(this.rootFamily);
    }

    public Optional<Particle.Flux<IFamily>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.get(id));
    }

    public void put(@NotNull String id, @NotNull Particle.Flux<IFamily> family) {
        this.families.put(id, family);
    }

    public void remove(@NotNull String id) {
        this.families.remove(id);
    }

    public List<Particle.Flux<IFamily>> dump() {
        return this.families.values().stream().toList();
    }

    public void clear() {
        this.families.clear();
    }

    public int size() {
        return this.families.size();
    }

    public void close() throws Exception {
        // Teardown logic for any families that need it
        for (Particle.Flux<IFamily> family : this.families.values()) {
            try {
                family.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.families.clear();
    }

    public static class Tinder extends Particle.Tinder<IFamilies> {
        private final IFamilies.Settings settings;

        public Tinder(IFamilies.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull IFamilies ignite() throws Exception {
            Families service = new Families();

            {
                service.put(this.settings.rootFamily().id(), new RootFamily.Tinder(this.settings.rootFamily()).flux());
            }
            {
                this.settings.scalarFamilies().forEach(s ->
                        service.put(s.id(), new ScalarFamily.Tinder(s).flux())
                );
            }
            {
                this.settings.staticFamilies().forEach(s ->
                        service.put(s.id(), new StaticFamily.Tinder(s).flux())
                );
            }
            {
                this.settings.rankedFamilies().forEach(s ->
                        service.put(s.id(), new RankedFamily.Tinder(s).flux())
                );
            }
            
            // Access will initiate ignition for all the fluxes.
            service.dump().forEach(Flux::access);

            return service;
        }
    }
}