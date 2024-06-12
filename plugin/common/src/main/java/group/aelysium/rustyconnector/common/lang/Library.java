package group.aelysium.rustyconnector.common.lang;

import group.aelysium.rustyconnector.toolkit.common.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.common.config.IConfig;
import org.jetbrains.annotations.NotNull;

public class Library implements Particle {
    protected Library(@NotNull MCLoaderSettings settings) {

    }
    protected Library(@NotNull ProxySettings settings) {

    }

    @Override
    public void close() throws Exception {

    }

    public record MCLoaderSettings(
            @NotNull Lang lang,
            @NotNull IConfig git,
            @NotNull IConfig config
    ) {}
    public record ProxySettings(
            @NotNull Lang lang,
            @NotNull IConfig git,
            @NotNull IConfig config,
            @NotNull IConfig family,
            @NotNull IConfig loadBalancer,
            @NotNull IConfig whitelist,
            @NotNull IConfig magicConfig,
            @NotNull IConfig magicLink
    ) {}
}
