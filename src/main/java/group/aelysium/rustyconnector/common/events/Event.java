package group.aelysium.rustyconnector.common.events;

public abstract class Event {
    public static abstract class Cancelable extends Event {
        private boolean canceled = false;
        private String canceledMessage = "This action has been canceled.";

        /**
         * Sets whether the event is canceled.
         * @param canceled Whether the event should be canceled.
         */
        public void canceled(boolean canceled) {
            this.canceled = canceled;
        }

        /**
         * Sets whether the event is canceled.
         * @param canceled Whether the event should be canceled.
         * @param message The message which indicates why the event was canceled.
         *                Depending on the event, this message may be shown to players.
         *                In other cases this message will be ignored.
         */
        public void canceled(boolean canceled, String message) {
            this.canceled = canceled;
            this.canceledMessage = message;
        }
        public boolean canceled() {
            return this.canceled;
        }
        public String canceledMessage() {
            return this.canceledMessage;
        }
    }
}
