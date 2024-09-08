package group.aelysium.rustyconnector.common;

/**
 * Enforces a closure on the inheriting entity.
 */
public interface Closure {
    /**
     * Closes the inheriting entity releasing all of its resources.
     * @throws Exception If there's an issue during the closure.
     */
    void close() throws Exception;
}
