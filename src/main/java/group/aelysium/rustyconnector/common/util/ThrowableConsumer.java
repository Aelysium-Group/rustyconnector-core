package group.aelysium.rustyconnector.common.util;

/**
 * Bears heavy resemblance to a Consumer except that it can throw an exception.
 * @param <T> The parameter used by the consumer.
 * @param <E> The throwable the consumer may throw.
 */
public interface ThrowableConsumer<T, E extends Throwable> {
    void accept(T t) throws E;
}