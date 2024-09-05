package group.aelysium.rustyconnector.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * Programmatically pulls data from the {@link Config} and stores it in the associated field.
 * The whatever that type of data is will be inferred from the field type.
 */
public @interface Node {
    /**
     * The order of this entry.
     * This value manages how the elements will appear in the config.
     */
    int order() default 0;

    /**
     * The key to read.
     */
    String key();

    /**
     * The default value if the entry doesn't exist.
     */
    String defaultValue();
}
