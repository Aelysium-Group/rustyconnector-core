package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.FailCapture;
import group.aelysium.rustyconnector.common.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.server.Environment;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.server.events.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    private String address;
    private WebSocketClient client = null;
    private final FailCapture failCapture = new FailCapture(5, LiquidTimestamp.from(15, TimeUnit.SECONDS));

    protected WebSocketMagicLink(
            @NotNull String address,
            @NotNull Packet.Target self,
            @NotNull AES cryptor,
            @NotNull MessageCache cache,
            @NotNull String registrationConfiguration,
            @Nullable IPV6Broadcaster broadcaster
    ) {
        super(self, cryptor, cache, registrationConfiguration, broadcaster);
        if(address.endsWith("/")) this.address = address.substring(0, address.length() - 1);
        else this.address = address;

        if(this.broadcaster == null) {
            this.connect();
            return;
        }

        this.broadcaster.onMessage(message -> {
            if(this.client != null) if(!this.client.isClosed()) return;

            try {
                String broadcastedAddress = this.cryptor.decrypt(message);
                if (broadcastedAddress.endsWith("/"))
                    this.address = broadcastedAddress.substring(0, broadcastedAddress.length() - 1);
                else this.address = broadcastedAddress;

                this.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Attempts to establish a connection to the MagicLink instance running on the proxy.
     */
    public void connect() throws ExceptionInInitializerError {
        if(this.failCapture.willFail()) return;

        HttpClient client = HttpClient.newHttpClient();

        String endpoint;
        String[] bearer = new String[3];
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.address+"/bDaBMkmYdZ6r4iFExwW6UzJyNMDseWoS3HDa6FcyM7xNeCmtK98S3Mhp4o7g7oW6VB9CA6GuyH2pNhpQk3QvSmBUeCoUDZ6FXUsFCuVQC59CB2y22SBnGkMf9NMB9UWk"))
                    .header("Authorization", "Bearer "+cryptor.encrypt(String.valueOf(Instant.now().getEpochSecond())))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            Gson gson = new Gson();
            JsonObject object = gson.fromJson(response.body(), JsonObject.class);
            endpoint    = cryptor.decrypt(object.get("endpoint").getAsString());
            bearer[0]   = cryptor.decrypt(object.get("token").getAsString());
            bearer[1]   = object.get("signature").getAsString();
            bearer[2]   = this.self.uuid().toString(); // Including the uuid in the bearer as well as "X-Server-Identification" is intentional.
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer "+Base64.getEncoder().encodeToString(cryptor.encrypt(String.join("-", bearer).getBytes(StandardCharsets.UTF_8))),
                    "X-Server-Identification", this.self.toJSON().toString()
            );
            this.client = new WebSocketClient(new URI(address.replace("http","ws").replace("https", "wss")+"/"+endpoint), headers) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    RC.S.Adapter().log(RC.S.Lang().lang().magicLink());
                    WebSocketMagicLink.this.heartbeat();
                }

                @Override
                public void onMessage(String message) {
                    WebSocketMagicLink.this.handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Websocket dropped, attempting to reconnect.");
                    try {
                        WebSocketMagicLink.this.executor.shutdownNow();
                        WebSocketMagicLink.this.failCapture.trigger("[" + code + "] Websocket connection closed with the following reason: " + reason);
                        this.connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            };
            this.client.connect();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void publish(Packet.Local packet) {
        try {
            this.client.send(this.cryptor.encrypt(packet.toString()));
            this.awaitReply(packet);
        } catch (Exception ignore) {}
    }

    private void heartbeat() {
        this.executor.schedule(() -> {
            if(stopPinging.get()) return;

            ServerFlame flame = RustyConnector.Toolkit.Server().orElseThrow().orElseThrow();

            try {
                Packet.Builder.PrepareForSending packet = Packet.New()
                        .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_PING)
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.SERVER_REGISTRATION, this.registration())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName()+":"+flame.address().getPort())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(flame.playerCount()));

                if(Environment.podName().isPresent())
                    packet.parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.POD_NAME, Environment.podName().orElseThrow());

                packet.addressedTo(Packet.Target.allAvailableProxies()).send();
            } catch (Exception e) {
                e.printStackTrace();
            }

            WebSocketMagicLink.this.heartbeat();
        }, this.delay.get(), TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        stopPinging.set(true);
        try {
            this.executor.shutdownNow();
        } catch (Exception ignore) {}

        try {
            Packet.New()
                    .identification(Packet.BuiltInIdentifications.MAGICLINK_HANDSHAKE_DISCONNECT)
                    .addressedTo(Packet.Target.allAvailableProxies())
                    .send();

            RC.S.EventManager().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}
    }

    public static class Tinder extends Particle.Tinder<WebSocketMagicLink> {
        private final String httpAddress;
        private final Packet.Target self;
        private final AES cryptor;
        private final MessageCache cache;
        private final String serverRegistration;
        private final IPV6Broadcaster broadcaster;
        public Tinder(
                @NotNull String httpAddress,
                @NotNull Packet.Target self,
                @NotNull AES cryptor,
                @NotNull MessageCache cache,
                @NotNull String serverRegistration,
                @Nullable IPV6Broadcaster broadcaster
                ) {
            this.httpAddress = httpAddress;
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.serverRegistration = serverRegistration;
            this.broadcaster = broadcaster;
        }

        @Override
        public @NotNull WebSocketMagicLink ignite() throws Exception {
            return new WebSocketMagicLink(
                    this.httpAddress,
                    this.self,
                    this.cryptor,
                    this.cache,
                    this.serverRegistration,
                    this.broadcaster
            );
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID serverUUID) {
            return new Tinder(
                    "http://127.0.0.1:8080",
                    Packet.Target.server(serverUUID),
                    AES.DEFAULT_CRYPTOR,
                    new MessageCache(50),
                    "default",
                    null
            );
        }
    }
}
