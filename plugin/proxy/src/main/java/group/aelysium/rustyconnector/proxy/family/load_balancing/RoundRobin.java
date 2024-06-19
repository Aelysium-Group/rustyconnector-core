package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.algorithm.WeightOnlyQuickSort;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.proxy.family.load_balancing.ILoadBalancer;
import org.jetbrains.annotations.NotNull;

public class RoundRobin extends LoadBalancer {
    public RoundRobin(boolean weighted, boolean persistence, int attempts) {
        super(weighted, persistence, attempts, null);
    }

    @Override
    public String toString() {
        return "LoadBalancer (RoundRobin): "+this.size()+" items";
    }

    @Override
    public void completeSort() {
        if(this.weighted()) WeightOnlyQuickSort.sort(this.unlockedServers);
    }

    @Override
    public void singleSort() {}

    public static class Tinder extends Particle.Tinder<ILoadBalancer> {
        private final ILoadBalancer.Settings settings;

        public Tinder(@NotNull ILoadBalancer.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull RoundRobin ignite() throws Exception {
            return new RoundRobin(
                    this.settings.weighted(),
                    this.settings.persistence(),
                    this.settings.attempts()
            );
        }
    }
}
