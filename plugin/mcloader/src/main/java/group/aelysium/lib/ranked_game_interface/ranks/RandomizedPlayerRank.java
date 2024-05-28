package group.aelysium.lib.ranked_game_interface.ranks;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import group.aelysium.rustyconnector.toolkit.common.matchmaking.IPlayerRank;

public class RandomizedPlayerRank implements IPlayerRank {
    public static String schema() {
        return "RANDOMIZED";
    }
    private static final RandomizedPlayerRank singleton = new RandomizedPlayerRank();

    protected RandomizedPlayerRank() {}
    public static RandomizedPlayerRank New() { return singleton; }

    public double rank() { return 1.0; }

    public String schemaName() {
        return schema();
    }

    @Override
    public JsonObject toJSON() {
        JsonObject object = new JsonObject();
        object.add("schema", new JsonPrimitive(this.schemaName()));
        return object;
    }
}
