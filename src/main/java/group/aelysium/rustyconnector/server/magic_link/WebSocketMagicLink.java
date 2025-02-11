package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.magic_link.PacketCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.util.URL;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.events.ConnectedEvent;
import group.aelysium.rustyconnector.server.events.DisconnectedEvent;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.server.magic_link.handlers.HandshakeStalePingListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.java_websocket.client.WebSocketClient;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    private final LiquidTimestamp retryDelay = LiquidTimestamp.from(20, TimeUnit.SECONDS);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean stopHeartbeat = new AtomicBoolean(true);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicReference<WebSocketClient> client = new AtomicReference<>(null);
    private URL address;

    protected WebSocketMagicLink(
            @NotNull URL address,
            @NotNull Packet.SourceIdentifier self,
            @NotNull AES cryptor,
            @NotNull PacketCache cache,
            @Nullable IPV6Broadcaster broadcaster
    ) {
        super(self, cryptor, cache, broadcaster);
        this.address = address;
        this.address = this.address.appendPath("bDaBMkmYdZ6r4iFExwW6UzJyNMDseWoS3HDa6FcyM7xNeCmtK98S3Mhp4o7g7oW6VB9CA6GuyH2pNhpQk3QvSmBUeCoUDZ6FXUsFCuVQC59CB2y22SBnGkMf9NMB9UWk");
        /*
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
                RC.Error(Error.from(e));
            }
        });*/

        this.listen(new HandshakeStalePingListener());
    }

    /**
     * Attempts to establish a connection to the MagicLink instance running on the proxy.
     */
    public void connect() throws ExceptionInInitializerError {
        if(this.closed.get()) return;
        if(this.client.get() != null) if(this.client.get().isOpen()) return;
        try {

            String websocketEndpoint;
            String[] bearer = new String[3];
            {
                HttpClient client = HttpClient.newHttpClient();
                URI uri = this.address.toURI();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Authorization", "Bearer " + aes.encrypt(((Long) Instant.now().getEpochSecond()).toString()))
                        .timeout(Duration.of(30, ChronoUnit.SECONDS))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if(response.statusCode() == 401) throw new RuntimeException("The your aes.private is invalid. Make sure the server has the same aes.private as the proxy!");
                if(response.statusCode() != 200) throw new RuntimeException("Magic Link response: " + response.body());

                Gson gson = new Gson();
                JsonObject object = gson.fromJson(response.body(), JsonObject.class);
                websocketEndpoint = aes.decrypt(object.get("endpoint").getAsString());
                bearer[0] = aes.decrypt(object.get("token").getAsString());
                bearer[1] = object.get("signature").getAsString();
                bearer[2] = this.self.id(); // Including the id in the bearer as well as "X-Server-Identification" is intentional.
            }

            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer " + aes.encrypt(String.join("$", bearer)),
                    "X-Server-Identification", this.self.toJSON().toString()
            );

            URL websocketURL = this.address.copy();
            if(websocketURL.protocol().equals(URL.Protocol.HTTP)) websocketURL = websocketURL.changeProtocol(URL.Protocol.WS);
            if(websocketURL.protocol().equals(URL.Protocol.HTTPS)) websocketURL = websocketURL.changeProtocol(URL.Protocol.WSS);
            websocketURL = websocketURL.clearPath().appendPath(websocketEndpoint);

            WebSocketClient client = new WebSocketClient(websocketURL.toURI(), new Draft_6455(), headers, 1000 * (60 * 15)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    WebSocketMagicLink.this.stopHeartbeat.set(false);
                    WebSocketMagicLink.this.heartbeat();
                    RC.Adapter().log(RC.Lang("rustyconnector-magicLinkHandshake").generate());
                }

                @Override
                public void onMessage(String message) {
                    WebSocketMagicLink.this.handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if(closed.get()) return;

                    try {
                        if (code == 1008)
                            RC.Adapter().log(Component.text("Unable to authenticate with Magic Link to the proxy. Trying again after " + retryDelay.value() + " " + retryDelay.unit() + "."));
                        else
                            RC.Adapter().log(Component.text("Unable to refresh connection with Magic Link on the proxy. Trying again after " + retryDelay.value() + " " + retryDelay.unit() + "."));
                    } catch (Exception ignore) {}

                    if(WebSocketMagicLink.this.client.get() != null) {
                        try {
                            WebSocketMagicLink.this.client.get().closeBlocking();
                        } catch (Exception ignore) {}
                        WebSocketMagicLink.this.client.set(null);
                    }
                    registered.set(false);
                    stopHeartbeat.set(true);
                    executor.schedule(() -> {
                        try {
                            WebSocketMagicLink.this.connect();
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, retryDelay.value(), retryDelay.unit());
                }

                @Override
                public void onError(Exception e) {
                    RC.Error(Error.from(e));
                }
            };
            client.setConnectionLostTimeout(0);
            if(client.connectBlocking()) {
                this.client.set(client);
                return;
            }
            this.client.set(null);
        } catch (Exception e) {
            RC.Error(Error.from(e).urgent(true));
        }
        RC.Adapter().log(Component.text("Unable to establish Magic Link connection to the proxy. Trying again after "+this.retryDelay.value()+" "+this.retryDelay.unit()+"."));
        this.executor.schedule(this::connect, this.retryDelay.value(), this.retryDelay.unit());
    }

    @Override
    public void publish(Packet.Local packet) {
        try {
            if(this.client.get() == null) return;
            this.cache.cache(packet);
            this.client.get().send(this.aes.encrypt(packet.toString()));
            packet.status(true, "Message successfully delivered.");
        } catch (Exception e) {
            packet.status(false, e.getMessage());
            RC.Error(Error.from(e));
        }
    }

    /**
     * Whether the server is successfully registered to the Proxy.
     * @return `true` if the server is registered. `false` otherwise.
     */
    public boolean registered() {
        return this.registered.get();
    }

    private void heartbeat() {
        if(this.closed.get()) return;
        if(this.stopHeartbeat.get()) return;

        ServerKernel kernel = RC.S.Kernel();

        try {
            JsonObject metadata = new JsonObject();
            kernel.parameterizedMetadata().forEach((k, v)->metadata.add(k, v.toJSON()));

            Packet.Builder.PrepareForSending packetBuilder = Packet.New()
                .identification(Packet.Type.from("RC","P"))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.TARGET_FAMILY, kernel.targetFamily())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.ADDRESS, kernel.address().getHostName()+":"+kernel.address().getPort())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.METADATA, new Packet.Parameter(metadata))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.PLAYER_COUNT, new Packet.Parameter(kernel.playerCount())
            );

            Packet.Local packet = packetBuilder.addressTo(Packet.SourceIdentifier.allAvailableProxies()).send();

            packet.onReply(Packets.Response.class, p -> {
                if (p.successful()) {
                    boolean canceled = RC.S.EventManager().fireEvent(new ConnectedEvent()).get(1, TimeUnit.MINUTES);
                    if (canceled) return PacketListener.Response.canceled();

                    if (!WebSocketMagicLink.this.registered.get()) {
                        int interval = p.parameters().get("i").getAsInt();
                        RC.S.Adapter().log(Component.text(p.message(), NamedTextColor.GREEN));
                        RC.S.Adapter().log(Component.text("This server will now ping the proxy every " + interval + " seconds...", NamedTextColor.GRAY));
                        RC.S.MagicLink().setDelay(interval);
                        WebSocketMagicLink.this.registered.set(true);
                    }
                    return PacketListener.Response.success("Successfully informed the server of its registration.");
                }

                RC.S.Adapter().log(Component.text(p.message(), NamedTextColor.RED));
                RC.S.Adapter().log(Component.text("Waiting 1 minute before trying again...", NamedTextColor.GRAY));
                RC.S.MagicLink().setDelay(60);
                WebSocketMagicLink.this.registered.set(false);
                return PacketListener.Response.success("Successfully informed the server of its failure to register.");
            });
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        this.executor.schedule(this::heartbeat, this.delay.get(), TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        closed.set(true);

        try {
            this.executor.shutdownNow();
            this.client.set(null);
        } catch (Exception ignore) {}

        try {
            this.client.get().close();
        } catch (Exception ignore) {}

        try {
            Packet.New()
                    .identification(Packet.Type.from("RC","D"))
                    .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                    .send();
        } catch (Exception ignore) {}
        try {
            RC.S.EventManager().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}
    }

    @Override
    public @Nullable Component details() {
        return join(
                newlines(),
                RC.Lang("rustyconnector-keyValue").generate("Target Address", this.address),
                RC.Lang("rustyconnector-keyValue").generate("Packet Cache Size", this.cache.size()),
                RC.Lang("rustyconnector-keyValue").generate("Packets Pending Responses", this.packetsAwaitingReply.size()),
                RC.Lang("rustyconnector-keyValue").generate("Packets Pending Responses", this.packetsAwaitingReply.expiration()),
                RC.Lang("rustyconnector-keyValue").generate("Ping Delay", this.retryDelay),
                RC.Lang("rustyconnector-keyValue").generate("Is Registered", this.registered.get()),
                RC.Lang("rustyconnector-keyValue").generate("Total Listeners Per Packet",
                        text(String.join(", ", this.listeners.entrySet().stream().map(e -> e.getKey() + " ("+e.getValue().size()+")").toList()))
                )
        );
    }

    public static class Tinder extends MagicLinkCore.Tinder<WebSocketMagicLink> {
        private final URL httpAddress;
        private final Packet.SourceIdentifier self;
        private final AES cryptor;
        private final PacketCache cache;
        private final IPV6Broadcaster broadcaster;
        public Tinder(
                @NotNull URL httpAddress,
                @NotNull Packet.SourceIdentifier self,
                @NotNull AES cryptor,
                @NotNull PacketCache cache,
                @Nullable IPV6Broadcaster broadcaster
                ) {
            super();
            this.httpAddress = httpAddress;
            this.cryptor = cryptor;
            this.self = self;
            this.cache = cache;
            this.broadcaster = broadcaster;
        }

        @Override
        public @NotNull WebSocketMagicLink ignite() throws Exception {
            return new WebSocketMagicLink(
                    this.httpAddress,
                    this.self,
                    this.cryptor,
                    this.cache,
                    this.broadcaster
            );
        }
    }
}
