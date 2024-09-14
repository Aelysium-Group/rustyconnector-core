package group.aelysium.rustyconnector.common.util;

/**
 * Bears heavy resemblance to a Consumer except that it can throw an exception.
 * @param <T>
 */
public interface ThrowableConsumer<T> {
    void accept(T t) throws Throwable;
}