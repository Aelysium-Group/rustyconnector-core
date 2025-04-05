package group.aelysium.rustyconnector.common.magic_link.packet;

import group.aelysium.rustyconnector.common.util.Parameter;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketListener {
    Class<? extends Packet.Remote> value();

    /**
     * If enabled, exceptions thrown by the listener method will be turned into an error {@link Response}.
     */
    boolean responseFromExceptions() default true;
    /**
     * If enabled, overrides {@link Response#shouldSendPacket()} and will always send a packet when a response is made.<br/>
     * Note that if {@link #responseFromExceptions} is false, exceptions won't send replies to packets since no response was generated from said exception.
     */
    boolean responsesAsPacketReplies() default false;

    @FunctionalInterface
    interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    class Response {
        public final boolean successful;
        public final String message;
        public final Map<String, Parameter> parameters;
        protected boolean shouldSendPacket = false;

        protected Response (
                boolean successful,
                @NotNull String message,
                @NotNull Map<String, Parameter> parameters
        ) {
            this.successful = successful;
            this.message = message;
            this.parameters = parameters;
        }

        public boolean shouldSendPacket() {
            return this.shouldSendPacket;
        }

        public static Response success(@NotNull String message) {
            return success(message, Map.of());
        }
        public static Response success(@NotNull String message, @NotNull Map<String, Parameter> parameters) {
            return new Response(true, message, parameters);
        }

        public static Response error(@NotNull String message) {
            return error(message, Map.of());
        }
        public static Response error(@NotNull String message, @NotNull Map<String, Parameter> parameters) {
            return new Response(false, message, parameters);
        }

        public static Response canceled() {
            return error("The action performed by this packet has been canceled.");
        }

        /**
         * Marks this response as a packet reply.
         * This will cause the response to be sent back to the original sender once it's been returned to a PacketListener.
         */
        public Response asReply() {
            this.shouldSendPacket = true;
            return this;
        }
    }
}