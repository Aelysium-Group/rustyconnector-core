package group.aelysium.rustyconnector.core.lib.database.redis;

import group.aelysium.rustyconnector.core.lib.database.redis.messages.RedisMessage;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

public class RedisPublisher {
    private final RedisClient client;
    protected RedisPublisher(RedisClient client) {
        this.client = client;
    }

    /**
     * Sends a message over a Redis data channel.
     * If a message is not already, this method will sign messages with the private key provided via the RedisClient used to init this RedisPublisher.
     * @param message The message to send.
     * @throws IllegalStateException If you attempt to send a received RedisMessage.
     */
    public void publish(RedisMessage message) {
        if(!message.isSendable()) throw new IllegalStateException("Attempted to send a RedisMessage that isn't sendable!");

        try {
            message.signMessage(client.getPrivateKey());
        } catch (IllegalStateException ignore) {} // If there's an issue it's bc the message is already signed. Thus ready to send.

        try (StatefulRedisPubSubConnection<String, String> connection = this.client.connectPubSub()) {
            RedisPubSubAsyncCommands<String, String> async = connection.async();

            async.publish(this.client.getDataChannel(), message.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
