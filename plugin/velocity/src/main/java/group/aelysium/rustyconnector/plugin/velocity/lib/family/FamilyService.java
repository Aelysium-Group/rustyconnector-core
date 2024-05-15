package group.aelysium.rustyconnector.plugin.velocity.lib.family;

import group.aelysium.rustyconnector.plugin.velocity.lib.family.ranked_family.RankedFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.scalar_family.RootFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.static_family.StaticFamily;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyConnector;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamilyService;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FamilyService extends IFamilyService {
    private final Map<String, Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>>> families = new ConcurrentHashMap<>();
    private WeakReference<IRootFamily> rootFamily;

    protected FamilyService() {}

    public void setRootFamily(IRootFamily family) {
        this.families.put(family.id(), family);
        this.rootFamily = new WeakReference<>(family);
    }

    public IRootFamily rootFamily() {
        return this.rootFamily.get();
    }

    public Optional<Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>>> find(@NotNull String id) {
        return Optional.ofNullable(this.families.get(id));
    }

    public void put(@NotNull String id, @NotNull Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>> family) {
        this.families.put(id, family);
    }

    public void remove(@NotNull String id) {
        this.families.remove(id);
    }

    public List<Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>>> dump() {
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
        for (Particle.Flux<? extends IFamily<? extends IFamilyConnector<? extends IMCLoader>>> family : this.families.values()) {
            try {
                family.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.families.clear();
        this.rootFamily.clear();
    }

    public static class Tinder extends Particle.Tinder<IFamilyService> {
        private final IFamilyService.Settings settings;

        public Tinder(IFamilyService.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull IFamilyService ignite() throws Exception {
            FamilyService service = new FamilyService();

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