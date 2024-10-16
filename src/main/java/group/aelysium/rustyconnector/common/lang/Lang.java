package group.aelysium.rustyconnector.common.lang;

import com.google.errorprone.annotations.Modifier;
import com.google.errorprone.annotations.RequiredModifiers;
import org.jetbrains.annotations.CheckReturnValue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the specific element as a Lang entry.
 * This entry will be targeted by various parts of the codebase.
 * Whatever you annotate with this annotation must be static.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@RequiredModifiers(modifier = {Modifier.STATIC})
public @interface Lang {
    /**
     * The identification for the lang node.
     * To prevent potential overwriting issues consider writing your node in the format of:
     * <code><pre>
     *     softwareName-entryName
     * </pre></code>
     * If you are a translator, be sure to read up on the documentation of the software you're translating for a list of their lang nodes.
     * @return The lang node.
     */
    String value();

    /**
     * Should this node require that the provided identification already exists.
     * If enabled, this node will be ignored if the provided identification doesn't already exist.
     * This setting is useful for lang translators who only want to overwrite existing entries
     * and don't want to add new entries to the lang.
     * @return Whether the node should require an already existing identification be present.
     */
    boolean required() default false;

    /**
     * Should this node operate in strict mode.
     * If enabled, this node will be ignored if its identification is already being used.
     * @return Whether the node should use strict mode or not.
     */
    boolean strict() default true;
}
