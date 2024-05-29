package group.aelysium.rustyconnector.toolkit.velocity.family.mcloader;

import com.sun.jdi.request.DuplicateRequestException;
import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.magic_link.packet.IPacket;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.family.load_balancing.ISortable;
import group.aelysium.rustyconnector.toolkit.velocity.connection.IPlayerConnectable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.util.Optional;
import java.util.UUID;

public interface IMCLoader extends ISortable, IPlayerConnectable {
    /**
     * Checks if the {@link IMCLoader} is stale.
     * @return {@link Boolean}
     */
    boolean stale();

    /**
     * Set's the {@link IMCLoader PlayerServer's} new timeout.
     * @param newTimeout The new timeout.
     */
    void setTimeout(int newTimeout);

    /**
     * The {@link UUID} of this {@link IMCLoader}.
     * This {@link UUID} will always be different between servers.
     * If this server unregisters and then re-registers into the proxy, this ID will be different.
     * @return {@link UUID}
     */
    UUID uuid();

    /**
     * Convenience method to return the MCLoader's display name if it exists.
     * If none exists, it will return the MCLoader's UUID in string format.
     */
    String uuidOrDisplayName();

    /**
     * Gets this {@link IMCLoader MCLoader's} pod name if it exists.
     * If your RC network isn't a part of a Kubernetes cluster, this will always return an empty optional.
     * @return {@link Optional<String>}
     */
    Optional<String> podName();

    /**
     * Decrease this {@link IMCLoader PlayerServer's} timeout by 1.
     * Once this value equals 0, this server will become stale and player's won't be able to join it anymore.
     * @param amount The amount to decrease by.
     * @return The new timeout value.
     */
    int decreaseTimeout(int amount);

    /**
     * This MCLoader's address.
     */
    InetSocketAddress address();

    /**
     * Gets the raw server that backs this MCLoader.
     */
    Object raw();

    /**
     * Is the server full? Will return `true` if and only if `soft-player-cap` has been reached or surpassed.
     * @return `true` if the server is full. `false` otherwise.
     */
    boolean full();

    /**
     * Is the server maxed out? Will return `true` if and only if `hard-player-cap` has been reached or surpassed.
     * @return `true` if the server is maxed out. `false` otherwise.
     */
    boolean maxed();

    /**
     * Lazily gets the player count for this server.
     * Depending on sync configurations and how often players connect and disconnect form this server.
     * This number can be off from the actual player count.
     * @return {@link Integer}
     */
    int playerCount();

    /**
     * Set the player count for this server.
     * This number will directly impact whether new players can join this server based on server soft and hard caps.
     * The number set here will be overwritten the next time this server syncs with the proxy.
     * @param playerCount The player count.
     */
    void setPlayerCount(int playerCount);

    /**
     * Gets the sort index of this server.
     * This method is used by the {@link ILoadBalancer} to sort this and other servers in a family.
     * @return {@link Integer}
     */
    double sortIndex();

    /**
     * Gets the weight of this server.
     * This method is used by the {@link ILoadBalancer} to sort this and other servers in a family.
     * @return {@link Integer}
     */
    int weight();

    /**
     * The soft player cap of this server.
     * If this value is reached by {@link IMCLoader#playerCount()}, {@link IMCLoader#full()} will evaluate to true.
     * The only way for new players to continue to join this server once it's full is by giving them the soft cap bypass permission.
     * @return {@link Integer}
     */
    int softPlayerCap();

    /**
     * The hard player cap of this server.
     * If this value is reached by {@link IMCLoader#playerCount()}, {@link IMCLoader#maxed()} will evaluate to true.
     * The only way for new players to continue to join this server once it's maxed is by giving them the hard cap bypass permission.
     *
     * If this value is reached by {@link IMCLoader#playerCount()}, it can be assumed that {@link IMCLoader#full()} is also true, because this value cannot be less than {@link IMCLoader#softPlayerCap()}.
     * @return {@link Integer}
     */
    int hardPlayerCap();

    /**
     * Get the family this server is associated with.
     * @return {@link Particle.Flux<IFamily>}
     */
    Particle.Flux<IFamily> family();

    /**
     * Locks the specific server in its respective family so that the load balancer won't return it for players to connect to.
     * If the server is already locked, or doesn't exist in the load balancer, nothing will happen.
     */
    void lock();

    /**
     * Unlocks the specific server in its respective family so that the load balancer can return it for players to connect to.
     * If the server is already unlocked, or doesn't exist in the load balancer, nothing will happen.
     */
    void unlock();
    record Unregistered(
            @NotNull UUID uuid,
            @NotNull InetSocketAddress address,
            @Nullable String podName,
            @Nullable String displayName,
            int softPlayerCap,
            int hardPlayerCap,
            int weight,
            int timeout
    ) {}
}
