package group.aelysium.rustyconnector.common.events;

public abstract class Event {
    private boolean canceled = false;

    public void canceled(boolean canceled) {
        this.canceled = canceled;
    }
    public boolean canceled() {
        return this.canceled;
    }
}
