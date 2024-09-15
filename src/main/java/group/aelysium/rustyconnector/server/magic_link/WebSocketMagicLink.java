package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.magic_link.exceptions.CanceledPacket;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.magic_link.MessageCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.util.URL;
import group.aelysium.rustyconnector.server.Environment;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.events.ConnectedEvent;
import group.aelysium.rustyconnector.server.events.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeFailureListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeStalePingListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean stopHeartbeat = new AtomicBoolean(true);
    private WebSocketClient client = null;
    private URL address;

    protected WebSocketMagicLink(
            @NotNull URL address,
            @NotNull Packet.SourceIdentifier self,
            @NotNull AES cryptor,
            @NotNull MessageCache cache,
            @NotNull String registrationConfiguration,
            @Nullable IPV6Broadcaster broadcaster
    ) {
        super(self, cryptor, cache, registrationConfiguration, broadcaster);
        this.address = address;

        if(this.broadcaster == null) {
            this.executor.submit(this::connect);
            return;
        }

        this.broadcaster.onMessage(message -> {
            if(this.client != null) if(!this.client.isClosed()) return;

            try {
                String broadcastedAddress = this.aes.decrypt(message);
                if (broadcastedAddress.endsWith("/"))
                    this.address = URL.parseURL(broadcastedAddress.substring(0, broadcastedAddress.length() - 1));
                else this.address = URL.parseURL(broadcastedAddress);

                this.executor.submit(this::connect);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        this.listen(HandshakeFailureListener.class);
        this.listen(HandshakeStalePingListener.class);
    }

    /**
     * Attempts to establish a connection to the MagicLink instance running on the proxy.
     */
    public void connect() throws ExceptionInInitializerError {
        if(this.closed.get()) return;
        if(this.client != null) if(this.client.isOpen()) return;
        try {
            HttpClient client = HttpClient.newHttpClient();

            String websocketEndpoint;
            String[] bearer = new String[3];
            try {
                URI uri = this.address
                                .appendPath("bDaBMkmYdZ6r4iFExwW6UzJyNMDseWoS3HDa6FcyM7xNeCmtK98S3Mhp4o7g7oW6VB9CA6GuyH2pNhpQk3QvSmBUeCoUDZ6FXUsFCuVQC59CB2y22SBnGkMf9NMB9UWk")
                                .toURI();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", "Bearer " + aes.encrypt(((Long)Instant.now().getEpochSecond()).toString()))
                        .timeout(Duration.of(30, ChronoUnit.SECONDS))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if(response.statusCode() != 200) throw new RuntimeException("Magic Link response: " + response.body());

                Gson gson = new Gson();
                JsonObject object = gson.fromJson(response.body(), JsonObject.class);
                websocketEndpoint = aes.decrypt(object.get("endpoint").getAsString());
                bearer[0] = aes.decrypt(object.get("token").getAsString());
                bearer[1] = object.get("signature").getAsString();
                bearer[2] = this.self.uuid().toString(); // Including the uuid in the bearer as well as "X-Server-Identification" is intentional.
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExceptionInInitializerError(e);
            }

            try {
                Map<String, String> headers = Map.of(
                        "Authorization", "Bearer " + aes.encrypt(String.join("$", bearer)),
                        "X-Server-Identification", this.self.toJSON().toString()
                );

                URL websocketURL = this.address.copy();
                if(websocketURL.protocol().equals(URL.Protocol.HTTP)) websocketURL.changeProtocol(URL.Protocol.WS);
                if(websocketURL.protocol().equals(URL.Protocol.HTTPS)) websocketURL.changeProtocol(URL.Protocol.WSS);
                websocketURL.clearPath();
                websocketURL.appendPath(websocketEndpoint);

                this.client = new WebSocketClient(websocketURL.toURI(), new Draft_6455(), headers, 1000 * (60 * 15)) {
                    @Override
                    public void onOpen(ServerHandshake handshake) {
                        RC.S.Adapter().log(RC.S.Lang().lang().magicLink());
                        WebSocketMagicLink.this.stopHeartbeat.set(false);
                        WebSocketMagicLink.this.heartbeat();
                    }

                    @Override
                    public void onMessage(String message) {
                        WebSocketMagicLink.this.handleMessage(message);
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        if(code == 401) {
                            RC.S.Adapter().log(Component.text("Unable to authenticate with Magic Link to the proxy. Trying again after 10 seconds."));
                            WebSocketMagicLink.this.executor.schedule(this::connect, 10, TimeUnit.SECONDS);
                            return;
                        }

                        try {
                            if(this.reconnectBlocking()) return;  // breaks the reconnection logic bc it must be on another thread
                        } catch (InterruptedException ignore) {}
                        RC.S.Adapter().log(Component.text("["+code+"] "+reason));
                        WebSocketMagicLink.this.stopHeartbeat.set(true);
                        RC.S.Adapter().log(Component.text("Unable to refresh connection with Magic Link on the proxy. Trying again after 10 seconds."));
                        WebSocketMagicLink.this.executor.schedule(this::connect, 10, TimeUnit.SECONDS);
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                };
                this.client.setConnectionLostTimeout(0);
                if(this.client.connectBlocking()) return;
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        RC.S.Adapter().log(Component.text("Unable to establish Magic Link connection to the proxy. Trying again after 10 seconds."));
        this.executor.schedule(this::connect, 10, TimeUnit.SECONDS);
    }

    @Override
    public void publish(Packet.Local packet) {
        try {
            this.client.send(this.aes.encrypt(packet.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void heartbeat() {
        if(this.closed.get()) return;
        if(this.stopHeartbeat.get()) return;

        ServerKernel flame = RustyConnector.Toolkit.Server().orElseThrow().orElseThrow();

        try {
            Packet.Builder.PrepareForSending packetBuilder = Packet.New()
                    .identification(Packet.Identification.from("RC","MLH"))
                    .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.DISPLAY_NAME, flame.displayName())
                    .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.SERVER_REGISTRATION, this.registration())
                    .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.ADDRESS, flame.address().getHostName()+":"+flame.address().getPort())
                    .parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(flame.playerCount()));

            if(Environment.podName().isPresent())
                packetBuilder.parameter(MagicLinkCore.Packets.Handshake.Ping.Parameters.POD_NAME, Environment.podName().orElseThrow());
            Packet.Local packet = packetBuilder.addressTo(Packet.SourceIdentifier.allAvailableProxies()).send();

            packet.onReply(Packets.Handshake.Success.class, p -> {
                Packets.Handshake.Success response = new Packets.Handshake.Success(p);

                boolean canceled = RC.S.EventManager().fireEvent(new ConnectedEvent()).get(20, TimeUnit.SECONDS);
                if(canceled) throw new CanceledPacket();

                RC.S.Adapter().log(Component.text(response.message(), response.color()));
                RC.S.Adapter().log(Component.text("This server will now ping the proxy every "+response.pingInterval()+" seconds...", NamedTextColor.GRAY));
                RC.S.MagicLink().setDelay(response.pingInterval());
            });
            packet.onReply(Packets.Handshake.Failure.class, p -> {
                Packets.Handshake.Failure response = new Packets.Handshake.Failure(p);
                RC.S.Adapter().log(Component.text(response.reason(), NamedTextColor.RED));
                RC.S.Adapter().log(Component.text("Waiting 1 minute before trying again...", NamedTextColor.GRAY));
                RC.S.MagicLink().setDelay(60);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.executor.schedule(this::heartbeat, this.delay.get(), TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        closed.set(true);
        try {
            this.executor.shutdownNow();
        } catch (Exception ignore) {}

        try {
            Packet.New()
                    .identification(Packet.Identification.from("RC","MLHK"))
                    .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                    .send();

            RC.S.EventManager().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}
    }

    public static class Tinder extends Particle.Tinder<WebSocketMagicLink> {
        private final URL httpAddress;
        private final Packet.SourceIdentifier self;
        private final AES cryptor;
        private final MessageCache cache;
        private final String serverRegistration;
        private final IPV6Broadcaster broadcaster;
        public Tinder(
                @NotNull URL httpAddress,
                @NotNull Packet.SourceIdentifier self,
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
    }
}
