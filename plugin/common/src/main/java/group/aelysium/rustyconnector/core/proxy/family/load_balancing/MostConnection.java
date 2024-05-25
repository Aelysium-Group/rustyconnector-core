package group.aelysium.rustyconnector.core.proxy.family.load_balancing;

import group.aelysium.rustyconnector.core.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.core.common.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class MostConnection extends LeastConnection {
    public MostConnection(boolean weighted, boolean persistence, int attempts, @NotNull LiquidTimestamp rebalance) {
        super(weighted, persistence, attempts, rebalance);
    }

    @Override
    public void iterate() {
        try {
            IMCLoader currentItem = this.unlockedServers.get(this.index);

            if(currentItem.playerCount() + 1 > currentItem.hardPlayerCap()) this.index++;
            if(this.index >= this.unlockedServers.size()) this.index = 0;
        } catch (IndexOutOfBoundsException ignore) {}
    }

    
    @Override
    public void completeSort() {
        this.index = 0;
        if(this.weighted()) WeightedQuickSort.sort(this.unlockedServers);
        else {
            QuickSort.sort(this.unlockedServers);
            Collections.reverse(this.unlockedServers);
        }
    }

    @Override
    public String toString() {
        return "LoadBalancer (MostConnection): "+this.size()+" items";
    }

    public static class Tinder extends Particle.Tinder<ILoadBalancer> {
        private final ILoadBalancer.Settings settings;

        public Tinder(@NotNull ILoadBalancer.Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull ILoadBalancer ignite() throws Exception {
            return new MostConnection(
                    this.settings.weighted(),
                    this.settings.persistence(),
                    this.settings.attempts(),
                    this.settings.rebalance()
            );
        }
    }
}
