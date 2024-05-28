package group.aelysium.rustyconnector.toolkit.mc_loader.ranked_game_interface;

import group.aelysium.rustyconnector.toolkit.common.matchmaking.IPlayerRank;

import java.util.UUID;

public record MCLoaderMatchPlayer(UUID uuid, String username, String schema, IPlayerRank rank) {}
