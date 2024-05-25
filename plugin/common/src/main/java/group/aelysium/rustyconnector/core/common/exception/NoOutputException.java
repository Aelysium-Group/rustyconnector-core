package group.aelysium.rustyconnector.core.common.exception;

/**
 * An exception that should never result in output being sent to the client.
 */
public class NoOutputException
        extends RuntimeException {
    public NoOutputException(Throwable err) {
        super("", err);
    }
    public NoOutputException() {
        super("", new Exception());
    }
}