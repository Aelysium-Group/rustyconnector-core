package group.aelysium.rustyconnector.common.events;

public interface Listener<Event extends group.aelysium.rustyconnector.common.events.Event> {
    void handler(Event event);
}
