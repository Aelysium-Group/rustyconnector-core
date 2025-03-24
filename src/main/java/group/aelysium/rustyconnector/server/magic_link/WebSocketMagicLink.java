package group.aelysium.rustyconnector.server.magic_link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.packet.PacketListener;
import group.aelysium.rustyconnector.common.util.IPV6Broadcaster;
import group.aelysium.rustyconnector.common.magic_link.PacketCache;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.common.util.URL;
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
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.newlines;

public class WebSocketMagicLink extends MagicLinkCore.Server {
    protected final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    protected final ScheduledExecutorService connectionExecutor = Executors.newSingleThreadScheduledExecutor();
    private final LiquidTimestamp retryDelay = LiquidTimestamp.from(10, TimeUnit.SECONDS);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean connectionClosed = new AtomicBoolean(false);
    private final AtomicBoolean stopHeartbeat = new AtomicBoolean(true);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicReference<WebSocketClient> client = new AtomicReference<>(null);
    private final URL address;
    
    public WebSocketMagicLink(
            @NotNull URL address,
            @NotNull Packet.SourceIdentifier self,
            @NotNull AES aes,
            @NotNull PacketCache cache,
            @Nullable IPV6Broadcaster broadcaster
    ) {
        super(self, aes, cache, broadcaster);
        this.address = address.appendPath(MagicLinkCore.endpoint);
        
        this.listen(new HandshakeStalePingListener());
        
        this.connect();
    }

    /**
     * Attempts to establish a connection to the MagicLink instance running on the proxy.
     */
    public void connect() throws ExceptionInInitializerError {
        if(this.disposed.get()) return;
        if(this.client.get() != null && this.client.get().isOpen()) throw new ExceptionInInitializerError("Unable to connect because there's already a websocket connection active.");
        
        try {
            String websocketEndpoint;
            String[] bearer = new String[3];
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(this.address.toURI())
                    .header("Authorization", "Bearer " + aes.encrypt(((Long) Instant.now().getEpochSecond()).toString()))
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
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
                bearer[2] = this.self.namespace(); // Including the id in the bearer as well as "X-Server-Identification" is intentional.
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
                    WebSocketMagicLink.this.connectionClosed.set(false);
                    WebSocketMagicLink.this.heartbeat();
                }
                
                @Override
                public void onMessage(String message) {
                    WebSocketMagicLink.this.handleMessage(message);
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if(WebSocketMagicLink.this.connectionClosed.get()) return;
                    try {
                        if (code == 1008)
                            RC.Adapter().log(Component.text("Unable to authenticate with Magic Link to the proxy. Trying again after " + retryDelay.value() + " " + retryDelay.unit() + "."));
                        else
                            RC.Adapter().log(Component.text("Unable to refresh connection with Magic Link on the proxy. Trying again after " + retryDelay.value() + " " + retryDelay.unit() + "."));
                        
                    } catch (Exception ignore) {}
                    
                    WebSocketMagicLink.this.reset();
                }
                
                @Override
                public void onError(Exception e) {
                    RC.Error(Error.from(e));
                    if(e instanceof WebsocketNotConnectedException)
                        WebSocketMagicLink.this.closeConnection();
                }
            };
            client.setConnectionLostTimeout(0);
            
            if(client.connectBlocking()) {
                this.client.set(client);
                return;
            }
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        
        RC.Adapter().log(Component.text("Unable to refresh connection with Magic Link on the proxy. Trying again after " + retryDelay.value() + " " + retryDelay.unit() + "."));
        this.reset();
    }
    
    @Override
    public void publish(Packet.Local packet) {
        try {
            if (this.client.get() == null) return;
            this.cache.cache(packet);
            this.client.get().send(this.aes.encrypt(packet.toString()));
            packet.status(true, "Message successfully delivered.");
        } catch (WebsocketNotConnectedException ignore) {
            // Theoretically the websocket disconnect should be getting handled elsewhere
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
        if(this.disposed.get()) return;
        if(this.stopHeartbeat.get()) return;
        
        Packet.Builder.PrepareForSending packetBuilder = null;
        try {
            ServerKernel kernel = RC.S.Kernel();
            
            JsonObject metadata = new JsonObject();
            kernel.metadata().forEach((k, v) -> metadata.add(k, v.toJSON()));
            
            packetBuilder = Packet.New()
                .identification(Packet.Type.from("RC", "P"))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.TARGET_FAMILY, kernel.targetFamily())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.ADDRESS, kernel.address().getHostName() + ":" + kernel.address().getPort())
                .parameter(MagicLinkCore.Packets.Ping.Parameters.METADATA, new Parameter(metadata))
                .parameter(MagicLinkCore.Packets.Ping.Parameters.PLAYER_COUNT, new Parameter(kernel.playerCount())
                );
        } catch (WebsocketNotConnectedException ignore) {
            return; // Theoretically the websocket disconnect should be getting handled elsewhere
        } catch (Exception ignore) {
            if(this.disposed.get()) return;
            if(this.stopHeartbeat.get()) return;
            this.heartbeatExecutor.schedule(this::heartbeat, this.delay.get(), TimeUnit.SECONDS);
            return;
        }
        
        try {
            Packet.Local packet = packetBuilder.addressTo(Packet.SourceIdentifier.allAvailableProxies()).send();
            
            packet.onReply(Packets.Response.class, p -> {
                if (p.successful()) {
                    boolean canceled = RC.S.EventManager().fireEvent(new ConnectedEvent()).get(1, TimeUnit.MINUTES);
                    if (canceled) return PacketListener.Response.canceled();
                    
                    if (!WebSocketMagicLink.this.registered.get()) {
                        int interval = p.parameters().get("i").getAsInt();
                        RC.Adapter().log(RC.Lang("rustyconnector-magicLinkHandshake").generate());
                        WebSocketMagicLink.this.setDelay(interval);
                        WebSocketMagicLink.this.registered.set(true);
                    }
                    return PacketListener.Response.success("Successfully informed the server of its registration.");
                }
                
                RC.S.Adapter().log(Component.text(p.message(), NamedTextColor.RED));
                RC.S.Adapter().log(Component.text("Waiting 1 minute before trying again...", NamedTextColor.GRAY));
                WebSocketMagicLink.this.setDelay(60);
                WebSocketMagicLink.this.registered.set(false);
                return PacketListener.Response.success("Successfully informed the server of its failure to register.");
            });
        } catch (WebsocketNotConnectedException ignore) {
            return; // Theoretically the websocket disconnect should be getting handled elsewhere
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        
        if(this.disposed.get()) return;
        if(this.stopHeartbeat.get()) return;
        this.heartbeatExecutor.schedule(this::heartbeat, this.delay.get(), TimeUnit.SECONDS);
    }
    
    /**
     * Resets the MagicLink connection to start from the beginning.
     * This method will for MagicLink to re-authenticate.
     */
    public void reset() {
        if(disposed.get()) return;
        
        this.closeConnection();
        
        this.connectionExecutor.schedule(this::connect, this.retryDelay.value(), this.retryDelay.unit());
    }
    
    private void closeConnection() {
        this.connectionClosed.set(true);
        this.registered.set(false);
        this.stopHeartbeat.set(true);
        
        try {
            this.client.get().closeConnection(1000, "Normal closure.");
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To close the websocket connection used by MagicLink"));
        }
        this.client.set(null);
    }

    @Override
    public void close() {
        disposed.set(true);
        
        try {
            Packet.New()
                .identification(Packet.Type.from("RC","D"))
                .addressTo(Packet.SourceIdentifier.allAvailableProxies())
                .send();
        } catch (Exception ignore) {}
        
        this.closeConnection();
        
        try {
            RC.S.EventManager().fireEvent(new DisconnectedEvent());
        } catch (Exception ignore) {}
        
        try {
            this.connectionExecutor.shutdownNow();
        } catch (Exception ignore) {}
        try {
            this.heartbeatExecutor.shutdownNow();
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
}
