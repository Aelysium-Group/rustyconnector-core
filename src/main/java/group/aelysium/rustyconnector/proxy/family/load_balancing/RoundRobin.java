package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.algorithm.WeightOnlyQuickSort;
import org.jetbrains.annotations.NotNull;

public class RoundRobin extends LoadBalancer {
    public static final String algorithm = "ROUND_ROBIN";

    public RoundRobin(boolean weighted, boolean persistence, int attempts) {
        super(weighted, persistence, attempts, null);
    }

    @Override
    public String toString() {
        return "LoadBalancer (RoundRobin): "+this.servers.size()+" items";
    }

    @Override
    public void completeSort() {
        if(this.weighted()) WeightOnlyQuickSort.sort(this.unlockedServers);
    }

    @Override
    public void singleSort() {}

    public static class Tinder extends Particle.Tinder<LoadBalancer> {
        private final LoadBalancer.Settings settings;

        public Tinder(@NotNull LoadBalancer.Settings settings) {
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
