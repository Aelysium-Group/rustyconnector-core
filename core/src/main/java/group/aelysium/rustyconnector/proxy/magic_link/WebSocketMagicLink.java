package group.aelysium.rustyconnector.proxy.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.crypt.Token;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.magic_link.packet_handlers.HandshakeDisconnectListener;
import group.aelysium.rustyconnector.proxy.magic_link.packet_handlers.HandshakePingListener;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jetbrains.annotations.NotNull;
import spark.Service;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebSocketMagicLink extends MagicLinkCore.Proxy {
    protected static final Token tokenGenerator = new Token(128);
    protected final String endpoint = tokenGenerator.nextString();
    protected final String authenticationToken = tokenGenerator.nextString();
    protected Service websocketServer = Service.ignite();
    protected WebSocketHandler webSocketHandler = new WebSocketHandler();
    protected final Vector<InetSocketAddress> whitelist;

    protected WebSocketMagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations,
            @NotNull List<InetSocketAddress> whitelist
    ) {
        super(cryptor, cache, self, registrationConfigurations);
        this.whitelist = whitelist;

        this.heartbeat();

        websocketServer.get("/register", (request, response) -> {
            if(this.whitelist.contains(request.ip()))
        });
        websocketServer.webSocket("/"+ websocketEndpoint, this.webSocketHandler);
    }

    private void heartbeat() {
        this.executor.schedule(() -> {
            try {
                RC.P.Families().dump().forEach(f -> {
                    try {
                        Family family = f.orElseThrow();
                        family.servers().forEach(server -> {
                            server.decreaseTimeout(3);

                            try {
                                if (server.stale()) family.deleteServer(server);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception ignore) {}

            this.heartbeat();
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    public void publish(Packet packet) {
        try {
            String encrypted = this.cryptor.encrypt(packet.toString());
            this.webSocketHandler.publish(packet.target(), encrypted);
        } catch (Exception ignore) {}
    }

    @Override
    public void close() throws Exception {
        super.close();
        this.executor.shutdownNow();
    }

    public static class Tinder extends Particle.Tinder<WebSocketMagicLink> {
        private final AESCryptor cryptor;
        private final Packet.Target self;
        private final MessageCache cache;
        private final Map<String, Proxy.ServerRegistrationConfiguration> magicConfigs;
        private final List<PacketListener<? extends Packet>> listeners = new Vector<>();
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull Packet.Target self,
                @NotNull MessageCache cache,
                @NotNull Map<String, Proxy.ServerRegistrationConfiguration> magicConfigs
                ) {
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.magicConfigs = magicConfigs;
        }

        public Tinder on(PacketListener<? extends Packet> listener) {
            this.listeners.add(listener);
            return this;
        }

        @Override
        public @NotNull WebSocketMagicLink ignite() throws Exception {
            WebSocketMagicLink magicLink = new WebSocketMagicLink(
                    this.cryptor,
                    this.cache,
                    this.self,
                    this.magicConfigs
            );

            this.listeners.forEach(magicLink::on);

            return magicLink;
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID proxyUUID) {
            Tinder tinder = new Tinder(
                    AESCryptor.DEFAULT_CRYPTOR,
                    Packet.Target.proxy(proxyUUID),
                    new MessageCache(50),
                    new HashMap<>()
            );

            tinder.on(new HandshakePingListener());
            tinder.on(new HandshakeDisconnectListener());

            return tinder;
        }
    }

    @WebSocket
    protected class WebSocketHandler {
        protected final Map<Packet.Target, Session> connections = new ConcurrentHashMap<>();

        public void publish(Packet.Target target, String encryptedMessage) {
            try {
                this.connections.get(target).getRemote().sendString(encryptedMessage);
            } catch (Exception ignore) {}
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
            if(!this.authenticated(session)) {
                session.close(401, "Unauthorized");
                return;
            }
        }

        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            if(!this.authenticated(session)) {
                session.close(401, "Unauthorized");
                return;
            }

            WebSocketMagicLink.this.handleMessage(message);
        }
    }
}