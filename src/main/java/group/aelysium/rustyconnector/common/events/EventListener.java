package group.aelysium.rustyconnector.common.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {
    /**
     * The order by which listeners should run.
     * Listeners will run in order from highest to lowest.
     */
    EventPriority order() default EventPriority.NORMAL;

    /**
     * Should the listener still run even if the event is canceled.
     */
    boolean ignoreCanceled() default false;
}
