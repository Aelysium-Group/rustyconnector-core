package group.aelysium.rustyconnector.core.mcloader.lib.ranked_game_interface;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import group.aelysium.rustyconnector.core.TinderAdapterForCore;
import group.aelysium.rustyconnector.core.mcloader.lib.server_info.ServerInfoService;
import group.aelysium.rustyconnector.toolkit.core.packet.GenericPacket;
import group.aelysium.rustyconnector.toolkit.core.packet.PacketIdentification;
import group.aelysium.rustyconnector.toolkit.mc_loader.events.ranked_game.RankedGameEndEvent;
import group.aelysium.rustyconnector.toolkit.mc_loader.ranked_game_interface.IRankedGameInterfaceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RankedGameInterfaceService implements IRankedGameInterfaceService {
    private UUID uuid;
    private List<UUID> players;

    public Optional<UUID> uuid() {
        if(this.uuid == null) return Optional.empty();
        return Optional.of(this.uuid);
    }

    public Optional<List<UUID>> players() {
        if(this.players == null) return Optional.empty();
        return Optional.of(this.players);
    }

    public void session(UUID uuid, List<UUID> players) {
        this.uuid = uuid;
        this.players = players;
    }

    public void end(UUID... winners) {
        if(this.uuid == null) return;

        ServerInfoService infoService = TinderAdapterForCore.getTinder().services().serverInfo();

        JsonObject object = new JsonObject();
        object.add("uuid", new JsonPrimitive(uuid.toString()));

        JsonArray winnerUUIDs = new JsonArray();
        JsonArray loserUUIDs = new JsonArray();

        List<UUID> sessionPlayers = new ArrayList<>(players);

        for (UUID uuid : winners) {
            if(!sessionPlayers.contains(uuid)) continue;
            sessionPlayers.remove(uuid);
            winnerUUIDs.add(uuid.toString());
        }
        for (UUID uuid : sessionPlayers) {
            loserUUIDs.add(uuid.toString());
        }

        object.add("winners", winnerUUIDs);
        object.add("losers", loserUUIDs);

        TinderAdapterForCore.getTinder().services().events().fire(new RankedGameEndEvent(uuid, sessionPlayers, List.of(winners)));

        GenericPacket packet = new GenericPacket.MCLoaderPacketBuilder()
                .identification(PacketIdentification.Predefined.END_RANKED_GAME)
                .sendingToProxy(infoService.uuid())
                .parameter("session", object.toString())
                .build();
        TinderAdapterForCore.getTinder().services().magicLink().connection().orElseThrow().publish(packet);

        this.uuid = null;
        this.players = null;
    }

    public void kill() {
        this.uuid = null;
        this.players = null;
    }
}
