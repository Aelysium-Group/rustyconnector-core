package group.aelysium.lib.ranked_game_interface.ranks;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import group.aelysium.rustyconnector.toolkit.common.matchmaking.IPlayerRank;

public class WinRatePlayerRank implements IPlayerRank {
    public static String schema() {
        return "WIN_RATE";
    }

    protected int wins;
    protected int losses;
    protected int ties;

    public WinRatePlayerRank(int wins, int losses, int ties) {
        this.wins = wins;
        this.losses = losses;
        this.ties = ties;
    }
    public WinRatePlayerRank() {
        this(0, 0, 0);
    }

    public double rank() {
        int games = wins + losses + ties;
        if (games == 0) return 0;
        return (double) wins / games;
    }

    public String schemaName() {
        return schema();
    }

    @Override
    public JsonObject toJSON() {
        JsonObject object = new JsonObject();
        object.add("schema", new JsonPrimitive(this.schemaName()));
        object.add("wins", new JsonPrimitive(this.wins));
        object.add("losses", new JsonPrimitive(this.losses));
        object.add("ties", new JsonPrimitive(this.ties));
        return object;
    }
}
