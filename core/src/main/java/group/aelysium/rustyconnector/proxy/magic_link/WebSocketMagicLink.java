package group.aelysium.rustyconnector.proxy.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.crypt.SHA256;
import group.aelysium.rustyconnector.common.crypt.Token;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.websocket.WsContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected final Map<Packet.Target, WsContext> clients =new ConcurrentHashMap<>();
    protected final InetSocketAddress address;
    private final Javalin server = Javalin.create();

    protected WebSocketMagicLink(
            @NotNull InetSocketAddress address,
            @NotNull Packet.Target self,
            @NotNull AES cryptor,
            @NotNull MessageCache cache,
            @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations,
            @Nullable IPV6Broadcaster broadcaster
    ) {
        super(self, cryptor, cache, registrationConfigurations, broadcaster);

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

                String randomData = tokenGenerator.nextString();
                request.json(Map.of(
                    "endpoint", cryptor.encrypt(endpoint),
                    "token", cryptor.encrypt(now.getEpochSecond()+"-"+randomData),
                    "signature", SHA256.hash(now.getEpochSecond()+"-"+randomData)
                ));
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
            throw new UnauthorizedResponse();
        });
        server.wsBeforeUpgrade("/"+endpoint, (request)->{
            try {
                String authorization = request.header("Authorization");
                UUID serverIdentification = UUID.fromString(Optional.ofNullable(request.header("X-Server-Identification")).orElse(""));
                if (authorization == null) throw new UnauthorizedResponse();
                authorization = authorization.replaceAll("Bearer ", "");
                authorization = new String(cryptor.decrypt(Base64.getDecoder().decode(authorization.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

                String[] split = authorization.split("-");
                String token = split[0];
                String signature = split[1];
                UUID uuid = UUID.fromString(split[2]);

                if(!serverIdentification.equals(uuid)) throw new UnauthorizedResponse("Invalid identification.");

                if(!SHA256.hash(token).equals(signature)) throw new UnauthorizedResponse("Invalid token.");

                int unix = Integer.parseInt(cryptor.decrypt(token.split("-")[0]));
                Instant time = Instant.ofEpochMilli(unix);
                if(time.plus(30, ChronoUnit.SECONDS).isBefore(Instant.now()))
                    throw new UnauthorizedResponse();
            } catch (Exception ignore) {
                ignore.printStackTrace();
                throw new UnauthorizedResponse();
            }
        });
        server.ws("/"+endpoint, (config) -> {
            Gson gson = new Gson();
            config.onConnect(request -> {
                try {
                    Context upgradeRequest = request.getUpgradeCtx$javalin();
                    Packet.Target target = Packet.Target.fromJSON(gson.fromJson(Optional.ofNullable(upgradeRequest.header("X-Server-Identification")).orElse(""), JsonObject.class));

                    this.clients.putIfAbsent(target, request);
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                    request.closeSession(500, "Unable to complete Magic Link connection.");
                }
            });
            config.onClose(request -> {
                try {
                    Context upgradeRequest = request.getUpgradeCtx$javalin();
                    Packet.Target target = Packet.Target.fromJSON(gson.fromJson(Optional.ofNullable(upgradeRequest.header("X-Server-Identification")).orElse(""), JsonObject.class));

                    this.clients.remove(target);
                } catch (Exception ignore) {ignore.printStackTrace();}
            });
            config.onMessage(request -> {
                try {
                    Context upgradeRequest = request.getUpgradeCtx$javalin();
                    Packet.Target target = Packet.Target.fromJSON(gson.fromJson(Optional.ofNullable(upgradeRequest.header("X-Server-Identification")).orElse(""), JsonObject.class));

                    if(!this.clients.containsKey(target)) {
                        request.closeSession(401, "Unauthorized");
                        return;
                    }
                    this.handleMessage(request.message());
                } catch (Exception ignore) {ignore.printStackTrace();}
            });
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
            try {
                if(this.broadcaster == null) return;
                this.broadcaster.sendEncrypted(AddressUtil.addressToString(this.address));
            } catch (Exception ignore) {}

            this.heartbeat();
        }, 3, TimeUnit.SECONDS);
    }

    @Override
    public void publish(Packet.Local packet) {
        try {
            String encrypted = this.cryptor.encrypt(packet.toString());
            Packet.Target target = packet.target();
            if(target.isEquivalent(Packet.Target.allAvailableProxies()) || target.isEquivalent(Packet.Target.allAvailableServers())) {
                this.clients.forEach((k, v) -> {
                    if(!k.isEquivalent(target)) return;
                    v.send(encrypted);
                });
            } else {
                this.clients.get(target).send(encrypted);
            }

            this.awaitReply(packet);
        } catch (Exception ignore) {}
    }

    @Override
    public void close() {
        super.close();
        this.server.stop();
        this.clients.clear();
        this.cache.close();
        this.executor.shutdownNow();
    }

    public static class Tinder extends Particle.Tinder<WebSocketMagicLink> {
        private final Packet.Target self;
        private final AES cryptor;
        private final MessageCache cache;
        private final IPV6Broadcaster broadcaster;
        private final InetSocketAddress address;
        private final Map<String, Proxy.ServerRegistrationConfiguration> registrationConfigurations;
        public Tinder(
                @NotNull InetSocketAddress address,
                @NotNull Packet.Target self,
                @NotNull AES cryptor,
                @NotNull MessageCache cache,
                @NotNull Map<String, ServerRegistrationConfiguration> registrationConfigurations,
                @Nullable IPV6Broadcaster broadcaster
                ) {
            this.address = address;
            this.self = self;
            this.cryptor = cryptor;
            this.cache = cache;
            this.registrationConfigurations = registrationConfigurations;
            this.broadcaster = broadcaster;
        }

        @Override
        public @NotNull WebSocketMagicLink ignite() throws Exception {
            return new WebSocketMagicLink(
                    this.address,
                    this.self,
                    this.cryptor,
                    this.cache,
                    this.registrationConfigurations,
                    this.broadcaster
            );
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID proxyUUID) {
            return new Tinder(
                    AddressUtil.parseAddress("127.0.0.1:500"),
                    Packet.Target.proxy(proxyUUID),
                    AES.DEFAULT_CRYPTOR,
                    new MessageCache(50),
                    new HashMap<>(),
                    null
            );
        }
    }
}