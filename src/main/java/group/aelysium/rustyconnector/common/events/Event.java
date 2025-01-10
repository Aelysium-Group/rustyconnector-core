package group.aelysium.rustyconnector.common.events;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Event {
    public static abstract class Cancelable extends Event {
        private final AtomicBoolean canceled = new AtomicBoolean(false);
        private final AtomicReference<String> canceledMessage = new AtomicReference<>("This action has been canceled.");

        /**
         * Sets whether the event is canceled.
         * @param canceled Whether the event should be canceled.
         */
        public void canceled(boolean canceled) {
            this.canceled.set(canceled);
        }

        /**
         * Sets whether the event is canceled.
         * @param canceled Whether the event should be canceled.
         * @param message The message which indicates why the event was canceled.
         *                Depending on the event, this message may be shown to players.
         *                In other cases this message will be ignored.
         */
        public void canceled(boolean canceled, String message) {
            this.canceled.set(canceled);
            this.canceledMessage.set(message);
        }
        public boolean canceled() {
            return this.canceled.get();
        }
        public String canceledMessage() {
            return this.canceledMessage.get();
        }
    }
}
