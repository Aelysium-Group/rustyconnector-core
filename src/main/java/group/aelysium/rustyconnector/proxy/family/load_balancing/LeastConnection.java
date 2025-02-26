package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.ara.Particle;
import group.aelysium.rustyconnector.common.algorithm.SingleSort;
import group.aelysium.rustyconnector.common.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeastConnection extends LoadBalancer {
    public static final String algorithm = "LEAST_CONNECTION";

    public LeastConnection(boolean weighted, boolean persistence, int attempts, @NotNull LiquidTimestamp rebalance) {
        super(weighted, persistence, attempts, rebalance);
    }

    @Override
    public void iterate() {
        try {
            Server thisItem = this.unlockedServers.get(this.index);
            Server theNextItem = this.unlockedServers.get(this.index + 1);

            if(thisItem.players() >= theNextItem.players()) this.index++;
        } catch (IndexOutOfBoundsException ignore) {}
    }

    @Override
    public void completeSort() {
        this.index = 0;
        if(this.weighted()) WeightedQuickSort.sort(this.unlockedServers);
        else QuickSort.sort(this.unlockedServers);
    }

    @Override
    public void singleSort() {
        this.index = 0;
        SingleSort.sortDesc(this.unlockedServers, this.index);
    }

    @Override
    public String toString() {
        return "LoadBalancer (LeastConnection): "+this.servers.size()+" items";
    }

    public static class Tinder extends LoadBalancer.Tinder<LeastConnection> {
        public Tinder(
                boolean weighted,
                boolean persistence,
                int attempts,
                @NotNull LiquidTimestamp rebalance
        ) {
            super(weighted, persistence, attempts, rebalance);
        }

        @Override
        public @NotNull LeastConnection ignite() throws Exception {
            return new LeastConnection(
                this.weighted,
                this.persistence,
                this.attempts,
                this.rebalance
            );
        }
    }
}
