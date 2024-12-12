package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.algorithm.WeightOnlyQuickSort;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
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

    public static class Tinder extends LoadBalancer.Tinder<RoundRobin> {
        public Tinder(
                boolean weighted,
                boolean persistence,
                int attempts,
                @NotNull LiquidTimestamp rebalance
        ) {
            super(weighted, persistence, attempts, rebalance);
        }

        @Override
        public @NotNull RoundRobin ignite() throws Exception {
            return new RoundRobin(
                    this.weighted,
                    this.persistence,
                    this.attempts
            );
        }
    }
}
