package group.aelysium.rustyconnector.core.common.messenger.implementors.redis;

import group.aelysium.rustyconnector.toolkit.core.UserPass;
import group.aelysium.rustyconnector.core.common.messenger.MessengerConnector;
import group.aelysium.rustyconnector.core.common.crypt.AESCryptor;
import group.aelysium.rustyconnector.toolkit.core.messenger.IMessengerConnection;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.net.InetSocketAddress;

public class RedisConnector extends MessengerConnector {
    private static final ClientResources resources = ClientResources.create();
    protected final String dataChannel;
    protected final ProtocolVersion protocolVersion;

    public RedisConnector(@NotNull AESCryptor cryptor, @NotNull Settings settings) {
        super(cryptor, settings.address(), settings.userPass());
        this.protocolVersion = ProtocolVersion.valueOf(settings.protocolVersion());
        this.dataChannel = settings.dataChannel();
    }

    @Override
    public IMessengerConnection connect() throws ConnectException {
        this.connection = new RedisConnection(
            this.toClientBuilder(),
            this.cryptor
        );

        return this.connection;
    }

    private RedisClient.Builder toClientBuilder() {
        return new RedisClient.Builder()
                .setHost(this.address.getHostName())
                .setPort(this.address.getPort())
                .setUser(this.userPass.user())
                .setPassword(this.userPass.password())
                .setDataChannel(this.dataChannel)
                .setResources(resources)
                .setProtocol(this.protocolVersion);
    }

    public record Settings(InetSocketAddress address, UserPass userPass, String protocolVersion, String dataChannel) { }


    @Override
    public void kill() {
        if(this.connection != null) this.connection.kill();
    }
}
