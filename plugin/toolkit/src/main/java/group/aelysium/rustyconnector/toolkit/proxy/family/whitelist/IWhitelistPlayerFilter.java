package group.aelysium.rustyconnector.toolkit.proxy.family.whitelist;

import java.util.UUID;

public interface IWhitelistPlayerFilter {
    UUID uuid();
    String username();
    String ip();
}