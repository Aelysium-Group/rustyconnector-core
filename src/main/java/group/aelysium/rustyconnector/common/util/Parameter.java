package group.aelysium.rustyconnector.common.util;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Parameter {
    protected char type;
    protected Object object;
    
    private Parameter(@NotNull Object object, char type) {
        this.object = object;
        this.type = type;
    }
    public Parameter(@NotNull Number object) {
        this(object, 'n');
    }
    public Parameter(@NotNull Boolean object) {
        this(object, 'b');
    }
    public Parameter(@NotNull String object) {
        this(object, 's');
    }
    public Parameter(@NotNull JsonArray object) {
        this(object, 'a');
    }
    public Parameter(@NotNull JsonObject object) {
        this(object, 'j');
    }
    public static Parameter fromJSON(@NotNull JsonElement object) {
        if(object.isJsonPrimitive()) {
            JsonPrimitive primitive = object.getAsJsonPrimitive();
            if(primitive.isNumber()) {
                try {
                    return new Parameter(primitive.getAsInt());
                } catch (Exception ignore) {}
                try {
                    return new Parameter(primitive.getAsLong());
                } catch (Exception ignore) {}
                try {
                    return new Parameter(primitive.getAsDouble());
                } catch (Exception ignore) {}
                try {
                    return new Parameter(primitive.getAsShort());
                } catch (Exception ignore) {}
                try {
                    return new Parameter(primitive.getAsFloat());
                } catch (Exception ignore) {}
            }
            if(primitive.isBoolean()) return new Parameter(primitive.getAsBoolean());
            if(primitive.isString()) return new Parameter(primitive.getAsString());
        }
        if(object.isJsonArray()) return new Parameter(object.getAsJsonArray());
        if(object.isJsonObject()) return new Parameter(object.getAsJsonObject());
        throw new IllegalStateException("Unexpected value: " + object.getClass().getName());
    }
    public static Parameter fromUnknown(@NotNull Object object) {
        if(object.getClass().isPrimitive()) {
            if(Number.class.isAssignableFrom(object.getClass()))
                return new Parameter((Number) object);
            if(int.class.isAssignableFrom(object.getClass()))
                return new Parameter((int) object);
            if(long.class.isAssignableFrom(object.getClass()))
                return new Parameter((long) object);
            if(double.class.isAssignableFrom(object.getClass()))
                return new Parameter((double) object);
            if(float.class.isAssignableFrom(object.getClass()))
                return new Parameter((float) object);
            if(short.class.isAssignableFrom(object.getClass()))
                return new Parameter((short) object);
            if(Boolean.class.isAssignableFrom(object.getClass()) || boolean.class.isAssignableFrom(object.getClass()))
                return new Parameter((boolean) object);
            if(String.class.isAssignableFrom(object.getClass()))
                return new Parameter(String.valueOf(object));
        }
        if(JsonElement.class.isAssignableFrom(object.getClass())) return fromJSON((JsonElement) object);
        throw new IllegalStateException("Unexpected value: " + object.getClass().getName());
    }
    
    public char type() {
        return this.type;
    }
    
    public int getAsInt() {
        return ((Number) this.object).intValue();
    }
    public long getAsLong() {
        return ((Number) this.object).longValue();
    }
    public double getAsDouble() {
        return ((Number) this.object).doubleValue();
    }
    public float getAsFloat() {
        return ((Number) this.object).floatValue();
    }
    public short getAsShort() {
        return ((Number) this.object).shortValue();
    }
    public boolean getAsBoolean() {
        return (boolean) this.object;
    }
    public String getAsString() {
        return (String) this.object;
    }
    public UUID getStringAsUUID() {
        return UUID.fromString(this.getAsString());
    }
    public JsonArray getAsJsonArray() {
        return (JsonArray) this.object;
    }
    public JsonObject getAsJsonObject() {
        return (JsonObject) this.object;
    }
    
    public Object getOriginalValue() {
        return this.object;
    }
    
    public JsonElement toJSON() {
        return switch (type) {
            case 'n' -> new JsonPrimitive((Number) this.object);
            case 'b' -> new JsonPrimitive((Boolean) this.object);
            case 's' -> new JsonPrimitive((String) this.object);
            case 'a' -> (JsonArray) this.object;
            case 'j' -> (JsonObject) this.object;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}