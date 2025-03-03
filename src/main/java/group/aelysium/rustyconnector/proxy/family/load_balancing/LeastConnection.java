package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.common.algorithm.SingleSort;
import group.aelysium.rustyconnector.common.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LeastConnection extends LoadBalancer {
    public static final String algorithm = "LEAST_CONNECTION";

    public LeastConnection(
        boolean weighted,
        boolean persistence,
        int attempts,
        @NotNull LiquidTimestamp rebalance,
        @NotNull Map<String, Object> metadata
    ) {
        super(weighted, persistence, attempts, rebalance, metadata);
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
}
