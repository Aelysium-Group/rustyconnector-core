package group.aelysium.rustyconnector.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The ascii alphabet class is used to generate large multi-line
 * representations of alphanumeric text via ASCII Characters.
 */
public interface ASCIIAlphabet {
    /**
     * @return A list of characters supported by this alphabet.
     */
    @NotNull List<Character> supportedCharacters();

    /**
     * Generate a component containing the ASCII representation of the provided string.
     */
    Component generate(String string);

    /**
     * Generates a component containing the ASCII representation of the provided string, also colorized.
     */
    Component generate(String string, NamedTextColor color);
}
