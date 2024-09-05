package group.aelysium.rustyconnector.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * Allows you to programmatically add YAML comments to your {@link Config}.
 */
public @interface Comment {
    /**
     * The order of this entry.
     * This value manages how the elements will appear in the config.
     */
    int order() default 0;

    /**
     * The comment to show.
     * Each new array element will be put on a newline.
     * If a entry is provided without a leading # character, one will be added.
     */
    String[] value();
}
