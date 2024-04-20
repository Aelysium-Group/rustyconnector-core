package group.aelysium.rustyconnector.toolkit.core.events;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;

public abstract class EventManager extends Particle {
    /**
     * Registers a new listener to this manager.
     */
    public abstract void on(Class<? extends Event> event, Listener<?> listener);

    /**
     * Unregisters a listener from this manager.
     */
    public abstract void off(Class<? extends Event> event, Listener<?> listener);
}
