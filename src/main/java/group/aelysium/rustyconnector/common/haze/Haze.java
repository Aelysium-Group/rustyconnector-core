package group.aelysium.rustyconnector.common.haze;

import com.google.errorprone.annotations.Modifier;
import com.google.errorprone.annotations.RequiredModifiers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a specific method as a HazeQuery.
 * HazeQueries can be used to interface with an active database without the caller needing to know the details of said database.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@RequiredModifiers(modifier = {Modifier.STATIC})
public @interface Haze {
    /**
     * The key for the Haze node.
     */
    String value();
}
