package group.aelysium.rustyconnector.common.events;

public interface Event {
    interface Handler<E extends Event> {
        void handle(E event);
    }
}
