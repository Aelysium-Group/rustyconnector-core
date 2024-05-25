package group.aelysium.rustyconnector.toolkit.core.events;

import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;

public interface IEventManager extends Particle {
    /**
     * Registers a new listener to this manager.
     */
    void on(Class<? extends Event> event, Listener<?> listener);

    /**
     * Unregisters a listener from this manager.
     */
    void off(Class<? extends Event> event, Listener<?> listener);
}
