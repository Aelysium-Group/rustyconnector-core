package group.aelysium.rustyconnector.toolkit.velocity.config;

import group.aelysium.rustyconnector.toolkit.common.config.IConfigService;

import java.util.Optional;

public abstract class IProxyConfigService extends IConfigService {
    public abstract DefaultConfig root();
    public abstract FamiliesConfig families();
    public abstract FriendsConfig friends();
    public abstract DataTransitConfig dataTransit();
    public abstract DynamicTeleportConfig dynamicTeleport();
    public abstract LoggerConfig logger();
    public abstract PartyConfig party();

    /**
     * Gets the specified magic config.
     * @param name The name of the magic config. Shouldn't contain the file extension.
     * @return {@link Optional<MagicMCLoaderConfig>}
     */
    public abstract Optional<? extends MagicMCLoaderConfig> magicMCLoaderConfig(String name);

    /**
     * Gets the specified load balancer config.
     * @param name The name of the load balancer. Shouldn't contain the file extension.
     * @return {@link Optional<LoadBalancerConfig>}
     */
    public abstract Optional<? extends LoadBalancerConfig> loadBalancer(String name);

    /**
     * Gets the specified matchmaker config.
     * @param name The name of the matchmaker. Shouldn't contain the file extension.
     * @return {@link Optional<MatchMakerConfig>}
     */
    public abstract Optional<? extends MatchMakerConfig> matchmaker(String name);

    /**
     * Gets the specified whitelist config.
     * @param name The name of the matchmaker. Shouldn't contain the file extension.
     * @return {@link Optional<WhitelistConfig>}
     */
    public abstract Optional<? extends WhitelistConfig> whitelist(String name);

    /**
     * Gets the specified Scalar Family config.
     * @param id The id of the family. Shouldn't contain the file extension, nor `.scalar`. should only be the family's id.
     * @return {@link Optional<ScalarFamilyConfig>}
     */
    public abstract Optional<? extends ScalarFamilyConfig> scalarFamily(String id);

    /**
     * Gets the specified Static Family config.
     * @param id The id of the family. Shouldn't contain the file extension, nor `.static`. should only be the family's id.
     * @return {@link Optional<LoadBalancerConfig>}
     */
    public abstract Optional<? extends StaticFamilyConfig> staticFamily(String id);

    /**
     * Gets the specified Ranked Family config.
     * @param id The id of the family. Shouldn't contain the file extension, nor `.ranked`. should only be the family's id.
     * @return {@link Optional<RankedFamilyConfig>}
     */
    public abstract Optional<? extends RankedFamilyConfig> rankedFamily(String id);
}