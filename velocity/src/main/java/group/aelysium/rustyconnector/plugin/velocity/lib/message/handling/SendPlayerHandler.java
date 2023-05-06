package group.aelysium.rustyconnector.plugin.velocity.lib.message.handling;

import com.velocitypowered.api.proxy.Player;
import group.aelysium.rustyconnector.core.lib.database.redis.messages.MessageHandler;
import group.aelysium.rustyconnector.core.lib.database.redis.messages.RedisMessage;
import group.aelysium.rustyconnector.core.lib.database.redis.messages.variants.RedisMessageSendPlayer;
import group.aelysium.rustyconnector.plugin.velocity.VelocityRustyConnector;
import group.aelysium.rustyconnector.plugin.velocity.central.VelocityAPI;
import group.aelysium.rustyconnector.plugin.velocity.lib.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.BaseServerFamily;
import net.kyori.adventure.text.Component;

import java.security.InvalidAlgorithmParameterException;
import java.util.UUID;

public class SendPlayerHandler implements MessageHandler {
    private final RedisMessageSendPlayer message;

    public SendPlayerHandler(RedisMessage message) {
        this.message = (RedisMessageSendPlayer) message;
    }

    @Override
    public void execute() throws Exception {
        VelocityAPI api = VelocityRustyConnector.getAPI();

        Player player = api.getServer().getPlayer(message.getUUID()).stream().findFirst().orElse(null);
        if(player == null) return;

        try {
            BaseServerFamily family = api.getVirtualProcessor().getFamilyManager().find(message.getFamilyName());
            if (family == null) throw new InvalidAlgorithmParameterException("A family with the name `"+message.getFamilyName()+"` doesn't exist!");

            family.connect(player);
        } catch (Exception e) {
            player.disconnect(Component.text("There was an issue connecting you to that server!"));
            throw new Exception(e.getMessage());
        }
    }
}
