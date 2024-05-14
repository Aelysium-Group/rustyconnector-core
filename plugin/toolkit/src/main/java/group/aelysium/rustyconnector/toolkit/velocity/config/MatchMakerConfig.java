package group.aelysium.rustyconnector.toolkit.velocity.config;

import group.aelysium.rustyconnector.toolkit.velocity.family.matchmaking.IMatchmaker;

public interface MatchMakerConfig {
    IMatchmaker.Settings settings();
}
