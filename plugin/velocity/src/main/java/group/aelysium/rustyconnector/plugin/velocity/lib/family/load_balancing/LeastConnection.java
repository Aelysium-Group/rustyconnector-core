package group.aelysium.rustyconnector.plugin.velocity.lib.family.load_balancing;

import group.aelysium.rustyconnector.core.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.core.common.algorithm.SingleSort;
import group.aelysium.rustyconnector.core.common.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

public class LeastConnection extends LoadBalancer {
    public LeastConnection(boolean weighted, boolean persistence, int attempts, @NotNull LiquidTimestamp rebalance) {
        super(weighted, persistence, attempts, rebalance);
    }

    @Override
    public void iterate() {
        try {
            IMCLoader thisItem = this.unlockedServers.get(this.index);
            IMCLoader theNextItem = this.unlockedServers.get(this.index + 1);

            if(thisItem.playerCount() >= theNextItem.playerCount()) this.index++;
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
        return "LoadBalancer (LeastConnection): "+this.size()+" items";
    }

    public static class Tinder extends Particle.Tinder<ILoadBalancer> {
        private final ILoadBalancer.Settings settings;

        public Tinder(@NotNull ILoadBalancer.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull ILoadBalancer ignite() throws Exception {
            return new LeastConnection(
                    this.settings.weighted(),
                    this.settings.persistence(),
                    this.settings.attempts(),
                    this.settings.rebalance()
                    );
        }
    }
}
