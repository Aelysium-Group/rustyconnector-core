package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.algorithm.WeightOnlyQuickSort;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RoundRobin extends LoadBalancer {
    public static final String algorithm = "ROUND_ROBIN";

    public RoundRobin(
        boolean weighted,
        boolean persistence,
        int attempts,
        @NotNull Map<String, Object> metadata
    ) {
        super(weighted, persistence, attempts, null, metadata);
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
}
