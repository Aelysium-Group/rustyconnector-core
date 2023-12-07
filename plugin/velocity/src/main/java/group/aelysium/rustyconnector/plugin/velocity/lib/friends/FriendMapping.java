package group.aelysium.rustyconnector.plugin.velocity.lib.friends;

import group.aelysium.rustyconnector.toolkit.velocity.friends.IFriendMapping;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.Player;

import java.util.Objects;

public class FriendMapping implements IFriendMapping<Player> {
    private final Player player1;
    private final Player player2;

    protected FriendMapping(Player player1, Player player2) {
        // Ensure that players are always in order of the lowest uuid to the highest uuid.
        if(player1.uuid().compareTo(player2.uuid()) > 0) {
            this.player1 = player2;
            this.player2 = player1;

            return;
        }

        this.player1 = player1;
        this.player2 = player2;
    }

    public Player player1() {
        return player1;
    }

    public Player player2() {
        return player2;
    }

    public boolean contains(Player player) {
        return this.player1.equals(player) || this.player2.equals(player);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FriendMapping that = (FriendMapping) o;
        return Objects.equals(player1, that.player1) && Objects.equals(player2, that.player2);
    }

    public static FriendMapping from(Player player1, Player player2) {
        return new FriendMapping(player1, player2);
    }

    public Player fetchOther(Player player) {
        if(this.player1.equals(player)) return this.player2;
        if(this.player2.equals(player)) return this.player1;

        return null;
    }
}