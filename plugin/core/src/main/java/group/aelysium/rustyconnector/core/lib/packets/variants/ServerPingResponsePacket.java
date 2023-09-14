package group.aelysium.rustyconnector.core.lib.packets.variants;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import group.aelysium.rustyconnector.core.lib.packets.PacketOrigin;
import group.aelysium.rustyconnector.core.lib.packets.PacketType;
import group.aelysium.rustyconnector.core.lib.packets.GenericPacket;
import group.aelysium.rustyconnector.core.lib.util.ColorMapper;
import io.lettuce.core.KeyValue;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerPingResponsePacket extends GenericPacket {
    private PingResponseStatus status;
    private String message;
    private NamedTextColor color;
    private Optional<Integer> pingInterval = Optional.empty();

    public PingResponseStatus status() {
        return status;
    }
    public String message() {
        return message;
    }
    public NamedTextColor color() {
        return color;
    }
    public Optional<Integer> pingInterval() {
        return pingInterval;
    }

    public ServerPingResponsePacket(InetSocketAddress address, PacketOrigin origin, List<KeyValue<String, JsonPrimitive>> parameters) {
        super(PacketType.PING_RESPONSE, address, origin);

        if(!ServerPingResponsePacket.validateParameters(ValidParameters.toList(), parameters))
            throw new IllegalStateException("Unable to construct Redis message! There are missing parameters!");

        parameters.forEach(entry -> {
            String key = entry.getKey();
            JsonPrimitive value = entry.getValue();

            switch (key) {
                case ValidParameters.STATUS -> this.status = PingResponseStatus.valueOf(value.getAsString());
                case ValidParameters.MESSAGE -> this.message = value.getAsString();
                case ValidParameters.COLOR -> this.color = ColorMapper.map(value.getAsString());
                case ValidParameters.INTERVAL_OPTIONAL -> this.pingInterval = Optional.of(value.getAsInt());
            }
        });
    }
    public ServerPingResponsePacket(int messageVersion, String rawMessage, InetSocketAddress address, PacketOrigin origin, List<KeyValue<String, JsonPrimitive>> parameters) {
        super(messageVersion, rawMessage, PacketType.PING_RESPONSE, address, origin);

        if(!ServerPingResponsePacket.validateParameters(ValidParameters.toList(), parameters))
            throw new IllegalStateException("Unable to construct Redis message! There are missing parameters!");

        parameters.forEach(entry -> {
            String key = entry.getKey();
            JsonPrimitive value = entry.getValue();

            switch (key) {
                case ValidParameters.STATUS -> this.status = PingResponseStatus.valueOf(value.getAsString());
                case ValidParameters.MESSAGE -> this.message = value.getAsString();
                case ValidParameters.COLOR -> this.color = ColorMapper.map(value.getAsString());
                case ValidParameters.INTERVAL_OPTIONAL -> this.pingInterval = Optional.of(value.getAsInt());
            }
        });
    }

    @Override
    public JsonObject toJSON() {
        JsonObject object = super.toJSON();
        JsonObject parameters = new JsonObject();

        parameters.add(ValidParameters.STATUS, new JsonPrimitive(this.status.toString()));
        parameters.add(ValidParameters.MESSAGE, new JsonPrimitive(this.message));
        parameters.add(ValidParameters.COLOR, new JsonPrimitive(this.color.toString()));
        if(this.pingInterval.isPresent())
            parameters.add(ValidParameters.INTERVAL_OPTIONAL, new JsonPrimitive(this.pingInterval.get()));

        object.add(MasterValidParameters.PARAMETERS, parameters);

        return object;
    }

    public enum PingResponseStatus {
        ACCEPTED,
        DENIED,
        ERROR,
    }

    public interface ValidParameters {
        String STATUS = "s";
        String MESSAGE = "m";
        String COLOR = "c";
        String INTERVAL_OPTIONAL = "i";

        static List<String> toList() {
            List<String> list = new ArrayList<>();
            list.add(STATUS);
            list.add(MESSAGE);
            list.add(COLOR);

            return list;
        }
    }
}
