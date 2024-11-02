package group.aelysium.rustyconnector.common.errors;

import group.aelysium.rustyconnector.RC;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Error {
    private final UUID uuid = UUID.randomUUID();
    private final Instant createdAt = Instant.now();
    private final Throwable throwable;
    private final String message;
    private final String hint;
    private final String solution;
    private final List<KeyValuePair> details = new ArrayList<>();
    private boolean urgent = false;

    protected Error(@NotNull String message, @Nullable String hint, @Nullable String solution) {
        this.throwable = null;
        this.message = message;
        this.hint = hint;
        this.solution = solution;
    }
    protected Error(@NotNull Throwable throwable, @Nullable String hint, @Nullable String solution) {
        this.throwable = throwable;
        this.message = throwable.getMessage() == null ? "No message was provided by the causing exception." : throwable.getMessage();
        this.causedBy(throwable.getClass().getName());
        this.hint = hint;
        this.solution = solution;
    }

    /**
     * @return The uuid of the error.
     *         This can be used in the future to reference this error.
     */
    public @NotNull UUID uuid() {
        return this.uuid;
    }

    /**
     * @return The instant that this error was created.
     */
    public @NotNull Instant createdAt() {
        return this.createdAt;
    }

    /**
     * @return The throwable that caused this error.
     *         May be null if this error was not triggered by a throwable.
     */
    public @Nullable Throwable throwable() {
        return this.throwable;
    }

    /**
     * @return The main message of this error.
     */
    public @NotNull String message() {
        return this.message;
    }

    /**
     * @return A tentative hint of what might be causing the issue and how to fix it.
     */
    public @Nullable String hint() {
        return this.hint;
    }

    /**
     * @return A definite solution of what needs ot be done to solve the problem.
     */
    public @Nullable String solution() {
        return this.solution;
    }

    /**
     * @return All the details of this error.
     */
    public @NotNull List<KeyValuePair> details() {
        return Collections.unmodifiableList(this.details);
    }

    /**
     * Creates adds a detail entry to this error.
     * @param key The name of the detail.
     * @param value The detail itself.
     * @return An instance of this error.
     */
    public Error detail(@NotNull String key, @NotNull Object value) {
        this.details.removeAll(this.details.stream().filter(e->e.key.equals(key)).toList());

        this.details.add(new KeyValuePair(key, value));
        return this;
    }

    /**
     * Adds a detail entry which specifies when a provided value is incorrect.
     * @param expectedValue The value that was expected.
     * @param providedValue The value that was actually provided.
     * @return An instance of this error.
     */
    public Error wrongValue(@NotNull Object expectedValue, @NotNull Object providedValue) {
        this.detail("Expected Value", expectedValue);
        this.detail("Provided Value", providedValue);
        return this;
    }

    /**
     * Adds a details entry which specifies what caused this error.
     * @param causedBy A description of what caused this error.
     * @return An instance of this error.
     */
    public Error causedBy(@NotNull String causedBy) {
        this.detail("Caused By", causedBy);
        return this;
    }

    /**
     * Adds a details entry which specifies what the perpitrator of this error is.
     * In other words, this should describe what logic was being attempted, which then caused this error to be generated.
     * @param whileAttempting What logic caused this error to be created.
     * @return An instance of this error.
     */
    public Error whileAttempting(@NotNull String whileAttempting) {
        this.detail("While Attempting", whileAttempting);
        return this;
    }

    /**
     * Adds a details entry which provides the user a simple example of usage.
     * @param exampleUssage An example of how the specific logic should be used.
     * @return An instance of this error.
     */
    public Error exampleUsage(@NotNull String exampleUssage) {
        this.detail("Example Usage", exampleUssage);
        return this;
    }

    /**
     * Marks this Error as urgent.
     * If an Error is urgent it will bypass the {@link ErrorRegistry#logErrors()} and log the error.
     */
    public Error urgent(boolean urgent) {
        this.urgent = urgent;
        return this;
    }

    /**
     * @return Whether this Error is urgent and must be logged immediately.
     */
    public boolean urgent() {
        return this.urgent;
    }

    /**
     * @return The Error in the form of a component.
     *         More specifically, this method parses the error using Lang code `rustyconnector-error`.
     */
    public Component toComponent() {
        return RC.Lang("rustyconnector-error").generate(this);
    }

    public static Error from(@NotNull String message) {
        return new Error(message, null, null);
    }
    public static Error withHint(@NotNull String message, @NotNull String hint) {
        return new Error(message, hint, null);
    }
    public static Error withSolution(@NotNull String message, @NotNull String solution) {
        return new Error(message, null, solution);
    }

    public static Error from(@NotNull Throwable throwable) {
        return new Error(throwable, null, null);
    }
    public static Error withHint(@NotNull Throwable throwable, @NotNull String hint) {
        return new Error(throwable, hint, null);
    }
    public static Error withSolution(@NotNull Throwable throwable, @NotNull String solution) {
        return new Error(throwable, null, solution);
    }
}
