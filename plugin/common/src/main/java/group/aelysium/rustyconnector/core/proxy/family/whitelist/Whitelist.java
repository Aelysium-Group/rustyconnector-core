package group.aelysium.rustyconnector.core.proxy.family.whitelist;

import group.aelysium.rustyconnector.core.proxy.Permission;
import group.aelysium.rustyconnector.toolkit.core.absolute_redundancy.Particle;
import group.aelysium.rustyconnector.toolkit.velocity.player.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelist;
import group.aelysium.rustyconnector.core.common.Callable;
import group.aelysium.rustyconnector.toolkit.velocity.family.whitelist.IWhitelistPlayerFilter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Whitelist implements IWhitelist {
    private final String message;
    private final String name;
    private final String permission;
    private final List<WhitelistPlayerFilter> playerFilters = new ArrayList<>();

    private final boolean usePlayers;
    private final boolean usePermission;
    private final boolean strict;
    private final boolean inverted;

    public Whitelist(String name, boolean usePlayers, boolean usePermission, String message, boolean strict, boolean inverted) {
        this.name = name;
        this.usePlayers = usePlayers;
        this.usePermission = usePermission;
        this.message = message;
        this.strict = strict;
        this.inverted = inverted;
        this.permission = Permission.constructNode("rustyconnector.whitelist.<whitelist id>",this.name);
    }

    public boolean usesPlayers() {
        return usePlayers;
    }
    public boolean usesPermission() {
        return usePermission;
    }
    public String name() {
        return name;
    }
    public String message() {
        return message;
    }
    public boolean inverted() {
        return this.inverted;
    }

    public List<WhitelistPlayerFilter> playerFilters() {
        return this.playerFilters;
    }

    /**
     * Validate a player against the whitelist.
     * @param player The player to validate.
     * @return `true` if the player is whitelisted. `false` otherwise.
     */
    public boolean validate(IPlayer player) {
        Callable<Boolean> validate = () -> {
            if (Whitelist.this.strict)
                return validateStrict(player);
            else
                return validateSoft(player);
        };

        if(this.inverted)
            return !validate.execute();
        else
            return validate.execute();
    }

    private boolean validateStrict(IPlayer player) {
        boolean playersValid = true;
        boolean countryValid = true;
        boolean permissionValid = true;


        if (this.usesPlayers())
            if (!WhitelistPlayerFilter.validate(this, player))
                playersValid = false;

        if (this.usesPermission())
            if (!Permission.validate(player.resolve().orElse(null), this.permission))
                permissionValid = false;


        return (playersValid && countryValid && permissionValid);
    }

    private boolean validateSoft(IPlayer player) {
        if (this.usesPlayers())
            if (WhitelistPlayerFilter.validate(this, player))
                return true;

        if (this.usesPermission())
            return Permission.validate(player.resolve().orElse(null), this.permission);

        return false;
    }

    @Override
    public void close() throws Exception {
        this.playerFilters.clear();
    }

    public static class Tinder extends Particle.Tinder<IWhitelist> {
        private final Settings settings;

        public Tinder(Settings settings) {
            this.settings = settings;
        }

        @Override
        public @NotNull IWhitelist ignite() throws Exception {
            return new Whitelist(
                    this.settings.name(),
                    this.settings.usePlayers(),
                    this.settings.usePermission(),
                    this.settings.message(),
                    this.settings.strict(),
                    this.settings.inverted()
            );
        }
    }

    public static class WhitelistPlayerFilter implements IWhitelistPlayerFilter {
        private UUID uuid = null;
        private String username = null;
        private String ip = null;

        public UUID uuid() {
            return this.uuid;
        }

        public String username() {
            return this.username;
        }

        public String ip() {
            return this.ip;
        }

        public WhitelistPlayerFilter(String username, UUID uuid, String ip) {
            this.username = username;
            this.uuid = uuid;
            this.ip = ip;
        }

        public static boolean validate(Whitelist whitelist, IPlayer playerToValidate) {
            WhitelistPlayerFilter player = whitelist.playerFilters().stream()
                    .filter(whitelistPlayerFilter -> whitelistPlayerFilter.username().equals(playerToValidate.username()))
                    .findAny().orElse(null);
            if(player == null) return false;

            if(player.uuid() != null)
                if(!Objects.equals(player.uuid().toString(), playerToValidate.uuid().toString()))
                    return false;

            if(player.ip() != null) {
                try {
                    return Objects.equals(player.ip(), playerToValidate.resolve().orElseThrow().getRemoteAddress().getHostString());
                } catch (Exception ignore) {
                    return false;
                }
            }

            return true;
        }

        public String toString() {
            return "WhitelistPlayer: "+username+" "+uuid+" "+ip;
        }
    }
}
