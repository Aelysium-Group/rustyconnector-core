package group.aelysium.rustyconnector.common.events;

public enum EventPriority {
    LOWEST(-2),
    LOW(-1),
    NORMAL(0),
    HIGH(1),
    HIGHEST(2),
    MONITOR(3);

    private final int slot;

    private EventPriority(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
}
