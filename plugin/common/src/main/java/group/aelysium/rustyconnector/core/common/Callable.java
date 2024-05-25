package group.aelysium.rustyconnector.core.common;

public interface Callable<K> {
    /**
     * Execute the callable
     */
    K execute();
}
