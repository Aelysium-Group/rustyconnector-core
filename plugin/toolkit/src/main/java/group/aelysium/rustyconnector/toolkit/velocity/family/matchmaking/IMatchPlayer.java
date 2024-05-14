package group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking;

import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ISortable;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;

public interface IMatchPlayer extends ISortable {
    IPlayer player();
    IVelocityPlayerRank gameRank();
    String gameId();
}
