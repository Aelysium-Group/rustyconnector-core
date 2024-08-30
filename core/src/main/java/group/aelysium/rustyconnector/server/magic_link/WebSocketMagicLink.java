package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.FailCapture;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.server.events.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.server.lang.ServerLang;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeFailureListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeStalePingListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeSuccessListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    private final InetSocketAddress address;
    private WebSocketClient client = null;
    private final FailCapture failCapture = new FailCapture(5, LiquidTimestamp.from(15, TimeUnit.SECONDS));

    protected WebSocketMagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull InetSocketAddress address,
            @NotNull String registrationConfiguration
    ) {
        super(cryptor, cache, self, registrationConfiguration);
        this.address = address;

        this.heartbeat();
    }

    /**
     * Attempts to establish a connection to the MagicLink instance running on the proxy.
     */
    public void connect() throws ExceptionInInitializerError {
        if(this.failCapture.willFail()) return;

        String apiAddress = AddressUtil.addressToString(this.address);
        HttpClient client = HttpClient.newHttpClient();

        String endpoint;
        String token;
        String signature;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://"+apiAddress+"/bDaBMkmYdZ6r4iFExwW6UzJyNMDseWoS3HDa6FcyM7xNeCmtK98S3Mhp4o7g7oW6VB9CA6GuyH2pNhpQk3QvSmBUeCoUDZ6FXUsFCuVQC59CB2y22SBnGkMf9NMB9UWk"))
                    .header("Authorization", "Bearer "+cryptor.encrypt(String.valueOf(Instant.now().getEpochSecond())))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            Gson gson = new Gson();
            JsonObject object = gson.fromJson(response.body(), JsonObject.class);
            endpoint    = cryptor.decrypt(object.get("endpoint").getAsString());
            token       = cryptor.decrypt(object.get("token").getAsString());
            signature   = object.get("signature").getAsString();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer "+token+"-"+signature
            );
            this.client = new WebSocketClient(new URI("ws://"+apiAddress+"/"+endpoint), headers) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    RC.S.Adapter().log(RC.S.Lang().lang().magicLink());
                }

                @Override
                public void onMessage(String message) {
                    WebSocketMagicLink.this.handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Websocket dropped, attempting to reconnect.");
                    try {
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

    /**
     * The name of the magic config that should be used on the proxy with this MCLoader.
     */
    public String magicConfig() {
        return this.registrationConfiguration;
    }

    public void setDelay(int delay) {
        this.delay.set(delay);
    }

    @Override
    public void publish(Packet packet) {
        try {
            this.client.send(this.cryptor.encrypt(packet.toString()));
            super.publish(packet);
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
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.MAGIC_CONFIG_NAME, this.magicConfig())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName()+":"+flame.address().getPort())
                        .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new PacketParameter(flame.playerCount()));

                if(podName != null)
                    packet.parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.POD_NAME, this.podName);

                packet.addressedTo(Packet.Target.allAvailableProxies()).send();
            } catch (Exception e) {
                e.printStackTrace();
            }

            WebSocketMagicLink.this.heartbeat();
        }, this.delay.get(), TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
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
        private final AESCryptor cryptor;
        private final Packet.Target self;
        private final MessageCache cache;
        private final InetSocketAddress address;
        private final String magicConfig;
        private final List<PacketListener<? extends Packet>> listeners = new Vector<>();
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull Packet.Target self,
                @NotNull MessageCache cache,
                @NotNull InetSocketAddress address,
                @NotNull String magicConfig
        ) {
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.address = address;
            this.magicConfig = magicConfig;

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
                    this.magicConfig
            );

            this.listeners.forEach(magicLink::on);

            return magicLink;
        }

        public static Tinder DEFAULT_CONFIGURATION(UUID serverUUID) {
            Tinder tinder = new Tinder(
                    AESCryptor.DEFAULT_CRYPTOR,
                    Packet.Target.server(serverUUID),
                    new MessageCache(50),
                    AddressUtil.parseAddress("localhost:500"),
                    "default"
            );

            tinder.on(new HandshakeFailureListener());
            tinder.on(new HandshakeStalePingListener());
            tinder.on(new HandshakeSuccessListener());

            return tinder;
        }
    }
}
