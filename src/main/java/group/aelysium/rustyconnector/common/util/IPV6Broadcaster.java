package group.aelysium.rustyconnector.common.util;

import group.aelysium.ara.Closure;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.errors.Error;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class IPV6Broadcaster implements Closure {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Vector<Consumer<String>> listeners = new Vector<>();
    private final AES cryptor;
    private final InetSocketAddress address;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public IPV6Broadcaster(@NotNull AES cryptor, @NotNull InetSocketAddress address) throws IOException {
        this.cryptor = cryptor;
        this.address = address;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", this.address.getPort()), 2000);
        } catch (Exception ignore) {
            throw new IOException("Port "+this.address.getPort()+" is currently unavailable! It might be closed or actively blocked by your network firewall.");
        }

        this.startListening();
    }

    private void handleMessages() {
        if(this.closed.get()) return;
        try(DatagramSocket socket = new DatagramSocket(this.address)) {
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String response = this.cryptor.decrypt(new String(packet.getData(), StandardCharsets.UTF_8));

            this.listeners.stream().toList().forEach(l -> {
                try {
                    l.accept(response);
                } catch (Exception ignore) {}
            });
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        this.executor.schedule(this::handleMessages, 1, TimeUnit.MINUTES);
    }

    /**
     * Takes the provided string, AES 256-bit encrypts it, Base64 encodes it, then sends it.
     * @param message The message to send.
     */
    public void sendEncrypted(String message) {
        try(DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            String encryptedData = this.cryptor.encrypt(message);
            DatagramPacket packet = new DatagramPacket(encryptedData.getBytes(StandardCharsets.UTF_8), encryptedData.length(), this.address);
            socket.send(packet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onMessage(Consumer<String> consumer) {
        this.listeners.add(consumer);
    }

    public void stopListening() {
        this.close();
    }
    public void startListening() {
        if(this.closed.get()) return;
        this.closed.set(false);
        this.executor.submit(this::handleMessages);
    }

    public void close() {
        this.closed.set(true);
        this.executor.shutdownNow();
    }
}