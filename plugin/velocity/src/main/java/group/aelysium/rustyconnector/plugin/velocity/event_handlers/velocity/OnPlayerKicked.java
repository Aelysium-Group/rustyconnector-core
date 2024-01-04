package group.aelysium.rustyconnector.plugin.velocity.event_handlers.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import group.aelysium.rustyconnector.core.lib.exception.NoOutputException;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.scalar_family.RootFamily;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.Player;
import group.aelysium.rustyconnector.plugin.velocity.lib.server.MCLoader;
import group.aelysium.rustyconnector.plugin.velocity.lib.webhook.WebhookAlertFlag;
import group.aelysium.rustyconnector.plugin.velocity.lib.webhook.WebhookEventManager;
import group.aelysium.rustyconnector.plugin.velocity.lib.webhook.DiscordWebhookMessage;
import group.aelysium.rustyconnector.toolkit.velocity.family.scalar_family.IRootFamily;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import net.kyori.adventure.text.Component;

public class OnPlayerKicked {
    /**
     * Runs when a player disconnects from a player server
     */
    @Subscribe(order = PostOrder.FIRST)
    public EventTask onPlayerKicked(KickedFromServerEvent event) {
        Tinder api = Tinder.get();
        Player player = Player.from(event.getPlayer());

        return EventTask.async(() -> {
            boolean isFromRootFamily = false;

            try {
                MCLoader oldServer = (MCLoader) player.server().orElseThrow();

                oldServer.playerLeft();

                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_LEAVE, oldServer.family().id(), DiscordWebhookMessage.PROXY__PLAYER_LEAVE_FAMILY.build(player, oldServer));
                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_LEAVE_FAMILY, oldServer.family().id(), DiscordWebhookMessage.FAMILY__PLAYER_LEAVE.build(player, oldServer));

                isFromRootFamily = oldServer.family() == api.services().family().rootFamily();
            } catch (Exception ignore) {}

            try {
                if (!api.services().family().shouldCatchDisconnectingPlayers()) throw new NoOutputException();

                IRootFamily rootFamily = api.services().family().rootFamily();
                if(rootFamily.registeredServers().isEmpty()) throw new RuntimeException("There are no available servers for you to connect to!");
                if(isFromRootFamily) throw new NoOutputException();

                IMCLoader newServer = rootFamily.fetchAny(player);
                if(newServer == null) throw new RuntimeException("Server closed.");

                try {
                    event.setResult(KickedFromServerEvent.RedirectPlayer.create(newServer.registeredServer(), event.getServerKickReason().get()));
                } catch (Exception ignore) {
                    event.setResult(KickedFromServerEvent.RedirectPlayer.create(newServer.registeredServer()));
                }

                newServer.playerJoined();

                WebhookEventManager.fire(WebhookAlertFlag.DISCONNECT_CATCH, api.services().family().rootFamily().id(), DiscordWebhookMessage.PROXY__DISCONNECT_CATCH.build(player, newServer));
                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_JOIN, api.services().family().rootFamily().id(), DiscordWebhookMessage.PROXY__PLAYER_JOIN_FAMILY.build(player, newServer));
                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_JOIN_FAMILY, api.services().family().rootFamily().id(), DiscordWebhookMessage.FAMILY__PLAYER_JOIN.build(player, newServer));

                return;
            }
            catch(NoOutputException ignore) {}
            catch (Exception e) {
                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_LEAVE, DiscordWebhookMessage.PROXY__PLAYER_LEAVE.build(player));
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("Kicked by server. "+e.getMessage())));
                e.printStackTrace();
            }

            try {
                if(event.getServerKickReason().isPresent())
                    event.setResult(KickedFromServerEvent.DisconnectPlayer.create(event.getServerKickReason().get()));
                else
                    event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("Kicked by server.")));

                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_LEAVE, DiscordWebhookMessage.PROXY__PLAYER_LEAVE.build(player));
            } catch (Exception e) {
                WebhookEventManager.fire(WebhookAlertFlag.PLAYER_LEAVE, DiscordWebhookMessage.PROXY__PLAYER_LEAVE.build(player));
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(Component.text("Kicked by server. "+e.getMessage())));
                e.printStackTrace();
            }
        });
    }
}