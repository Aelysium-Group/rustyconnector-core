package group.aelysium.rustyconnector.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
    /**
     * The location of the config.
     * Points to the config's physical path on the machine.
     * If the config exists, it'll load the details.
     * If it doesn't exist, it'll be created.
     */
    String value();
}
