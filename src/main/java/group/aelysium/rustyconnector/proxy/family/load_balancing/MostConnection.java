package group.aelysium.rustyconnector.proxy.family.load_balancing;

import group.aelysium.rustyconnector.common.algorithm.QuickSort;
import group.aelysium.rustyconnector.common.algorithm.WeightedQuickSort;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class MostConnection extends LeastConnection {
    public static final String algorithm = "MOST_CONNECTION";

    public MostConnection(boolean weighted, boolean persistence, int attempts, @NotNull LiquidTimestamp rebalance) {
        super(weighted, persistence, attempts, rebalance);
    }

    @Override
    public void iterate() {
        try {
            Server currentItem = this.unlockedServers.get(this.index);

            if(currentItem.players() + 1 > currentItem.hardPlayerCap()) this.index++;
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
        return "LoadBalancer (MostConnection): "+this.servers.size()+" items";
    }
}
