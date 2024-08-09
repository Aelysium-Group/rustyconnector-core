package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.common.cache.MessageCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketParameter;
import group.aelysium.rustyconnector.server.ServerFlame;
import group.aelysium.rustyconnector.server.events.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeFailureListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeStalePingListener;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeSuccessListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    private final URL proxyEndpoint;
    private WebSocketClient client = null;

    protected WebSocketMagicLink(
            @NotNull AESCryptor cryptor,
            @NotNull MessageCache cache,
            @NotNull Packet.Target self,
            @NotNull String registrationConfiguration,
            @NotNull URL proxyEndpoint
    ) {
        super(cryptor, cache, self, registrationConfiguration);
        this.proxyEndpoint = proxyEndpoint;

        this.heartbeat();
    }

    public void connect() {
        try {
            URL url = new URL("https://api.example.com/endpoint");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", this.cryptor.encrypt(AUTH_));
            connection.setDoOutput(true);

            JsonObject object = new JsonObject();
            object.add("");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Print the response
            System.out.println("Response: " + response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", cryptor.encrypt(AUTH_TOKEN));

        this.client = new WebSocketClient(new URI("ws://localhost:4567/ws"), headers) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("Connected to server");
            }

            @Override
            public void onMessage(String message) {
                WebSocketMagicLink.this.handleMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Disconnected from server");
            }

            @Override
            public void onError(Exception ex) {}
        };
    }

    public String magicConfig() {
        return this.magicConfig;
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
        private final URL proxyEndpoint;
        private final String magicConfig;
        private final List<PacketListener<? extends Packet>> listeners = new Vector<>();
        public Tinder(
                @NotNull AESCryptor cryptor,
                @NotNull Packet.Target self,
                @NotNull MessageCache cache,
                @NotNull URL proxyEndpoint,
                @NotNull String magicConfig
        ) {
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.proxyEndpoint = proxyEndpoint;
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
                    this.proxyEndpoint,
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
                    "default"
            );

            tinder.on(new HandshakeFailureListener());
            tinder.on(new HandshakeStalePingListener());
            tinder.on(new HandshakeSuccessListener());

            return tinder;
        }
    }
}
