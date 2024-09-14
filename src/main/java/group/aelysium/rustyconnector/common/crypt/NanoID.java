package group.aelysium.rustyconnector.common.crypt;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

import java.text.ParseException;
import java.util.Objects;
import java.util.Random;

public class NanoID {
    private static final Random random = new Random();
    private static final int LENGTH = 16;
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final String value;
    protected NanoID(String value) {
        this.value = value;
    }

    /**
     * This method returns the raw ID value.
     * @return The unmasked ID.
     */
    @Override
    public String toString() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NanoID id = (NanoID) o;
        return Objects.equals(value, id.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value);
    }

    /**
     * Creates a new NanoID using the passed string as the value.
     * @param value The value of the MaskedID.
     */
    public static NanoID fromString(String value) {
        String regex = "[" + ALPHABET + "]+";
        if(value.length() > LENGTH) throw new IllegalArgumentException("Couldn't parse value as NanoID! Value was to long!");
        if(!value.matches(regex)) throw new IllegalArgumentException("Couldn't parse value as NanoID! Value contained invalid characters.");
        return new NanoID(value);
    }

    public static NanoID randomNanoID() {
        return new NanoID(NanoIdUtils.randomNanoId(random, ALPHABET.toCharArray(), LENGTH));
    }
}