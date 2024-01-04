package group.aelysium.rustyconnector.plugin.velocity.event_handlers.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.EventDispatch;
import group.aelysium.rustyconnector.plugin.velocity.lib.dynamic_teleport.injectors.InjectorService;
import group.aelysium.rustyconnector.toolkit.core.logger.PluginLogger;
import group.aelysium.rustyconnector.core.lib.exception.NoOutputException;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.lib.friends.FriendsService;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.ProxyLang;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.Player;
import group.aelysium.rustyconnector.toolkit.velocity.events.player.FamilyPostJoinEvent;
import group.aelysium.rustyconnector.toolkit.velocity.family.InitiallyConnectableFamily;
import group.aelysium.rustyconnector.toolkit.velocity.friends.IFriendRequest;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import group.aelysium.rustyconnector.toolkit.velocity.whitelist.IWhitelist;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

public class OnPlayerChooseInitialServer {
    /**
     * Runs when a player first joins the proxy
     */
    @Subscribe(order = PostOrder.LAST)
    public EventTask onPlayerChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Tinder api = Tinder.get();
        PluginLogger logger = api.logger();
        Player player = Player.from(event.getPlayer());
        com.velocitypowered.api.proxy.Player eventPlayer = event.getPlayer();

        return EventTask.async(() -> {
            try {
                // Check for network whitelist
                try {
                    IWhitelist whitelist = api.services().whitelist().proxyWhitelist();
                    if (!whitelist.validate(player)) {
                        logger.log("Player isn't whitelisted on the proxy whitelist! Kicking...");
                        eventPlayer.disconnect(Component.text(whitelist.message()));
                        return;
                    }
                } catch (Exception ignore) {}

                // Handle family injectors if they exist
                InitiallyConnectableFamily family = null;
                try {
                    InjectorService injectors = api.services().dynamicTeleport().orElseThrow().services().injector().orElseThrow();
                    String host = eventPlayer.getVirtualHost().map(InetSocketAddress::getHostString).orElse("").toLowerCase(Locale.ROOT);

                    family = injectors.familyOf(host).orElseThrow();
                } catch (NoSuchElementException ignore) {
                } catch (Exception e) {
                    logger.send(Component.text("Error while using Family Injectors! Players will attempt to connect to the root family because of this! "+e.getMessage(), NamedTextColor.RED));

                    family = api.services().family().rootFamily();
                }
                if(family == null) throw new RuntimeException("Unable to fetch a server to connect to.");

                IMCLoader server = family.connect(event);

                EventDispatch.Safe.fireAndForget(new FamilyPostJoinEvent(server.family(), server, player));
            } catch (Exception e) {
                eventPlayer.disconnect(Component.text("Disconnected. "+e.getMessage()));
                e.printStackTrace();
            }

            // Check for active friend requests
            try {
                FriendsService friendsService = api.services().friends().orElseThrow();
                List<IFriendRequest> requests = friendsService.findRequestsToTarget(player);

                if(requests.size() == 0) throw new NoOutputException();

                eventPlayer.sendMessage(ProxyLang.FRIENDS_JOIN_MESSAGE.build(requests));
            } catch (Exception ignore) {}

            // Check for online friends
            try {
                FriendsService friendsService = api.services().friends().orElseThrow();
                List<IPlayer> friends = friendsService.findFriends(player).orElseThrow();

                if(friends.size() == 0) throw new NoOutputException();

                List<com.velocitypowered.api.proxy.Player> onlineFriends = new ArrayList<>();
                friends.forEach(friend -> {
                    try {
                        com.velocitypowered.api.proxy.Player onlineFriend = friend.resolve().orElseThrow();

                        if (onlineFriend.isActive()) onlineFriends.add(onlineFriend);
                    } catch (Exception ignore) {}
                });

                if(friends.size() == 0 || onlineFriends.size() == 0) {
                    eventPlayer.sendMessage(ProxyLang.NO_ONLINE_FRIENDS);
                    throw new NoOutputException();
                }

                eventPlayer.sendMessage(ProxyLang.ONLINE_FRIENDS);
                final Component[] friendsList = {Component.text("", NamedTextColor.WHITE)};
                onlineFriends.forEach(friend -> friendsList[0] = friendsList[0].append(Component.text(friend.getUsername())));
                eventPlayer.sendMessage(Component.join(JoinConfiguration.commas(true), friendsList));

                onlineFriends.forEach(friend -> friend.sendMessage(ProxyLang.FRIEND_JOIN.build(player)));
            } catch (Exception ignore) {}
        });
    }
}