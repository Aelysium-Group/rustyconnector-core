package group.aelysium.rustyconnector.proxy.magic_link;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.crypt.SHA256;
import group.aelysium.rustyconnector.common.crypt.Token;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.magic_link.packet_handlers.HandshakeDisconnectListener;
import group.aelysium.rustyconnector.proxy.magic_link.packet_handlers.HandshakePingListener;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.websocket.WsContext;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WebSocketMagicLink extends MagicLinkCore.Proxy {
    protected static final Token tokenGenerator = new Token(128);
    protected final String endpoint;
    protected final Map<UUID, WsContext> clients =new ConcurrentHashMap<>();
    protected final InetSocketAddress address;
    private final Javalin server = Javalin.create();

    protected WebSocketMagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull InetSocketAddress address,
            @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations
    ) {
        super(cryptor, cache, self, registrationConfigurations);

        this.endpoint = tokenGenerator.nextString() + "/" + tokenGenerator.nextString();

        this.heartbeat();
        this.address = address;

        server.get("/bDaBMkmYdZ6r4iFExwW6UzJyNMDseWoS3HDa6FcyM7xNeCmtK98S3Mhp4o7g7oW6VB9CA6GuyH2pNhpQk3QvSmBUeCoUDZ6FXUsFCuVQC59CB2y22SBnGkMf9NMB9UWk", (request) -> {
            String authorization = request.header("Authorization");
            if(authorization == null) throw new UnauthorizedResponse();
            authorization = authorization.replaceAll("Bearer ", "");

            try {
                long unix = Long.parseLong(cryptor.decrypt(new String(Base64.getDecoder().decode(authorization), StandardCharsets.UTF_8)));
                Instant time = Instant.ofEpochSecond(unix);
                Instant now = Instant.now();

                if(time.plus(30, ChronoUnit.SECONDS).isBefore(now))
                    throw new UnauthorizedResponse();

                String token = tokenGenerator.nextString();
                request.json(Map.of(
                    "endpoint", cryptor.encrypt(endpoint),
                    "token", cryptor.encrypt(now.getEpochSecond()+"-"+token),
                    "signature", SHA256.hash(now.getEpochSecond()+"-"+token)
                ));
            } catch (Exception ignore) {}
            throw new UnauthorizedResponse();
        });
        server.ws("/"+endpoint, (config)-> config.onMessage(m -> this.handleMessage(m.message())));
        server.wsBeforeUpgrade("/"+endpoint, (request)->{
            String authorization = request.header("Authorization");
            if(authorization == null) throw new UnauthorizedResponse();
            authorization = authorization.replaceAll("Bearer ", "");

            try {
                String[] split = authorization.split("-");
                String token = cryptor.decrypt(split[0]);
                String signature = split[1];

                if(!SHA256.hash(token).equals(signature)) throw new UnauthorizedResponse();

                int unix = Integer.parseInt(cryptor.decrypt(token.split("-")[0]));
                Instant time = Instant.ofEpochMilli(unix);
                if(time.plus(30, ChronoUnit.SECONDS).isBefore(Instant.now()))
                    throw new UnauthorizedResponse();
                return;
            } catch (Exception ignore) {}
            throw new UnauthorizedResponse();
        });
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
                                if (server.stale()) {
                                    family.deleteServer(server);
                                    WsContext connection = this.clients.get(server.uuid());
                                    connection.closeSession(1013, "Stale connection. Re-register.");
                                }
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
            Packet.Target target = packet.target();
            this.clients.get(target.uuid()).send(encrypted);
        } catch (Exception ignore) {}
    }

    @Override
    public void close() throws Exception {
        super.close();
        this.server.stop();
        this.clients.clear();
        this.cache.close();
        this.executor.shutdownNow();
    }

    public static class Tinder extends Particle.Tinder<WebSocketMagicLink> {
        private final AESCryptor cryptor;
        private final Packet.Target self;
        private final MessageCache cache;
        private final InetSocketAddress address;
        private final Map<String, Proxy.ServerRegistrationConfiguration> magicConfigs;
        private final List<PacketListener<? extends Packet>> listeners = new Vector<>();
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull Packet.Target self,
                @NotNull MessageCache cache,
                @NotNull InetSocketAddress address,
                @NotNull Map<String, Proxy.ServerRegistrationConfiguration> magicConfigs
                ) {
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.address = address;
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
                    this.address,
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
                    AddressUtil.parseAddress("127.0.0.1:500"),
                    new HashMap<>()
            );

            tinder.on(new HandshakePingListener());
            tinder.on(new HandshakeDisconnectListener());

            return tinder;
        }
    }
}